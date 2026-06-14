import type {ApiResponse, ListResponse} from "../types/response";
import axiosClient from "./axiosClient";
import type {Role, UserRoleSearchRequest} from "../types/role.ts";

const roleApi = {
    // Admin
    async getAdminRoles() {
        const res = await axiosClient.get<ApiResponse<Role[]>>('/admin/roles');
        return res;
    },

    // User
    async listUserRoles(params: UserRoleSearchRequest) {
        return await axiosClient.get<ApiResponse<ListResponse<Role>>>("/user/roles", {params});
    },

    async listUserAllRoles() {
        return await axiosClient.get<ApiResponse<Role[]>>("/user/roles/all");
    },

    async getUserRoleById(roleId: number) {
        return await axiosClient.get<ApiResponse<Role>>(`/user/roles/${roleId}`);
    },

    async createUserRole(data: Role) {
        return await axiosClient.post<ApiResponse<null>>("/user/roles", data);
    },

    async updateUserRole(roleId: number, data: Role) {
        return await axiosClient.put<ApiResponse<null>>(`/user/roles/${roleId}`, data);
    },

    async deleteUserRole(roleId: number) {
        return await axiosClient.delete<ApiResponse<string>>(`/user/roles/${roleId}`);
    },

};

export default roleApi;
