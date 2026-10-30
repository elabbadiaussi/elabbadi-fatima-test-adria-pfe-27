package ma.adria.accountservice.service;

import lombok.RequiredArgsConstructor;
import ma.adria.accountservice.domain.Account;
import ma.adria.accountservice.domain.AccountStatus;
import ma.adria.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Account findById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Account> findByCustomerReference(String customerReference) {
        return accountRepository.findByCustomerReference(customerReference);
    }

    public Account create(String customerReference, String currency, double initialBalance) {
        if (customerReference == null || customerReference.isBlank())
            throw new IllegalArgumentException("customerReference est obligatoire");
        if (currency == null || currency.length() != 3)
            throw new IllegalArgumentException("currency doit etre un code ISO de 3 lettres (ex: EUR)");
        if (initialBalance < 0)
            throw new IllegalArgumentException("Le solde initial ne peut pas etre negatif");

        Account account = Account.builder()
                .customerReference(customerReference)
                .currency(currency.toUpperCase())
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    public Account updateStatus(UUID id, AccountStatus status) {
        Account account = findById(id);
        account.setStatus(status);
        return accountRepository.save(account);
    }

    public Account debit(UUID id, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Le montant doit etre strictement positif");
        Account account = findById(id);
        if (account.getStatus() != AccountStatus.ACTIVE)
            throw new IllegalStateException("Le compte " + id + " n'est pas actif");
        if (account.getBalance() < amount)
            throw new IllegalStateException("Solde insuffisant sur le compte " + id);
        account.setBalance(account.getBalance() - amount);
        return accountRepository.save(account);
    }

    public Account credit(UUID id, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Le montant doit etre strictement positif");
        Account account = findById(id);
        if (account.getStatus() != AccountStatus.ACTIVE)
            throw new IllegalStateException("Le compte " + id + " n'est pas actif");
        account.setBalance(account.getBalance() + amount);
        return accountRepository.save(account);
    }
}