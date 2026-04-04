package kz.ktj.digitaltwin.alerts.dto;

import kz.ktj.digitaltwin.alerts.entity.Alert;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AlertEvent {
    private String id;
    private String locomotiveId;
    private String severity;
    private String paramName;
    private String displayName;
    private double paramValue;
    private double thresholdValue;
    private String message;
    private String recommendation;
    private Instant triggeredAt;

    public static AlertEvent from(Alert alert) {
        return AlertEvent.builder()
            .id(alert.getId().toString())
            .locomotiveId(alert.getLocomotiveId())
            .severity(alert.getSeverity().name())
            .paramName(alert.getParamName())
            .displayName(alert.getDisplayName())
            .paramValue(alert.getParamValue())
            .thresholdValue(alert.getThresholdValue())
            .message(alert.getMessage())
            .recommendation(alert.getRecommendation())
            .triggeredAt(alert.getTriggeredAt())
            .build();
    }
}
