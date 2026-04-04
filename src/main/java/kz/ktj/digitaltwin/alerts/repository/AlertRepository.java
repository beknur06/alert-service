package kz.ktj.digitaltwin.alerts.repository;

import kz.ktj.digitaltwin.alerts.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByLocomotiveIdAndStatusOrderByTriggeredAtDesc(String locomotiveId, Alert.Status status);

    List<Alert> findByLocomotiveIdOrderByTriggeredAtDesc(String locomotiveId);

    List<Alert> findTop50ByLocomotiveIdOrderByTriggeredAtDesc(String locomotiveId);

    List<Alert> findByLocomotiveIdAndTriggeredAtBetweenOrderByTriggeredAtDesc(
        String locomotiveId, Instant from, Instant to);

    /** Find active alert for same locomotive+param (for cooldown check) */
    List<Alert> findByLocomotiveIdAndParamNameAndStatusAndTriggeredAtAfter(
        String locomotiveId, String paramName, Alert.Status status, Instant after);
}
