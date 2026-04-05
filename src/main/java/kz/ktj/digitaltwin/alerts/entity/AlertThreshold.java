package kz.ktj.digitaltwin.alerts.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

/**
 * Конфигурация порогов для генерации алертов.
 * Один параметр может иметь WARNING и CRITICAL порог.
 */
@Entity
@Table(name = "alert_thresholds")
@Data
public class AlertThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String paramName;

    private String displayName;

    private Double warningHigh;
    private Double warningLow;
    private Double criticalHigh;
    private Double criticalLow;

    @Column(length = 500)
    private String warningRecommendation;

    @Column(length = 500)
    private String criticalRecommendation;

    @Column(nullable = false)
    private String applicableTo = "BOTH";

    @Column(nullable = false)
    private boolean enabled = true;
}
