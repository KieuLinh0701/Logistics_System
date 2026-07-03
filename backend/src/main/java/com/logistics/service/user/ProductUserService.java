package com.logistics.service.user;

import com.logistics.dto.ProductDto;
import com.logistics.entity.OrderProduct;
import com.logistics.request.user.product.UserBulkProductForm;
import com.logistics.request.user.product.UserProductForm;
import com.logistics.request.user.product.UserProductSearchRequest;
import com.logistics.response.BulkResponse;
import com.logistics.response.ListResponse;

import java.util.List;

public interface ProductUserService {
    ListResponse<ProductDto> list(int userId, UserProductSearchRequest request);
    ProductDto create(int userId, UserProductForm request);
    ProductDto update(int userId, UserProductForm request);
    ProductDto delete(int userId, Integer productId);
    BulkResponse<ProductDto> createBulk(Integer userId, UserBulkProductForm request);
    ListResponse<ProductDto> getActiveAndInstockUserProducts(int userId, UserProductSearchRequest request);
    void restoreStockFromOrder(List<OrderProduct> orderProducts);
    byte[] export(Integer userId, UserProductSearchRequest request);
}