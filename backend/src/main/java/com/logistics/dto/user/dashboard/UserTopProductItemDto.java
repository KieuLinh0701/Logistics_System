package com.logistics.dto.user.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserTopProductItemDto {
    private Integer id;
    private String name;
    private long total;
}