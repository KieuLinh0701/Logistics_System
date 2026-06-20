package com.logistics.request.user.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressUserRequest {

    @Min(value = 1, message = "Mã thành phố không hợp lệ")
    private int cityCode;

    @Min(value = 1, message = "Mã phường/xã không hợp lệ")
    private int wardCode;

    @NotBlank(message = "Chi tiết địa chỉ không được để trống")
    private String detail;

    @JsonProperty("isDefault")
    private boolean isDefault;

    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không đúng định dạng")
    private String phoneNumber;

    @NotNull(message = "Vĩ độ không được để trống")
    private Double latitude;

    @NotNull(message = "Kinh độ không được để trống")
    private Double longitude;

    @NotBlank(message = "Tên thành phố không được để trống")
    private String cityName;

    @NotBlank(message = "Tên phường/xã không được để trống")
    private String wardName;
}