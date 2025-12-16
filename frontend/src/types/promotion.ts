import type { ServiceType } from "./serviceType";

export interface Promotion {
  id: number;
  code: string;
  title: string;
  description: string;
  discountType: string;
  discountValue: number;
  isGlobal: boolean;
  minOrderValue: number;
  maxDiscountAmount: number;
  minWeight: number;
  maxWeight: number;
  minOrdersCount: number;
  serviceTypes: ServiceType[];
  firstTimeUser: boolean;
  validMonthsAfterJoin: number;
  validYearsAfterJoin: number;
  startDate: Date;
  endDate: Date;
  usageLimit: number;
  maxUsagePerUser: number;
  dailyUsageLimitGlobal: number;
  dailyUsageLimitPerUser: number;
  usedCount: number;
  status: string;
}

export interface PromotionUserRequest {
  page?: number; 
  limit?: number;
  search?: string;
  serviceFee?: number;
  weight?: number;
  serviceTypeId?: number;
}

export interface PromotionPublicRequest {
  page?: number; 
  limit?: number;
}

//
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

