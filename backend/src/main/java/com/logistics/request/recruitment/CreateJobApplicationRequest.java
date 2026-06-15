package com.logistics.request.recruitment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobApplicationRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên không vượt quá 150 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9+()\\-\\s]{8,20}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email không vượt quá 150 ký tự")
    private String email;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ không vượt quá 255 ký tự")
    private String address;

    @NotBlank(message = "CV URL không được để trống")
    @Size(max = 500, message = "CV URL không vượt quá 500 ký tự")
    private String cvUrl;

    @NotNull(message = "jobPostingId không được để trống")
    private Long jobPostingId;
}
