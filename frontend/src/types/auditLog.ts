import type {User} from "./user.ts";
import type {Role} from "./role.ts";
import type {Office} from "./office.ts";

export interface AuditLog {
  id: number;
  entity: string;
  entityId: string;
  action: string;
  description: string;
  status: string;
  createdAt: string;
  user: User;
  office: Office;
}

export interface AuditLogSearchRequest {
  page: number;
  limit: number;
  search?: string;
  action?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  entity?: string;
  sort?: string;
}