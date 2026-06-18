package com.logistics.request.user.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoleUserRequest(

        @NotBlank(message = "Tên nhóm quyền không được để trống")
        String name,

        @NotBlank(message = "Mô tả nhóm quyền không được để trống")
        String description,

        @NotEmpty(message = "Phải chọn ít nhất một nhóm quyền")
        List<Integer> permissionGroupIds
) {}