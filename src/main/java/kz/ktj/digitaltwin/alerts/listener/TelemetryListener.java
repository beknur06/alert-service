package kz.ktj.digitaltwin.alerts.listener;

import kz.ktj.digitaltwin.alerts.dto.TelemetryEnvelope;
import kz.ktj.digitaltwin.alerts.service.AlertEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(TelemetryListener.class);
    private final AlertEvaluator evaluator;

    public TelemetryListener(AlertEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @RabbitListener(queues = "${rabbitmq.queue.alerts}")
    public void onMessage(TelemetryEnvelope envelope) {
        if (envelope == null || envelope.getLocomotiveId() == null) return;
        try {
            evaluator.evaluate(envelope);
        } catch (Exception e) {
            log.error("Alert evaluation failed for [{}]: {}", envelope.getMessageId(), e.getMessage());
        }
    }
}
