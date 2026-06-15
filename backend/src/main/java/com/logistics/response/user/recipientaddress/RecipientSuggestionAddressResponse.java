package com.logistics.response.user.recipientaddress;

import com.logistics.enums.RecipientAddressType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RecipientSuggestionAddressResponse {

    List<RecipientAddressWithStats> addresses;
    RecipientAddressType type;}