package com.logistics.request.admin;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBatchRequest {
    private Integer shipperId;
    private List<Integer> submissionIds;
    private Integer officeId;
}
