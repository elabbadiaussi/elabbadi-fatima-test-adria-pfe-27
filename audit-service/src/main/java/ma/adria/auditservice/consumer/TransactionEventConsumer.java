package ma.adria.auditservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.adria.auditservice.event.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les evenements et les JOURNALISE uniquement (aucune modification des donnees bancaires).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${bank.kafka.topic.transactions-created}", groupId = "audit-service")
    public void onTransactionCreated(ConsumerRecord<String, String> record) {
        try {
            TransactionCreatedEvent event = objectMapper.readValue(record.value(), TransactionCreatedEvent.class);
            log.info("[AUDIT] {} recu | key(accountId)={} | partition={} offset={} | eventId={} transactionId={} amount={} {} occurredAt={}",
                    event.eventType(), record.key(), record.partition(), record.offset(),
                    event.eventId(), event.transactionId(), event.amount(), event.currency(), event.occurredAt());
        } catch (Exception e) {
            log.warn("[AUDIT] Message illisible key={} value={}", record.key(), record.value());
        }
    }
}