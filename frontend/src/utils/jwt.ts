import { jwtDecode } from "jwt-decode";

export interface DecodedToken {
  sub: string;
  user: {
    id: number;
    fullName: string;
    images: string;
  };
  account: {
    email: string;
    role: string;
  };
  exp: number;
  iat: number;
}

export function getDecodedToken(): DecodedToken | null {
  const token = sessionStorage.getItem("token");
  if (!token) return null;
  try {
    return jwtDecode<DecodedToken>(token);
  } catch (error) {
    console.error("Token không hợp lệ:", error);
    return null;
  }
}