package com.logistics.request.user.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class UserProductForm {
        private Integer id;

        @NotBlank(message = "Tên sản phẩm không được để trống")
        private String name;

        @NotNull(message = "Trọng lượng không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Trọng lượng phải lớn hơn 0")
        private BigDecimal weight;

        @NotNull(message = "Giá không được để trống")
        @Min(value = 0, message = "Giá không được âm")
        private Integer price;

        @NotBlank(message = "Loại sản phẩm không được để trống")
        private String type;

        private String status;
        private Integer stock;
        private MultipartFile imageFile;
}
