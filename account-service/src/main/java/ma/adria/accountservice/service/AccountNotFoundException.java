package ma.adria.accountservice.service;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID id) {
        super("Compte introuvable : " + id);
    }
}