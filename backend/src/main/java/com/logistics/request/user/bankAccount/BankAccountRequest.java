package com.logistics.request.user.bankAccount;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountRequest {

    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    private String accountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    private String accountName;

    private Boolean isDefault;

    private String notes;
}