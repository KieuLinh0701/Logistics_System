// Type API network
export interface Bank {
  name: string;
  short_name: string;
}

export interface BankApiResponse {
  code: number;
  success: boolean;
  data: Bank[];
}

// User
export interface BankAccount {
  id: number;
  bankName: string;
  accountNumber: string;
  accountName: string;
  isDefault: boolean;
  notes: string;
}

export interface BankAccountRequest {
  id?: number;
  bankName: string;
  accountNumber: string;
  accountName: string;
  isDefault: boolean;
  notes: string;
}