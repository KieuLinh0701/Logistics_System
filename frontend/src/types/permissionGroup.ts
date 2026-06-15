export interface PermissionGroup {
    id: number;
    name: string;
    description: string;
    children: PermissionGroup[];
}