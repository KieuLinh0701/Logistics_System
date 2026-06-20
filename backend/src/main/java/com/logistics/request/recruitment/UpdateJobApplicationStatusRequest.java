package com.logistics.request.recruitment;

import com.logistics.enums.JobApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobApplicationStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private JobApplicationStatus status;
}
