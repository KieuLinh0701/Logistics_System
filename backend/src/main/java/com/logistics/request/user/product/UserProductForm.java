package com.logistics.request.user.product;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProductForm { 
    private Integer id;
    private String name;
    private BigDecimal weight;
    private Integer price;
    private String type;
    private String status;
    private Integer stock;

    private MultipartFile imageFile;
}
