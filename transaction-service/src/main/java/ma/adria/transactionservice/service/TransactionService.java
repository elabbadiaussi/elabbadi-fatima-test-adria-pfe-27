package ma.adria.transactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.adria.transactionservice.client.AccountDto;
import ma.adria.transactionservice.client.AccountRestClient;
import ma.adria.transactionservice.domain.Transaction;
import ma.adria.transactionservice.domain.TransactionType;
import ma.adria.transactionservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRestClient accountRestClient;

    @Value("${bank.transaction.max-amount:1000}")
    private BigDecimal maxAmount;

    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Transaction findById(UUID id) {
        return transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Transaction> findByAccount(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    /** Paiement : enregistre la transaction puis debite le compte. */
    @Transactional
    public Transaction create(UUID accountId, BigDecimal amount, String currency,
                              String merchantCategory, String country) {
        checkAmount(amount);
        // Regle : consultation du compte (existence, statut, devise, solde)
        AccountDto account = accountRestClient.getAccount(accountId);
        checkAccount(account, currency, amount);

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .currency(currency.toUpperCase())
                .merchantCategory(merchantCategory)
                .country(country)
                .createdAt(Instant.now())
                .type(TransactionType.PAYMENT)
                .build());

        // Regle : apres l'enregistrement, mise a jour du solde
        accountRestClient.debit(accountId, amount.doubleValue());
        log.info("Transaction {} enregistree sur le compte {} ({} {})",
                transaction.getId(), accountId, amount, currency);
        return transaction;
    }

    /** Virement d'un compte vers un autre. */
    @Transactional
    public List<Transaction> transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
        if (fromAccountId.equals(toAccountId))
            throw new IllegalArgumentException("Le compte source et le compte destination doivent etre differents");
        checkAmount(amount);

        AccountDto from = accountRestClient.getAccount(fromAccountId);
        AccountDto to = accountRestClient.getAccount(toAccountId);
        checkAccount(from, currency, amount);
        checkAccount(to, currency, null);

        Instant now = Instant.now();
        Transaction debit = transactionRepository.save(Transaction.builder()
                .accountId(fromAccountId).amount(amount).currency(currency.toUpperCase())
                .merchantCategory("TRANSFER").createdAt(now).type(TransactionType.TRANSFER_DEBIT).build());
        Transaction credit = transactionRepository.save(Transaction.builder()
                .accountId(toAccountId).amount(amount).currency(currency.toUpperCase())
                .merchantCategory("TRANSFER").createdAt(now).type(TransactionType.TRANSFER_CREDIT).build());

        accountRestClient.debit(fromAccountId, amount.doubleValue());
        try {
            accountRestClient.credit(toAccountId, amount.doubleValue());
        } catch (RuntimeException e) {
            // compensation : on recredite le compte source
            accountRestClient.credit(fromAccountId, amount.doubleValue());
            throw e;
        }
        log.info("Virement de {} {} du compte {} vers {}", amount, currency, fromAccountId, toAccountId);
        return List.of(debit, credit);
    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Le montant doit etre strictement positif");
        if (amount.compareTo(maxAmount) > 0)
            throw new IllegalArgumentException("Le montant depasse le plafond autorise de " + maxAmount);
    }

    private void checkAccount(AccountDto account, String currency, BigDecimal amountToDebit) {
        if (!"ACTIVE".equals(account.status()))
            throw new IllegalStateException("Le compte " + account.id() + " n'est pas actif");
        if (currency == null || !account.currency().equalsIgnoreCase(currency))
            throw new IllegalArgumentException("La devise " + currency
                    + " ne correspond pas a celle du compte (" + account.currency() + ")");
        if (amountToDebit != null && BigDecimal.valueOf(account.balance()).compareTo(amountToDebit) < 0)
            throw new IllegalStateException("Solde insuffisant : solde actuel = " + account.balance());
    }
}