export interface Account {
  id: number;
  email: string;
  phoneNumber: string;
  role: string;
  lastLoginAt: Date;
  createdAt: Date;
}

export interface AccountResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}