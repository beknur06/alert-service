package kz.ktj.digitaltwin.alerts.config;

import jakarta.annotation.PostConstruct;
import kz.ktj.digitaltwin.alerts.entity.AlertThreshold;
import kz.ktj.digitaltwin.alerts.repository.AlertThresholdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultThresholdSeeder {

    private static final Logger log = LoggerFactory.getLogger(DefaultThresholdSeeder.class);
    private final AlertThresholdRepository repository;

    public DefaultThresholdSeeder(AlertThresholdRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void seed() {
        if (repository.count() > 0) return;

        log.info("Seeding default alert thresholds...");
        repository.saveAll(List.of(
            threshold("coolant_temp", "Охл. жидкость",
                null, 88.0, null, 95.0,
                "Снизьте нагрузку, проверьте вентиляторы",
                "Остановите поезд, проверьте систему охлаждения", "TE33A"),

            threshold("oil_temp", "Масло двигателя",
                null, 90.0, null, 100.0,
                "Контролируйте температуру, снизьте обороты",
                "Остановите дизель, возможен перегрев", "TE33A"),

            threshold("exhaust_temp", "Выхлопные газы",
                null, 550.0, null, 600.0,
                "Проверьте топливную аппаратуру",
                "Немедленно снизьте нагрузку", "TE33A"),

            threshold("traction_motor_temp", "Обмотки ТЭД",
                null, 140.0, null, 160.0,
                "Снизьте тяговое усилие",
                "Отключите тяговый двигатель", "BOTH"),

            threshold("transformer_oil_temp", "Масло трансформатора",
                null, 85.0, null, 95.0,
                "Проверьте вентиляцию трансформатора",
                "Снизьте нагрузку, возможен перегрев обмоток", "KZ8A"),

            threshold("oil_pressure", "Давление масла",
                0.25, null, 0.15, null,
                "Проверьте уровень масла",
                "Остановите дизель! Критическое давление масла", "TE33A"),

            threshold("brake_pipe_pressure", "Торм. магистраль",
                0.40, null, 0.35, null,
                "Проверьте тормозную магистраль на утечки",
                "Экстренное торможение! Утечка воздуха", "BOTH"),

            threshold("main_reservoir_pressure", "Главный резервуар",
                0.65, null, 0.55, null,
                "Компрессор не поддерживает давление",
                "Остановитесь, нет запаса воздуха для торможения", "BOTH"),

            threshold("catenary_voltage", "Напряжение сети",
                21.0, 29.0, 19.0, 31.0,
                "Нестабильное напряжение, контролируйте ток",
                "Аварийное напряжение сети, снизьте потребление", "KZ8A"),

            threshold("traction_motor_current", "Ток ТЭД",
                null, 1100.0, null, 1200.0,
                "Приближение к пределу, снизьте тягу",
                "Перегрузка ТЭД! Отключите тяговый двигатель", "BOTH"),

            threshold("fuel_level", "Уровень топлива",
                600.0, null, 300.0, null,
                "Низкий запас топлива, планируйте заправку",
                "Критический уровень топлива!", "TE33A"),

            threshold("battery_voltage", "АКБ",
                95.0, null, 85.0, null,
                "Низкий заряд АКБ",
                "Критический разряд АКБ", "BOTH"),

            threshold("sand_level", "Уровень песка",
                20.0, null, 10.0, null,
                "Мало песка, заправьте при остановке",
                "Критически мало песка, сцепление ухудшено", "BOTH"),

            threshold("engine_rpm", "Обороты дизеля",
                null, 1050.0, 400.0, 1100.0,
                "Нестабильные обороты, проверьте регулятор",
                "Аварийные обороты дизеля", "TE33A")
        ));
        log.info("Seeded {} thresholds", repository.count());
    }

    private AlertThreshold threshold(String param, String display,
                                      Double warnLow, Double warnHigh,
                                      Double critLow, Double critHigh,
                                      String warnRec, String critRec, String applicableTo) {
        AlertThreshold t = new AlertThreshold();
        t.setParamName(param);
        t.setDisplayName(display);
        t.setWarningLow(warnLow);
        t.setWarningHigh(warnHigh);
        t.setCriticalLow(critLow);
        t.setCriticalHigh(critHigh);
        t.setWarningRecommendation(warnRec);
        t.setCriticalRecommendation(critRec);
        t.setApplicableTo(applicableTo);
        t.setEnabled(true);
        return t;
    }
}
