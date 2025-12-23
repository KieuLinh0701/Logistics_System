package com.logistics.request.user.bankAccount;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountRequest {
    private String bankName; 
    private String accountNumber; 
    private String accountName;
    private Boolean isDefault;
    private String notes;
}
