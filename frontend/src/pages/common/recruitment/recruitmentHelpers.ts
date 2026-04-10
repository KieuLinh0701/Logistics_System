import type { JobApplicationStatus, JobPostingStatus, RecruitmentRoleType } from "../../../types/recruitment";

export const roleTypeLabelMap: Record<RecruitmentRoleType, string> = {
  DRIVER: "Tài xế",
  SHIPPER: "Nhân viên giao hàng",
  WAREHOUSE_STAFF: "Nhân viên kho",
  RECONCILIATION_STAFF: "Nhân viên đối soát",
};

export const postingStatusLabelMap: Record<JobPostingStatus, string> = {
  OPEN: "Đang mở",
  CLOSED: "Đã đóng",
};

export const applicationStatusLabelMap: Record<JobApplicationStatus, string> = {
  PENDING: "Chờ xử lý",
  REVIEWING: "Đang xem xét",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
};

export const postingStatusColorMap: Record<JobPostingStatus, string> = {
  OPEN: "green",
  CLOSED: "default",
};

export const applicationStatusColorMap: Record<JobApplicationStatus, string> = {
  PENDING: "gold",
  REVIEWING: "blue",
  APPROVED: "green",
  REJECTED: "red",
};

export const roleTypeOptions = [
  { label: roleTypeLabelMap.DRIVER, value: "DRIVER" },
  { label: roleTypeLabelMap.SHIPPER, value: "SHIPPER" },
  { label: roleTypeLabelMap.WAREHOUSE_STAFF, value: "WAREHOUSE_STAFF" },
  { label: roleTypeLabelMap.RECONCILIATION_STAFF, value: "RECONCILIATION_STAFF" },
];

export const jobStatusOptions = [
  { label: postingStatusLabelMap.OPEN, value: "OPEN" },
  { label: postingStatusLabelMap.CLOSED, value: "CLOSED" },
];

export const applicationStatusOptions = [
  { label: applicationStatusLabelMap.PENDING, value: "PENDING" },
  { label: applicationStatusLabelMap.REVIEWING, value: "REVIEWING" },
  { label: applicationStatusLabelMap.APPROVED, value: "APPROVED" },
  { label: applicationStatusLabelMap.REJECTED, value: "REJECTED" },
];
