package com.logistics.request.notification;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationSearchRequest {
    private Integer page = 1;
    private Integer limit = 10;
    private String search;
    private Boolean isRead;
}
