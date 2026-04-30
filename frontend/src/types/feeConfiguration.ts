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

export interface QueryState {
  page: number;
  limit: number;
  search: string;
  feeType?: string;
  serviceTypeId?: number;
  active?: boolean;
}

export interface Option {
  label: string;
  value: string;
}

export interface FeeConfigurationsTableProps {
  loading: boolean;
  rows: FeeConfiguration[];
  onView: (record: FeeConfiguration) => void;
  onEdit: (record: FeeConfiguration) => void;
  onDelete: (id: number) => void;
  feeTypeLabel: (value: string) => string;
}

export type UpdateFeeConfigurationPayload = Partial<CreateFeeConfigurationPayload>;
