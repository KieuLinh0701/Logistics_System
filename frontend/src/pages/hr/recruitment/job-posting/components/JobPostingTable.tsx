import React, { useMemo } from "react";
import { Table } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { JobPosting, JobPostingStatus, RecruitmentRoleType } from "../../../../../types/recruitment";
import { postingStatusLabelMap, roleTypeLabelMap } from "../../../../common/recruitment/recruitmentHelpers";
import { shiftLabel } from "../../../../../utils/recruitmentHelpers";
import Actions from "./Actions";

interface JobPostingTableProps {
  data: JobPosting[];
  loading: boolean;
  actionLoading?: boolean;
  currentPage: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, limit: number) => void;
  onEdit: (job: JobPosting) => void;
  onToggleStatus: (job: JobPosting, nextStatus: JobPostingStatus) => void;
  onDelete: (id: number) => void;
}

const JobPostingTable: React.FC<JobPostingTableProps> = ({
  data,
  loading,
  currentPage,
  pageSize,
  total,
  onPageChange,
  onEdit,
  onToggleStatus,
  onDelete,
}) => {
  const columns: ColumnsType<JobPosting> = useMemo(
    () => [
      {
        title: "Tiêu đề",
        dataIndex: "title",
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (value: JobPostingStatus) => <span style={{ color: '#000' }}>{postingStatusLabelMap[value]}</span>,
      },
      {
        title: "Vị trí",
        dataIndex: "roleType",
        render: (value: RecruitmentRoleType) => roleTypeLabelMap[value],
      },
      {
        title: "Số lượng tuyển",
        dataIndex: "quantityNeeded",
        render: (value) => (value !== null && value !== undefined ? String(value) : "Đang cập nhật"),
      },
      {
        title: "Ca làm việc",
        dataIndex: "shift",
        render: (value) => shiftLabel(value),
      },
      {
        title: "Ngày đăng",
        render: (_, record) => (record.createdAt ? new Date(record.createdAt).toLocaleDateString("vi-VN") : "-"),
      },
      {
        title: "",
        render: (_, record) => (
          <Actions
            record={record}
            onEdit={onEdit}
            onToggleStatus={onToggleStatus}
            onDelete={onDelete}
          />
        ),
      },
    ],
    [onDelete, onEdit, onToggleStatus]
  );

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={data}
      loading={loading}
      pagination={{
        current: currentPage,
        pageSize,
        total,
        onChange: onPageChange,
      }}
    />
  );
};

export default JobPostingTable;
