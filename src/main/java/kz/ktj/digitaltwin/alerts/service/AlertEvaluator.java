package kz.ktj.digitaltwin.alerts.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.ktj.digitaltwin.alerts.dto.AlertEvent;
import kz.ktj.digitaltwin.alerts.dto.TelemetryEnvelope;
import kz.ktj.digitaltwin.alerts.entity.Alert;
import kz.ktj.digitaltwin.alerts.entity.Alert.Severity;
import kz.ktj.digitaltwin.alerts.entity.Alert.Status;
import kz.ktj.digitaltwin.alerts.entity.AlertThreshold;
import kz.ktj.digitaltwin.alerts.repository.AlertRepository;
import kz.ktj.digitaltwin.alerts.repository.AlertThresholdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluator.class);

    private final AlertRepository alertRepository;
    private final AlertThresholdRepository thresholdRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int cooldownSeconds;

    private volatile List<AlertThreshold> thresholdCache = new ArrayList<>();
    private volatile long lastCacheRefresh = 0;

    private final ConcurrentHashMap<String, Instant> cooldowns = new ConcurrentHashMap<>();

    public AlertEvaluator(
            AlertRepository alertRepository,
            AlertThresholdRepository thresholdRepository,
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${alert.cooldown-seconds:30}") int cooldownSeconds) {
        this.alertRepository = alertRepository;
        this.thresholdRepository = thresholdRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.cooldownSeconds = cooldownSeconds;
    }

    public void evaluate(TelemetryEnvelope envelope) {
        refreshCacheIfNeeded();

        Map<String, Double> params = envelope.getParameters();
        String locoId = envelope.getLocomotiveId();
        List<AlertEvent> newEvents = new ArrayList<>();
        Set<String> evaluatedParams = new HashSet<>();

        for (AlertThreshold threshold : thresholdCache) {
            if (!isApplicable(threshold.getApplicableTo(), envelope.getLocomotiveType())) continue;

            String paramName = threshold.getParamName();
            if (evaluatedParams.contains(paramName)) continue;

            Double value = params.get(paramName);
            if (value == null) continue;

            Severity triggered = checkThreshold(value, threshold);

            if (triggered != null) {
                String cooldownKey = locoId + ":" + paramName;
                Instant lastFired = cooldowns.get(cooldownKey);
                if (lastFired != null &&
                    Duration.between(lastFired, Instant.now()).getSeconds() < cooldownSeconds) {
                    evaluatedParams.add(paramName);
                    continue;
                }

                Alert alert = createAlert(envelope, paramName, threshold, value, triggered);
                alertRepository.save(alert);
                cooldowns.put(cooldownKey, Instant.now());
                newEvents.add(AlertEvent.from(alert));
                evaluatedParams.add(paramName);

                log.info("[{}] ALERT {}: {} = {} (threshold breached)", locoId, triggered, paramName, value);
            } else {
                autoResolve(locoId, paramName);
                evaluatedParams.add(paramName);
            }
        }

        if (!newEvents.isEmpty()) {
            publishToRedis(locoId, newEvents);
        }
    }

    private Severity checkThreshold(double value, AlertThreshold t) {
        if (t.getCriticalHigh() != null && value >= t.getCriticalHigh()) return Severity.CRITICAL;
        if (t.getCriticalLow()  != null && value <= t.getCriticalLow())  return Severity.CRITICAL;
        if (t.getWarningHigh()  != null && value >= t.getWarningHigh())  return Severity.WARNING;
        if (t.getWarningLow()   != null && value <= t.getWarningLow())   return Severity.WARNING;
        return null;
    }

    private Alert createAlert(TelemetryEnvelope envelope, String paramName,
                               AlertThreshold threshold, double value, Severity severity) {
        Alert alert = new Alert();
        alert.setLocomotiveId(envelope.getLocomotiveId());
        alert.setSeverity(severity);
        alert.setParamName(paramName);
        alert.setDisplayName(threshold.getDisplayName());
        alert.setParamValue(value);
        alert.setStatus(Status.ACTIVE);
        alert.setTriggeredAt(Instant.now());

        if (severity == Severity.CRITICAL) {
            boolean high = threshold.getCriticalHigh() != null && value >= threshold.getCriticalHigh();
            alert.setThresholdValue(high ? threshold.getCriticalHigh() : threshold.getCriticalLow());
            alert.setMessage(threshold.getDisplayName() + " в критической зоне: " + value);
            alert.setRecommendation(threshold.getCriticalRecommendation());
        } else {
            boolean high = threshold.getWarningHigh() != null && value >= threshold.getWarningHigh();
            alert.setThresholdValue(high ? threshold.getWarningHigh() : threshold.getWarningLow());
            alert.setMessage(threshold.getDisplayName() + " требует внимания: " + value);
            alert.setRecommendation(threshold.getWarningRecommendation());
        }

        return alert;
    }

    private void autoResolve(String locomotiveId, String paramName) {
        List<Alert> active = alertRepository
            .findByLocomotiveIdAndParamNameAndStatusAndTriggeredAtAfter(
                locomotiveId, paramName, Status.ACTIVE, Instant.now().minusSeconds(3600));

        for (Alert alert : active) {
            alert.setStatus(Status.RESOLVED);
            alert.setResolvedAt(Instant.now());
            alertRepository.save(alert);
            log.debug("[{}] Auto-resolved alert for {}", locomotiveId, paramName);
        }
    }

    private void publishToRedis(String locomotiveId, List<AlertEvent> events) {
        try {
            String json = objectMapper.writeValueAsString(events);
            redis.convertAndSend("alerts:" + locomotiveId, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish alerts to Redis: {}", e.getMessage());
        }
    }

    private boolean isApplicable(String applicableTo, String locoType) {
        return "BOTH".equals(applicableTo) || applicableTo.equals(locoType);
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheRefresh > 60_000) {
            List<AlertThreshold> all = thresholdRepository.findByEnabledTrue();
            thresholdCache = all;
            lastCacheRefresh = now;
            log.debug("Threshold cache refreshed: {} entries", all.size());
        }
    }
}
