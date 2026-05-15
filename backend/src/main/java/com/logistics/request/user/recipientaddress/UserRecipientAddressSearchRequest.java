package com.logistics.request.user.recipientaddress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRecipientAddressSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String sort;
    private String startDate;
    private String endDate;
}
