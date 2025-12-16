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

export interface ManagerPaymentSubmissionEditRequest {
  status: string;
  notes: string;
}
