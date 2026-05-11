package com.logistics.response.user.recipientaddress;

import com.logistics.enums.RecipientAddressType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecipientSuggestionAddressResponse {

    private RecipientAddress address;
    private RecipientAddressType type;
    private RecipientStats recipientStats;
}