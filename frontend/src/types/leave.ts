export type LeaveRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type LeaveReasonType = "SICK" | "PERSONAL" | "FAMILY" | "EMERGENCY" | "OTHER";
export type LeaveShift = "MORNING" | "AFTERNOON" | "EVENING" | "FULL_DAY";

export interface LeaveItem {
  id: number;
  employeeId: number | null;
  employeeName: string;
  officeId: number | null;
  leaveDate: string;
  shift: LeaveShift;
  reasonType: LeaveReasonType;
  customReason: string | null;
  employeeNote: string | null;
  reasonDisplay: string;
  status: LeaveRequestStatus;
  approvedById: number | null;
  approvedByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLeavePayload {
  leaveDate: string;
  shift: LeaveShift;
  reasonType: LeaveReasonType;
  customReason?: string;
  employeeNote?: string;
}

export interface ApproveLeavePayload {
  status: Extract<LeaveRequestStatus, "APPROVED" | "REJECTED">;
}
