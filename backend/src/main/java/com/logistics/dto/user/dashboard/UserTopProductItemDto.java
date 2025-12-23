package com.logistics.dto.user.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserTopProductItemDto {
    private Integer id;
    private String name;
    private long total;
}