import React, { useMemo } from "react";
import { Table } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { JobApplication } from "../../../../../types/recruitment";
import StatusBadge from "./StatusBadge";
import Actions from "./Actions";
import "./ApplicationComponents.css";

interface ApplicationTableProps {
  data: JobApplication[];
  loading: boolean;
  actionLoading: boolean;
  currentPage: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, limit: number) => void;
  onView: (application: JobApplication) => void;
  onReviewing: (id: number) => void;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
}

const ApplicationTable: React.FC<ApplicationTableProps> = ({
  data,
  loading,
  actionLoading,
  currentPage,
  pageSize,
  total,
  onPageChange,
  onView,
  onReviewing,
  onApprove,
  onReject,
}) => {
  const columns: ColumnsType<JobApplication> = useMemo(
    () => [
      {
        title: "Ứng viên",
        render: (_, record) => (
          <div className="hr-application-candidate">
            <span>{record.fullName}</span>
            <span className="hr-application-email">{record.email}</span>
          </div>
        ),
      },
      {
        title: "Vị trí",
        dataIndex: "jobTitle",
      },
      {
        title: "Bưu cục",
        render: (_, record) => record.officeName || `#${record.officeId}`,
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (value) => <StatusBadge status={value} />,
      },
      {
        title: "Thời gian nộp",
        dataIndex: "createdAt",
        render: (value: string) => new Date(value).toLocaleString("vi-VN"),
      },
      {
        title: "",
        render: (_, record) => (
          <Actions
            record={record}
            actionLoading={actionLoading}
            onView={onView}
            onReviewing={onReviewing}
            onApprove={onApprove}
            onReject={onReject}
          />
        ),
        width: 160,
        align: 'right',
      },
    ],
    [actionLoading, onApprove, onReject, onReviewing, onView]
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

export default ApplicationTable;
