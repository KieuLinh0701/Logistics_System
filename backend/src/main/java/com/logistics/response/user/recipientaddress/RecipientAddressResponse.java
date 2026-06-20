package com.logistics.response.user.recipientaddress;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipientAddressResponse {
    private RecipientAddress address;
    private RecipientStats recipientStats;
}