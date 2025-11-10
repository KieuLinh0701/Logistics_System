export interface User {
  id: number;
  code: string;
  firstName: string;
  lastName: string;
  fullName: string;
  images: string;
}

export interface UserResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}