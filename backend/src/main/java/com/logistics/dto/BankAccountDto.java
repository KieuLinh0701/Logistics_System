package com.logistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountDto {
    private int id;
    private String bankName;
    private String accountNumber;
    private String accountName;

    @JsonProperty("isDefault")
    private boolean isDefault;

    private String notes;
}
