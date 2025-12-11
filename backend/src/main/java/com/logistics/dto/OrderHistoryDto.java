package com.logistics.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderHistoryDto {
    private String fromOfficeName;
    private String toOfficeName;
    private String action;
    private String note;
    private LocalDateTime actionTime;
}