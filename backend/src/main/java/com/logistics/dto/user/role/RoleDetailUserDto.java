package com.logistics.dto.user.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleDetailUserDto {
    private Integer id;
    private String name;
    private String description;
    private List<Integer> permissionGroupIds;
} 
