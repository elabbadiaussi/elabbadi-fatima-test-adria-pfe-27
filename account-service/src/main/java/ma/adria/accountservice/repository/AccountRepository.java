package ma.adria.accountservice.repository;

import ma.adria.accountservice.domain.Account;
import ma.adria.accountservice.domain.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByCustomerReference(String customerReference);
    List<Account> findByStatus(AccountStatus status);
}