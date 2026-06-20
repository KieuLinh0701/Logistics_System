package com.logistics.request.common.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private Boolean isRead;
}
