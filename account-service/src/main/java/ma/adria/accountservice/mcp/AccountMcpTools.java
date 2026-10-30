package ma.adria.accountservice.mcp;

import lombok.RequiredArgsConstructor;
import ma.adria.accountservice.domain.Account;
import ma.adria.accountservice.domain.AccountStatus;
import ma.adria.accountservice.service.AccountService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountMcpTools {

    private final AccountService accountService;

    @Tool(name = "listAccounts", description = "Liste tous les comptes bancaires avec leur statut, devise et solde")
    public List<Account> listAccounts() {
        return accountService.findAll();
    }

    @Tool(name = "getAccount", description = "Retourne un compte bancaire a partir de son identifiant UUID")
    public Account getAccount(@ToolParam(description = "Identifiant UUID du compte") String accountId) {
        return accountService.findById(UUID.fromString(accountId));
    }

    @Tool(name = "getAccountsByCustomer", description = "Retourne les comptes d'un client a partir de sa reference client (ex: CUST-001)")
    public List<Account> getAccountsByCustomer(@ToolParam(description = "Reference du client") String customerReference) {
        return accountService.findByCustomerReference(customerReference);
    }

    @Tool(name = "createAccount", description = "Cree un nouveau compte bancaire actif")
    public Account createAccount(@ToolParam(description = "Reference du client") String customerReference,
                                 @ToolParam(description = "Devise ISO sur 3 lettres, ex: EUR") String currency,
                                 @ToolParam(description = "Solde initial, positif ou nul") double initialBalance) {
        return accountService.create(customerReference, currency, initialBalance);
    }

    @Tool(name = "blockAccount", description = "Bloque un compte bancaire (statut BLOCKED)")
    public Account blockAccount(@ToolParam(description = "Identifiant UUID du compte") String accountId) {
        return accountService.updateStatus(UUID.fromString(accountId), AccountStatus.BLOCKED);
    }

    @Tool(name = "activateAccount", description = "Active un compte bancaire (statut ACTIVE)")
    public Account activateAccount(@ToolParam(description = "Identifiant UUID du compte") String accountId) {
        return accountService.updateStatus(UUID.fromString(accountId), AccountStatus.ACTIVE);
    }
}