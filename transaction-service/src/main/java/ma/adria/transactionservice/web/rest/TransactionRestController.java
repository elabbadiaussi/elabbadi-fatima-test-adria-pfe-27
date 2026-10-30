package ma.adria.transactionservice.web.rest;

import lombok.RequiredArgsConstructor;
import ma.adria.transactionservice.domain.Transaction;
import ma.adria.transactionservice.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionRestController {

    private final TransactionService transactionService;

    @GetMapping
    public List<Transaction> findAll() {
        return transactionService.findAll();
    }

    @GetMapping("/{id}")
    public Transaction findById(@PathVariable UUID id) {
        return transactionService.findById(id);
    }

    @GetMapping("/account/{accountId}")
    public List<Transaction> findByAccount(@PathVariable UUID accountId) {
        return transactionService.findByAccount(accountId);
    }

    @GetMapping("/config/max-amount")
    public Map<String, BigDecimal> maxAmount() {
        return Map.of("bank.transaction.max-amount", transactionService.getMaxAmount());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@RequestBody CreateTransactionRequest r) {
        return transactionService.create(r.accountId(), r.amount(), r.currency(), r.merchantCategory(), r.country());
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transaction> transfer(@RequestBody TransferRequest r) {
        return transactionService.transfer(r.fromAccountId(), r.toAccountId(), r.amount(), r.currency());
    }
}