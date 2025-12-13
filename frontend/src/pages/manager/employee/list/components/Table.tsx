import React, { useEffect, useState } from 'react';
import { Button, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { translateEmployeeShift, translateEmployeeStatus } from '../../../../../utils/employeeUtils';
import locationApi from '../../../../../api/locationApi';
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
      render: (code,_) => <span className="custom-table-content-strong">{code}</span>,
    },
    {
      title: "Thông tin cá nhân",
      key: "personalInfo",
      align: "left",
      render: (_, record) => (
        <>
          <span className="custom-table-content-strong">Họ tên: </span>{record.userLastName} {record.userFirstName}<br />
          <span className="custom-table-content-strong">Email: </span>{record.userEmail}<br />
          <span className="custom-table-content-strong">SĐT: </span>{record.userPhoneNumber}
        </>
      ),
    },
    {
      title: "Thông tin công việc",
      key: "jobInfo",
      align: "left",
      render: (_, record) => (
        <>
          <span className="custom-table-content-strong">Chức vụ: </span>{translateRoleName(record.userRole) || "-"}<br />
          <span className="custom-table-content-strong">Ca làm: </span>{translateEmployeeShift(record.shift)}<br />
          <span className="custom-table-content-strong">Trạng thái: </span>{translateEmployeeStatus(record.status)}
        </>
      ),
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