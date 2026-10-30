package ma.adria.accountservice.web.rest;

import lombok.RequiredArgsConstructor;
import ma.adria.accountservice.domain.Account;
import ma.adria.accountservice.domain.AccountStatus;
import ma.adria.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @GetMapping
    public List<Account> findAll() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public Account findById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @GetMapping("/customer/{customerReference}")
    public List<Account> findByCustomer(@PathVariable String customerReference) {
        return accountService.findByCustomerReference(customerReference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@RequestBody CreateAccountRequest request) {
        return accountService.create(request.customerReference(), request.currency(), request.initialBalance());
    }

    @PatchMapping("/{id}/status")
    public Account updateStatus(@PathVariable UUID id, @RequestParam AccountStatus status) {
        return accountService.updateStatus(id, status);
    }

    @PostMapping("/{id}/debit")
    public Account debit(@PathVariable UUID id, @RequestParam double amount) {
        return accountService.debit(id, amount);
    }

    @PostMapping("/{id}/credit")
    public Account credit(@PathVariable UUID id, @RequestParam double amount) {
        return accountService.credit(id, amount);
    }
}