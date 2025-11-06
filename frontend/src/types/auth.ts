export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: 'admin' | 'manager' | 'shipper' | 'driver' | 'user';
  isVerified: boolean;
  isActive: boolean;
  lastLoginAt?: string;
  images?: string;
  detailAddress?: string;
  codeWard?: number;
  codeCity?: number;
  createdAt: string;
  updatedAt: string;
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

export interface AuthResponse {
  accessToken: string;
  user: User;
}