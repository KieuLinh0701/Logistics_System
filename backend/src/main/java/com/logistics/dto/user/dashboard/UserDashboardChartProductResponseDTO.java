package com.logistics.dto.user.dashboard;

import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardChartProductResponseDTO {
    private List<UserTopProductItemDto> topSelling;
    private List<UserTopProductItemDto> topReturned;
}
