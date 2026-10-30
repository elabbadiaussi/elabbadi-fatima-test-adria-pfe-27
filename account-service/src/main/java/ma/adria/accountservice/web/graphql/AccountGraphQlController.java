package ma.adria.accountservice.web.graphql;

import lombok.RequiredArgsConstructor;
import ma.adria.accountservice.domain.Account;
import ma.adria.accountservice.domain.AccountStatus;
import ma.adria.accountservice.service.AccountService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountGraphQlController {

    private final AccountService accountService;

    @QueryMapping
    public List<Account> accounts() {
        return accountService.findAll();
    }

    @QueryMapping
    public Account accountById(@Argument String id) {
        return accountService.findById(UUID.fromString(id));
    }

    @QueryMapping
    public List<Account> accountsByCustomer(@Argument String customerReference) {
        return accountService.findByCustomerReference(customerReference);
    }

    @MutationMapping
    public Account createAccount(@Argument String customerReference,
                                 @Argument String currency,
                                 @Argument double initialBalance) {
        return accountService.create(customerReference, currency, initialBalance);
    }

    @MutationMapping
    public Account updateAccountStatus(@Argument String id, @Argument AccountStatus status) {
        return accountService.updateStatus(UUID.fromString(id), status);
    }

    @MutationMapping
    public Account debitAccount(@Argument String id, @Argument double amount) {
        return accountService.debit(UUID.fromString(id), amount);
    }

    @MutationMapping
    public Account creditAccount(@Argument String id, @Argument double amount) {
        return accountService.credit(UUID.fromString(id), amount);
    }
}