export interface UpdateProfileRequest {
  id?: number;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  avatarFile?: File;
}

export interface UpadtePasswordData {
  oldPassword: string;
  newPassword: string;
}

export interface UpadteEmailData {
  newEmail: string;
  password: string;
}

export interface VerifyEmailUpdateOTPData {
  newEmail: string;
  otp: string;
}