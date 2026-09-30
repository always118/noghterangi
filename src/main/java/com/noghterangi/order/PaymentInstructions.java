package com.noghterangi.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentInstructions {
    private final String cardNumber;
    private final String accountName;
    private final String instructions;
    public PaymentInstructions(@Value("${store.payment.card-number}") String cardNumber,
                               @Value("${store.payment.account-name}") String accountName,
                               @Value("${store.payment.instructions}") String instructions) {
        this.cardNumber = cardNumber; this.accountName = accountName; this.instructions = instructions;
    }
    public String getCardNumber() { return cardNumber; }
    public String getAccountName() { return accountName; }
    public String getInstructions() { return instructions; }
    public boolean isConfigured() { return !cardNumber.isBlank() && !accountName.isBlank() && !instructions.isBlank(); }
}
