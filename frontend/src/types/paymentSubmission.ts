import type { Order } from "./order";
import type { User } from "./user";

export interface ManagerPaymentSubmission {
  id: number;
  code: string;
  order: Order;
  systemAmount: number;
  actualAmount: number;
  status: string;
  checkedBy: User;
  checkedAt: string;
  notes: string;
  paidAt: string;
  updatedAt: string;
}

export interface PaymentSubmission {
  id: number;
  code?: string;
  orderId?: number;
  trackingNumber?: string;
  systemAmount: number;
  actualAmount: number;
  status: string;
  notes?: string;
  paidAt?: string;
  checkedAt?: string;
}

export interface ManagerPaymentSubmissionEditRequest {
  status: string;
  notes: string;
}

export interface AdminPaymentSubmissionListDto {
  id: number;
  code?: string;
  systemAmount?: number;
  actualAmount?: number;
  order?: { trackingNumber?: string };
  status?: string;
  checkedBy?: { lastName?: string; firstName?: string; phoneNumber?: string };
  checkedAt?: string;
  paidAt?: string;
  updatedAt?: string;
  notes?: string;
}
