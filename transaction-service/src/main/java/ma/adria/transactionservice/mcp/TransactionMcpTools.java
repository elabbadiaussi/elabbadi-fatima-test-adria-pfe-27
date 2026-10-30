package ma.adria.transactionservice.mcp;

import lombok.RequiredArgsConstructor;
import ma.adria.transactionservice.domain.Transaction;
import ma.adria.transactionservice.service.TransactionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionMcpTools {

    private final TransactionService transactionService;

    @Tool(name = "listTransactions", description = "Liste toutes les transactions bancaires")
    public List<Transaction> listTransactions() {
        return transactionService.findAll();
    }

    @Tool(name = "getTransactionsByAccount", description = "Liste les transactions d'un compte (UUID du compte)")
    public List<Transaction> getTransactionsByAccount(@ToolParam(description = "UUID du compte") String accountId) {
        return transactionService.findByAccount(UUID.fromString(accountId));
    }

    @Tool(name = "createPayment", description = "Enregistre un paiement (transaction) sur un compte et debite son solde")
    public Transaction createPayment(@ToolParam(description = "UUID du compte") String accountId,
                                     @ToolParam(description = "Montant strictement positif") double amount,
                                     @ToolParam(description = "Devise ISO 3 lettres, doit etre celle du compte") String currency,
                                     @ToolParam(description = "Categorie du marchand, ex: GROCERY", required = false) String merchantCategory,
                                     @ToolParam(description = "Code pays, ex: MA", required = false) String country) {
        return transactionService.create(UUID.fromString(accountId), BigDecimal.valueOf(amount),
                currency, merchantCategory, country);
    }

    @Tool(name = "transfer", description = "Effectue un virement d'un compte source vers un compte destination")
    public List<Transaction> transfer(@ToolParam(description = "UUID du compte source") String fromAccountId,
                                      @ToolParam(description = "UUID du compte destination") String toAccountId,
                                      @ToolParam(description = "Montant strictement positif") double amount,
                                      @ToolParam(description = "Devise ISO 3 lettres") String currency) {
        return transactionService.transfer(UUID.fromString(fromAccountId), UUID.fromString(toAccountId),
                BigDecimal.valueOf(amount), currency);
    }

    @Tool(name = "getMaxTransactionAmount", description = "Retourne le plafond configure pour une transaction")
    public BigDecimal getMaxTransactionAmount() {
        return transactionService.getMaxAmount();
    }
}