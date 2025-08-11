package vn.hoidanit.jobhunter.kafka;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class MailProducer {

    private static final Logger log = LoggerFactory.getLogger(MailProducer.class);

    private final KafkaTemplate<String, MailCommand> kafkaTemplate;
    private final String topic;

    public MailProducer(
            KafkaTemplate<String, MailCommand> kafkaTemplate,
            @Value("${app.kafka.topics.mail}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void queue(MailCommand cmd) {
        // key = email người nhận để partitioning ổn định
        CompletableFuture<SendResult<String, MailCommand>> future = kafkaTemplate.send(topic, cmd.getTo(), cmd);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                var md = result.getRecordMetadata();
                log.info("[PRODUCER] sent ok -> topic={} partition={} offset={} key={}",
                        md.topic(), md.partition(), md.offset(), cmd.getTo());
            } else {
                log.error("[PRODUCER] send failed key={} cause={}", cmd.getTo(), ex.toString(), ex);
            }
        });
    }
}
