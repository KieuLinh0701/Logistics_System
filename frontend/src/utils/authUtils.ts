import type { Account, User } from "../types/auth";
import { getDecodedToken, type DecodedToken } from "./jwt";

export function getUserRole(): string | null {
  const decoded: DecodedToken | null = getDecodedToken();
  if (!decoded || !decoded.account || !decoded.account.role) return null;
  return decoded.account.role.toLowerCase();
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