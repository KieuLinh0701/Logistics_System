import type { ListResponse } from "./response";

export interface FeeConfiguration {
  id: number;
  serviceTypeId?: number;
  serviceTypeName?: string;
  feeType: string; // COD | PACKAGING | INSURANCE | VAT
  calculationType: string; // FIXED | PERCENTAGE
  feeValue: number;
  minOrderFee?: number;
  maxOrderFee?: number;
  active: boolean;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateFeeConfigurationPayload {
  serviceTypeId?: number;
  feeType: string; // COD | PACKAGING | INSURANCE | VAT
  calculationType: string; // FIXED | PERCENTAGE
  feeValue: number;
  minOrderFee?: number;
  maxOrderFee?: number;
  active?: boolean;
  notes?: string;
}

export type UpdateFeeConfigurationPayload = Partial<CreateFeeConfigurationPayload>;


