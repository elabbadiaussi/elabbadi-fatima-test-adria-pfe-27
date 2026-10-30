package ma.adria.accountservice;

import lombok.extern.slf4j.Slf4j;
import ma.adria.accountservice.domain.Account;
import ma.adria.accountservice.domain.AccountStatus;
import ma.adria.accountservice.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@Slf4j
@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAccounts(AccountRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            List<Account> accounts = List.of(
                    Account.builder().customerReference("CUST-001").status(AccountStatus.ACTIVE).currency("EUR").balance(5000).build(),
                    Account.builder().customerReference("CUST-002").status(AccountStatus.ACTIVE).currency("EUR").balance(1200).build(),
                    Account.builder().customerReference("CUST-003").status(AccountStatus.BLOCKED).currency("EUR").balance(800).build(),
                    Account.builder().customerReference("CUST-004").status(AccountStatus.ACTIVE).currency("MAD").balance(10000).build()
            );
            repository.saveAll(accounts).forEach(a ->
                    log.info("Compte cree : id={} ref={} status={} currency={} balance={}",
                            a.getId(), a.getCustomerReference(), a.getStatus(), a.getCurrency(), a.getBalance()));
        };
    }
}