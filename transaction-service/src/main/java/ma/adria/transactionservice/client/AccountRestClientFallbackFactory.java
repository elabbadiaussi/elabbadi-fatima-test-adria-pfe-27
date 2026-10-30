package ma.adria.transactionservice.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tolerance aux pannes (Resilience4J) :
 * - erreur metier renvoyee par account-service (404 / 400) -> on remonte une erreur metier claire
 * - account-service injoignable / lent / circuit ouvert -> AccountServiceUnavailableException (HTTP 503)
 */
@Slf4j
@Component
public class AccountRestClientFallbackFactory implements FallbackFactory<AccountRestClient> {

    @Override
    public AccountRestClient create(Throwable cause) {
        return new AccountRestClient() {
            @Override
            public AccountDto getAccount(UUID id) {
                throw translate(cause, id);
            }

            @Override
            public AccountDto debit(UUID id, double amount) {
                throw translate(cause, id);
            }

            @Override
            public AccountDto credit(UUID id, double amount) {
                throw translate(cause, id);
            }
        };
    }

    private RuntimeException translate(Throwable cause, UUID id) {
        Throwable t = cause;
        while (t != null && !(t instanceof FeignException)) {
            t = t.getCause();
        }
        if (t instanceof FeignException fe) {
            if (fe.status() == 404) {
                return new IllegalArgumentException("Le compte " + id + " n'existe pas");
            }
            if (fe.status() == 400) {
                return new IllegalStateException("Refus de account-service : " + fe.contentUTF8());
            }
        }
        log.warn("Fallback Resilience4J pour le compte {} : {}", id, cause.toString());
        return new AccountServiceUnavailableException(
                "account-service indisponible, reessayez plus tard (circuit breaker)");
    }
}