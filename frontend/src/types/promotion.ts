export interface Promotion {
  id: number;
  code: string;
  description: string;
  discountType: string;
  discountValue: number;
  minOrderValue: number;
  maxDiscountAmount: number;
  startDate: Date;
  endDate: Date;
  usageLimit: number;
  usedCount: number;
  status: string;
  lastLoginAt: Date;
  createdAt: Date;
}

export interface PublicPromotionRequest {
  page?: number; 
  limit?: number;
}