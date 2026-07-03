package com.logistics.service.user;

import com.logistics.request.user.recipientaddress.RecipientAddressUserRequest;
import com.logistics.request.user.recipientaddress.RecipientSuggestionRequest;
import com.logistics.request.user.recipientaddress.UserRecipientAddressSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.user.recipientaddress.RecipientAddressResponse;
import com.logistics.response.user.recipientaddress.RecipientSuggestionAddressResponse;

public interface RecipientAddressUserService {
    ListResponse<RecipientAddressResponse> list(int userId, UserRecipientAddressSearchRequest request);
    RecipientAddressResponse create(int userId, RecipientAddressUserRequest request);
    RecipientAddressResponse update(int userId, int id, RecipientAddressUserRequest request);
    void delete(int userId, int id);
    RecipientSuggestionAddressResponse getRecipientSuggestion(Integer userId, RecipientSuggestionRequest request);
    byte[] export(Integer userId, UserRecipientAddressSearchRequest request);
}