package kz.ktj.digitaltwin.alerts.controller;

import kz.ktj.digitaltwin.alerts.entity.Alert;
import kz.ktj.digitaltwin.alerts.entity.AlertThreshold;
import kz.ktj.digitaltwin.alerts.repository.AlertRepository;
import kz.ktj.digitaltwin.alerts.repository.AlertThresholdRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertThresholdRepository thresholdRepository;

    public AlertController(AlertRepository alertRepository,
                            AlertThresholdRepository thresholdRepository) {
        this.alertRepository = alertRepository;
        this.thresholdRepository = thresholdRepository;
    }

    /** GET /api/v1/alerts/{locomotiveId} — all recent alerts */
    @GetMapping("/{locomotiveId}")
    public List<Alert> getAlerts(@PathVariable String locomotiveId) {
        return alertRepository.findTop50ByLocomotiveIdOrderByTriggeredAtDesc(locomotiveId);
    }

    /** GET /api/v1/alerts/{locomotiveId}/active — only active */
    @GetMapping("/{locomotiveId}/active")
    public List<Alert> getActiveAlerts(@PathVariable String locomotiveId) {
        return alertRepository.findByLocomotiveIdAndStatusOrderByTriggeredAtDesc(
            locomotiveId, Alert.Status.ACTIVE);
    }

    /** GET /api/v1/alerts/{locomotiveId}/history?from=...&to=... */
    @GetMapping("/{locomotiveId}/history")
    public List<Alert> getHistory(
            @PathVariable String locomotiveId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return alertRepository.findByLocomotiveIdAndTriggeredAtBetweenOrderByTriggeredAtDesc(
            locomotiveId, from, to);
    }

    /** POST /api/v1/alerts/{id}/acknowledge — dispatcher acknowledges alert */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledge(@PathVariable UUID id) {
        return alertRepository.findById(id)
            .map(alert -> {
                alert.setStatus(Alert.Status.ACKNOWLEDGED);
                alert.setAcknowledgedAt(Instant.now());
                return ResponseEntity.ok(alertRepository.save(alert));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/v1/alerts/{id}/resolve — manually resolve */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Alert> resolve(@PathVariable UUID id) {
        return alertRepository.findById(id)
            .map(alert -> {
                alert.setStatus(Alert.Status.RESOLVED);
                alert.setResolvedAt(Instant.now());
                return ResponseEntity.ok(alertRepository.save(alert));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/v1/alerts/thresholds — current threshold config */
    @GetMapping("/thresholds")
    public List<AlertThreshold> getThresholds() {
        return thresholdRepository.findAll();
    }

    /** PUT /api/v1/alerts/thresholds/{id} — update threshold (without recompilation!) */
    @PutMapping("/thresholds/{id}")
    public ResponseEntity<AlertThreshold> updateThreshold(
            @PathVariable UUID id, @RequestBody AlertThreshold update) {
        return thresholdRepository.findById(id)
            .map(existing -> {
                existing.setWarningHigh(update.getWarningHigh());
                existing.setWarningLow(update.getWarningLow());
                existing.setCriticalHigh(update.getCriticalHigh());
                existing.setCriticalLow(update.getCriticalLow());
                existing.setWarningRecommendation(update.getWarningRecommendation());
                existing.setCriticalRecommendation(update.getCriticalRecommendation());
                existing.setEnabled(update.isEnabled());
                return ResponseEntity.ok(thresholdRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
