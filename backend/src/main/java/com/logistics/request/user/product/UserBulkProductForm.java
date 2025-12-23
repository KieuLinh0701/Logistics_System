package com.logistics.request.user.product;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBulkProductForm {
    private List<UserProductForm> products;
}
