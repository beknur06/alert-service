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

    /** WARNING: значение выше этого порога (или ниже для invertDirection) */
    private Double warningHigh;
    private Double warningLow;

    /** CRITICAL: значение выше этого порога */
    private Double criticalHigh;
    private Double criticalLow;

    /** Рекомендация для машиниста при WARNING */
    @Column(length = 500)
    private String warningRecommendation;

    /** Рекомендация для машиниста при CRITICAL */
    @Column(length = 500)
    private String criticalRecommendation;

    /** Применимо к типу локомотива */
    @Column(nullable = false)
    private String applicableTo = "BOTH";

    @Column(nullable = false)
    private boolean enabled = true;
}
