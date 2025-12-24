import React from 'react';
import { Button, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { translateEmployeeShift, translateEmployeeStatus } from '../../../../../utils/employeeUtils';
import { translateRoleName } from '../../../../../utils/roleUtils';
import { EditOutlined } from '@ant-design/icons';
import type { ManagerEmployee } from '../../../../../types/employee';

interface EmployeeTableProps {
  data: ManagerEmployee[];
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onEdit: (employee: ManagerEmployee) => void;
  onPageChange: (page: number, limit?: number) => void;
}

const EmployeeTable: React.FC<EmployeeTableProps> = ({
  data,
  page,
  limit,
  total,
  loading,
  onEdit,
  onPageChange,
}) => {

  const columns: ColumnsType<ManagerEmployee> = [
    {
      title: "Mã NV",
      dataIndex: "code",
      key: "code",
      align: "center",
      render: (code, _) => <span className="custom-table-content-strong">{code}</span>,
    },
    {
      title: "Họ tên",
      key: "name",
      align: "left",
      render: (_, record) => (
        <>
          <span>{record.lastName} {record.firstName}</span><br />
        </>
      ),
    },
    {
      title: "Thông tin liên hệ",
      key: "personalInfo",
      align: "left",
      render: (_, record) => (
        <>
          <span>{record.phoneNumber}</span><br />
          <span>{record.email}</span>
        </>
      ),
    },
    {
      title: "Chức vụ",
      key: "role",
      dataIndex: "role",
      align: "center",
      render: (role) => {
        const key = role ? (String(role)).toLowerCase() : "";
        const ROLE_LABELS: Record<string, string> = {
          admin: "Quản trị viên",
          manager: "Quản lý bưu cục",
          user: "Cửa hàng",
          shipper: "Nhân viên giao hàng",
          driver: "Tài xế lái xe",
          staff: "Nhân viên",
          employee: "Nhân viên",
        };
        const getRoleColor = (r: string) => {
          const colors: Record<string, string> = {
            admin: "red",
            manager: "blue",
            staff: "green",
            driver: "purple",
            user: "gold",
            shipper: "orange",
            employee: "cyan",
          };
          return colors[r] || "default";
        };
        return <Tag color={getRoleColor(key)}>{ROLE_LABELS[key] || String(role)}</Tag>;
      },
    },
    {
      title: "Ca làm việc",
      key: "shift",
      dataIndex: "shift",
      align: "center",
      render: (shift) => translateEmployeeShift(shift),
    },
    {
      title: "Trạng thái",
      key: "status",
      dataIndex: "status",
      align: "center",
      render: (status) => translateEmployeeStatus(status),
    },
    {
      title: "Ngày vào làm",
      key: "hireDate",
      align: "center",
      render: (_, record) => dayjs(record.hireDate).format("YYYY-MM-DD"),
    },
    {
      title: "Hành động",
      key: "action",
      align: "center",
      render: (_, record) => (
        <Space>
          <Button
            className="action-button-link"
            type="link"
            icon={<EditOutlined />}
            disabled={record.status === "LEAVE"}
            onClick={() => onEdit(record)}
          >
            Sửa
          </Button>
        </Space>
      ),
    },
  ];

  const tableData = data.map((p, index) => ({
    ...p,
    key: String(index + 1 + (page - 1) * limit),
  }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="key"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        loading={loading}
        pagination={{
          current: page,
          pageSize: limit,
          total,
          onChange: (page, pageSize) => onPageChange(page, pageSize)
        }}
      />
    </div>
  );
};

export default EmployeeTable;