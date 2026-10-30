package ma.adria.transactionservice.web.graphql;

import lombok.RequiredArgsConstructor;
import ma.adria.transactionservice.domain.Transaction;
import ma.adria.transactionservice.service.TransactionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionGraphQlController {

    private final TransactionService transactionService;

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.findAll();
    }

    @QueryMapping
    public Transaction transactionById(@Argument String id) {
        return transactionService.findById(UUID.fromString(id));
    }

    @QueryMapping
    public List<Transaction> transactionsByAccount(@Argument String accountId) {
        return transactionService.findByAccount(UUID.fromString(accountId));
    }

    @MutationMapping
    public Transaction createTransaction(@Argument String accountId, @Argument double amount,
                                         @Argument String currency, @Argument String merchantCategory,
                                         @Argument String country) {
        return transactionService.create(UUID.fromString(accountId), BigDecimal.valueOf(amount),
                currency, merchantCategory, country);
    }

    @MutationMapping
    public List<Transaction> transfer(@Argument String fromAccountId, @Argument String toAccountId,
                                      @Argument double amount, @Argument String currency) {
        return transactionService.transfer(UUID.fromString(fromAccountId), UUID.fromString(toAccountId),
                BigDecimal.valueOf(amount), currency);
    }
}