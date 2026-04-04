package kz.ktj.digitaltwin.alerts.repository;

import kz.ktj.digitaltwin.alerts.entity.AlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, UUID> {

    List<AlertThreshold> findByEnabledTrue();

    List<AlertThreshold> findByEnabledTrueAndApplicableToIn(List<String> types);
}
