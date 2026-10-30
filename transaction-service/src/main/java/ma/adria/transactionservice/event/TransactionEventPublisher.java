package ma.adria.transactionservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publie l'evenement dans Kafka UNIQUEMENT apres le commit de la transaction en base
 * (persistance reussie). Cle du message = accountId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${bank.kafka.topic.transactions-created}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.accountId().toString();
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Echec publication Kafka de l'evenement {} : {}", event.eventId(), ex.getMessage());
                } else {
                    log.info("Evenement publie dans {} [partition={}, offset={}] key={} : {}",
                            topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset(),
                            key, payload);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Serialisation impossible de l'evenement {}", event, e);
        }
    }
}