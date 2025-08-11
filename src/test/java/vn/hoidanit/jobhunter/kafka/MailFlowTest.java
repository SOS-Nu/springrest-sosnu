package vn.hoidanit.jobhunter.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;

import vn.hoidanit.jobhunter.service.EmailService;

@SpringBootTest(properties = {
                // Dùng broker embedded thay cho localhost:9092
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",

                // Cấu hình JSON producer/consumer để serialize/deserialize MailCommand
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
                "spring.kafka.consumer.properties.spring.json.trusted.packages=vn.hoidanit.jobhunter.*",
                "spring.kafka.consumer.properties.spring.json.value.default.type=vn.hoidanit.jobhunter.kafka.MailCommand",

                // Tên topic dùng trong test
                "app.kafka.topics.mail=mail-commands"
})
@EmbeddedKafka(partitions = 1, topics = { "mail-commands" })
class MailFlowIT {

        @Autowired
        private MailProducer mailProducer;

        // Mock để KHÔNG gửi mail thật; chỉ verify được gọi
        @MockBean
        private EmailService emailService;

        @Test
        void shouldConsumeAndInvokeEmailService() {
                MailCommand cmd = new MailCommand(
                                "someone@example.com",
                                "Subject demo",
                                "job",
                                "User Demo",
                                Collections.emptyList() // không cần dữ liệu job thật cho test
                );

                // Gửi vào Kafka
                mailProducer.queue(cmd);

                // Verify: Consumer đã nhận và gọi EmailService trong vòng 5 giây
                verify(emailService, timeout(5000)).sendEmailFromTemplateSync(
                                eq("someone@example.com"),
                                eq("Subject demo"),
                                eq("job"),
                                eq("User Demo"),
                                any());
        }
}
