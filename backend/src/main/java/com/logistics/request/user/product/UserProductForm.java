package com.logistics.request.user.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UserProductForm (
        Integer id,

        @NotBlank(message = "Tên sản phẩm không được để trống")
        String name,

        @NotNull(message = "Trọng lượng không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Trọng lượng phải lớn hơn 0")
        BigDecimal weight,

        @NotNull(message = "Giá không được để trống")
        @Min(value = 0, message = "Giá không được âm")
        Integer price,

        @NotBlank(message = "Loại sản phẩm không được để trống")
        String type,

        String status,
        Integer stock,
        MultipartFile imageFile
) {}
