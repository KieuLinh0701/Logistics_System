export interface Account {
  id: number;
  email: string;
  phoneNumber: string;
  role: string;
  lastLoginAt: Date;
  createdAt: Date;
}