package com.logistics.response.user.recipientaddress;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipientAddressWithStats {
    private RecipientAddress address;
    private RecipientStats recipientStats;
}