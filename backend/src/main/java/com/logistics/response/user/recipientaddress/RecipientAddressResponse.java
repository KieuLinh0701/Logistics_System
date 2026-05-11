package com.logistics.response.user.recipientaddress;

import com.logistics.enums.RecipientAddressType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecipientAddressResponse {
    private RecipientAddress address;
    private RecipientStats recipientStats;
}