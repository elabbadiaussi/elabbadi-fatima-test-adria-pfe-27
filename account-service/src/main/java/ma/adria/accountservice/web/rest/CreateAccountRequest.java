package ma.adria.accountservice.web.rest;

public record CreateAccountRequest(String customerReference, String currency, double initialBalance) {
}