import type { ManagerPaymentSubmission } from "./paymentSubmission";
import type { User } from "./user";

export interface ManagerPaymentSubmissionBatch {
  id: number;
  code: string;
  shipper: User;
  totalSystemAmount: number;
  totalActualAmount: number;
  status: string;
  checkedBy: User;
  checkedAt: string;
  notes: string;
  submissions: ManagerPaymentSubmission[];
  createdAt: string;
  updatedAt: string;
  totalOrders: number;
}

export interface ManagerPaymentSubmissionBatchEditRequest {
  status: string;
  notes: string;
}

export interface ManagerPaymentSubmissionBatchCreateRequest {
  shipperId: number;
  totalActualAmount: number;
}