export interface User {
  id: number;
  firstName: string;
  lastName: string;
  images: string;
  phoneNumber: string;
}

export function getFullName(user: User): string {
  return `${user.lastName} ${user.firstName}`.trim();
}

export interface Account {
  id: number;
  email: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface RegisterData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
}

export interface VerifyRegisterOtpData {
  email: string;
  otp: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
}

export interface ForgotPasswordEmailData  {
  email: string;
}

export interface VerifyResetOTPData {
  email: string;
  otp: string;
}

export interface ForgotPasswordResetData {
  email: string;
  newPassword: string;
}

export interface TokenResponse {
  token: string;
  user: User;
}