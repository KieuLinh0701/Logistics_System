import type {ApiResponse} from "../types/response";
import axiosClient from "./axiosClient";
import type {PermissionModule} from "../types/permissionModule.ts";

const permissionModuleApi = {

    // User
    async listUserActivePermissionModules() {
        const res = await axiosClient.get<ApiResponse<PermissionModule[]>>("/user/permission-modules/active");
        return res;
    },
};

export default permissionModuleApi;
