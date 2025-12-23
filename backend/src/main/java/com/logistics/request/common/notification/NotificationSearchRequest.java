package com.logistics.request.common.notification;

import lombok.*;

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
