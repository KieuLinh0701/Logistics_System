import type { User } from "../types/auth";
import { getDecodedToken, type DecodedToken } from "./jwt";

// Lấy role của người dùng từ token (luôn lowercase)
export function getUserRole(): string | null {
  const decoded: DecodedToken | null = getDecodedToken();
  if (!decoded || !decoded.account || !decoded.account.role) return null;
  return decoded.account.role.toLowerCase();
}

// Lấy user của người dùng từ token
export function getCurrentUser(): User | null {
  const decoded: DecodedToken | null = getDecodedToken();
  if (!decoded || !decoded.user) return null;
  return decoded.user;
}