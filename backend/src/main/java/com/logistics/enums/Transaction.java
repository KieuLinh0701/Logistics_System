package com.logistics.enums;

public class Transaction {
    public enum TransactionType {
        INCOME, EXPENSE
    }

    public enum TransactionMethod {
        CASH, ONLINE
    }

    public enum TransactionPurpose {
        REFUND, 
        COD_RETURN, 
        SHIPPING_SERVICE, 
        OFFICE_EXPENSE, 
        REVENUE_TRANSFER
    }

    public enum TransactionStatus {
        PENDING, 
        SUCCESS, 
        FAILED
    }
}
