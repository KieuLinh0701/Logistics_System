package com.logistics.request.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
