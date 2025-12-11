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

export interface PromotionPublicRequest {
  page?: number; 
  limit?: number;
}

export interface PromotionUserRequest {
  page?: number; 
  limit?: number;
  search?: string;
  serviceFee?: number;
  weight?: number;
  serviceTypeId?: number;
}