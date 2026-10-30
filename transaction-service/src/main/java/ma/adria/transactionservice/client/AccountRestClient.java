package ma.adria.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "account-service",
        url = "${account-service.url:}",
        fallbackFactory = AccountRestClientFallbackFactory.class)
public interface AccountRestClient {

    @GetMapping("/api/accounts/{id}")
    AccountDto getAccount(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts/{id}/debit")
    AccountDto debit(@PathVariable("id") UUID id, @RequestParam("amount") double amount);

    @PostMapping("/api/accounts/{id}/credit")
    AccountDto credit(@PathVariable("id") UUID id, @RequestParam("amount") double amount);
}