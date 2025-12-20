import React, { useState, useEffect } from 'react';
import { Button, Dropdown, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { translateEmployeeShift, translateEmployeeStatus } from '../../../../../utils/employeeUtils';
import type { ManagerEmployee, ManagerEmployeeWithShipperAssignments } from '../../../../../types/employee';
import type { ManagerShipperAssignment } from '../../../../../types/shipperAssignment';
import { DownOutlined, PlusCircleOutlined } from '@ant-design/icons';
import locationApi from '../../../../../api/locationApi';
import dayjs from 'dayjs';

interface EmployeeTableProps {
  data: ManagerEmployeeWithShipperAssignments[];
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onEdit: (employee: ManagerEmployee, assignment: ManagerShipperAssignment) => void;
  onAdd: (employee: ManagerEmployee) => void;
  onDelete: (id: number) => void;
  onPageChange: (page: number, limit?: number) => void;
}

const EmployeeTable: React.FC<EmployeeTableProps> = ({
  data,
  page,
  limit,
  total,
  loading,
  onEdit,
  onAdd,
  onDelete,
  onPageChange,
}) => {

  const [assignmentLocations, setAssignmentLocations] = useState<Record<number, { ward: string; city: string }>>({});

  useEffect(() => {
    const fetchAllLocations = async () => {
      const map: Record<number, { ward: string; city: string }> = {};
      for (const employee of data) {
        for (const assignment of employee.assignments) {
          if (!map[assignment.id]) {
            const cityName = await locationApi.getCityNameByCode(assignment.cityCode) || assignment.cityCode;
            const wardName = await locationApi.getWardNameByCode(assignment.cityCode, assignment.wardCode) || assignment.wardCode;
            map[assignment.id] = { city: String(cityName), ward: String(wardName) };
          }
        }
      }
      setAssignmentLocations(map);
    };

    fetchAllLocations();
  }, [data]);

  const columns: ColumnsType<ManagerEmployeeWithShipperAssignments> = [
    {
      title: "Mã NV",
      dataIndex: "code",
      key: "code",
      align: "center",
      render: (_, record) => (
        <span className="custom-table-content-strong">
          {record.employee.code}
        </span>
      ),
    },
    { title: "Họ tên", dataIndex: "fullName", key: "fullName", align: "left", render: (_, record) => `${record.employee.lastName} ${record.employee.firstName}` },
    { title: "Email", dataIndex: "email", key: "email", align: "left", render: (_, record) => record.employee.email },
    { title: "SĐT", dataIndex: "phoneNumber", key: "phoneNumber", align: "center", render: (_, record) => record.employee.phoneNumber },
    { title: "Ca làm", dataIndex: "shift", key: "shift", align: "center", render: (_, record) => translateEmployeeShift(record.employee.shift) },
    { title: "Trạng thái", dataIndex: "status", key: "status", align: "center", render: (_, record) => translateEmployeeStatus(record.employee.status) },
    { title: "Số vùng đang đảm nhận", dataIndex: "assignments", key: "assignments", align: "center", render: (_, record) => record.assignments?.length || 0 },
    {
      title: "Hành động",
      key: "action",
      align: "center",
      render: (_, record) => (
        <Space>
          <Button
            className="action-button-link"
            type="link"
            icon={<PlusCircleOutlined />}
            onClick={() => onAdd(record.employee)}
          >
            Tạo mới
          </Button>
        </Space>
      ),
    },
  ];

  const expandedRowRender = (record: ManagerEmployeeWithShipperAssignments) => {
    const assignmentColumns: ColumnsType<ManagerShipperAssignment> = [
      {
        title: "Phường/Xã",
        dataIndex: "wardCode",
        key: "wardCode",
        align: "center",
        render: (_, a) => assignmentLocations[a.id]?.ward || String(a.wardCode),
      },
      {
        title: "Tỉnh/Thành phố",
        dataIndex: "cityCode",
        key: "cityCode",
        align: "center",
        render: (_, a) => assignmentLocations[a.id]?.city || String(a.cityCode),
      },
      {
        title: "Ngày bắt đầu",
        dataIndex: "startAt",
        key: "startAt",
        align: "center",
        render: (value) => value ? dayjs(value).format("DD/MM/YYYY HH:mm") : <span className='text-muted'>N/A</span>
      },
      {
        title: "Ngày kết thúc",
        dataIndex: "endAt",
        key: "endAt",
        align: "center",
        render: (value) => value ? dayjs(value).format("DD/MM/YYYY HH:mm") : <span className='text-muted'>N/A</span>
      },
      {
        title: "Ghi chú",
        dataIndex: "notes",
        key: "notes",
        align: "left",
        render: (value) => {
          if (value) {
            return <span className="custom-table-content-limit">{value}</span>;
          } else {
            return <span className="text-muted">N/A</span>;
          }
        },
      },
      {
        key: "action",
        align: "left",
        render: (_, assignment) => {
          const now = dayjs();
          const isExpired = assignment.endAt !== null && dayjs(assignment.endAt).isBefore(now);
          const notStarted = dayjs(assignment.startAt).isAfter(now);

          const items = [
            {
              key: "edit",
              label: "Chỉnh sửa",
              disabled: isExpired,
              onClick: () => onEdit(record.employee, assignment),
            },
            {
              key: "delete",
              label: "Xóa",
              disabled: !notStarted,
              onClick: () => onDelete(assignment.id),
            },
          ];

          const allDisabled = items.every(item => item.disabled);

          return (
            <Dropdown menu={{ items }} trigger={["click"]}>
              <Button className="dropdown-trigger-button" disabled={allDisabled}>
                Thao tác <DownOutlined />
              </Button>
            </Dropdown>
          );
        },
      }
    ];

    const assignmentData = record.assignments.map((a, idx) => ({ ...a, key: idx }));
    return <Table columns={assignmentColumns} dataSource={assignmentData} pagination={false} />;
  };

  const tableData = data.map((p, index) => ({ ...p, key: String(index + 1 + (page - 1) * limit) }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="key"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        loading={loading}
        expandable={{ expandedRowRender }}
        pagination={{
          current: page,
          pageSize: limit,
          total,
          onChange: (page, pageSize) => onPageChange(page, pageSize),
        }}
      />
    </div>
  );
};

export default EmployeeTable;