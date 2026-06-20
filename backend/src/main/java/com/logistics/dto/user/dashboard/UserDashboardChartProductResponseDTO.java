package com.logistics.dto.user.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardChartProductResponseDTO {
    private List<UserTopProductItemDto> topSelling;
    private List<UserTopProductItemDto> topReturned;
}
