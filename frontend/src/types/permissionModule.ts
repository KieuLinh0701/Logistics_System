import type {PermissionGroup} from "./permissionGroup.ts";

export interface PermissionModule {
    id: number;
    name: string;
    description: string;
    permissionGroups: PermissionGroup[];
}