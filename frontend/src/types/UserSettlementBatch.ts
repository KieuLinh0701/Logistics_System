import type { User } from "./user";

export interface UserSettlementSchedule {
    id: number;
    weekdays: string[];
    user: User;
}