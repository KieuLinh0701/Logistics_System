import type { ListResponse } from "./response";

export interface Promotion {
  id: number;
  code: string;
  title?: string;
  description?: string;
  discountType: string; // PERCENTAGE | FIXED
  discountValue: number;
  isGlobal: boolean;
  maxDiscountAmount?: number;
  startDate: string;
  endDate: string;
  minOrderValue?: number;
  minWeight?: number;
  maxWeight?: number;
  minOrdersCount?: number;
  serviceTypeIds?: number[];
  firstTimeUser?: boolean;
  validMonthsAfterJoin?: number;
  validYearsAfterJoin?: number;
  usageLimit?: number;
  maxUsagePerUser?: number;
  dailyUsageLimitGlobal?: number;
  dailyUsageLimitPerUser?: number;
  usedCount: number;
  status: string; // ACTIVE | INACTIVE | EXPIRED
  userIds?: number[]; // Nếu isGlobal = false
  createdAt: string;
  updatedAt?: string;
}

export interface PublicPromotionRequest {
  page?: number; 
  limit?: number;
}

export interface CreatePromotionPayload {
  code: string;
  title?: string;
  description?: string;
  discountType: string; // PERCENTAGE | FIXED
  discountValue: number;
  isGlobal?: boolean;
  maxDiscountAmount?: number;
  startDate: string;
  endDate: string;
  minOrderValue?: number;
  minWeight?: number;
  maxWeight?: number;
  minOrdersCount?: number;
  serviceTypeIds?: number[];
  firstTimeUser?: boolean;
  validMonthsAfterJoin?: number;
  validYearsAfterJoin?: number;
  usageLimit?: number;
  maxUsagePerUser?: number;
  dailyUsageLimitGlobal?: number;
  dailyUsageLimitPerUser?: number;
  status?: string; // ACTIVE | INACTIVE | EXPIRED
  userIds?: number[]; // Nếu isGlobal = false
}

export type UpdatePromotionPayload = Partial<CreatePromotionPayload>;