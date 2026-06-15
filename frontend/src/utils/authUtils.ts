import type {Account, User} from "../types/auth";
import {getDecodedToken, type DecodedToken} from "./jwt";

export function getUserRole(): string | null {
    const decoded: DecodedToken | null = getDecodedToken();
    if (!decoded || !decoded.account || !decoded.account.roleName) return null;
    return decoded.account.roleName.toLowerCase();
}

export function getCurrentAccount(): Account | null {
    const decoded: DecodedToken | null = getDecodedToken();
    if (!decoded || !decoded.account) return null;
    return decoded.account;
}

export function getCurrentUser(): User | null {
    const userStr = sessionStorage.getItem("user");
    if (!userStr) return null;
    try {
        return JSON.parse(userStr) as User;
    } catch (error) {
        console.error("Không thể parse user từ sessionStorage:", error);
        return null;
    }
}

export function getUserId(): number | null {
    const decoded = getDecodedToken();
    if (decoded?.account?.id) return Number(decoded.account.id);

    const userStr = sessionStorage.getItem("user");
    if (!userStr) return null;

    try {
        return Number(JSON.parse(userStr).id);
    } catch {
        return null;
    }
}

export function getUserPermissionGroups(): string[] {
    const decoded: DecodedToken | null = getDecodedToken();

    if (!decoded || !decoded.account || !decoded.account.permissionGroups) {
        return [];
    }

    return decoded.account.permissionGroups.map(group => group.toLowerCase());
}

export function hasPermissionGroup(requiredGroups: string[]): boolean {
    const userGroups = getUserPermissionGroups();
    return requiredGroups.some(req => userGroups.includes(req.toLowerCase()));
}

type DecodedWithOffice = {
    account?: {
        id?: number | string;
        officeId?: number | string;
        office?: { id?: number | string } | number;
        role?: string;
    };
    user?: {
        id?: number | string;
        officeId?: number | string;
        office?: { id?: number | string };
    };
};

export function getUserOfficeId(): number | null {
    const decoded = getDecodedToken() as DecodedWithOffice | null;
    if (decoded?.account?.officeId) return Number(decoded.account.officeId);

    const userStr = sessionStorage.getItem("user");
    if (!userStr) return null;

    try {
        const u = JSON.parse(userStr) as { officeId?: number | string; office?: { id?: number | string } };
        if (u.officeId) return Number(u.officeId);
        if (u.office && u.office.id) return Number(u.office.id);
        return null;
    } catch {
        return null;
    }
}
