import React, { useState, useEffect } from 'react';
import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { translateEmployeeShift, translateEmployeeStatus } from '../../../../../utils/employeeUtils';
import type { ManagerShipperAssignment } from '../../../../../types/shipperAssignment';
import locationApi from '../../../../../api/locationApi';
import dayjs from 'dayjs';

interface AssignmentHistoryTableProps {
  data: ManagerShipperAssignment[];
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, limit?: number) => void;
}

const AssignmentHistoryTable: React.FC<AssignmentHistoryTableProps> = ({
  data,
  page,
  limit,
  total,
  loading,
  onPageChange,
}) => {

  const [locations, setLocations] = useState<Record<number, { ward: string; city: string }>>({});

  useEffect(() => {
    const fetchLocations = async () => {
      const map: Record<number, { ward: string; city: string }> = {};
      for (const assignment of data) {
        if (!map[assignment.id]) {
          const cityName = await locationApi.getCityNameByCode(assignment.cityCode) || assignment.cityCode;
          const wardName = await locationApi.getWardNameByCode(assignment.cityCode, assignment.wardCode) || assignment.wardCode;
          map[assignment.id] = { city: String(cityName), ward: String(wardName) };
        }
      }
      setLocations(map);
    };
    fetchLocations();
  }, [data]);

  const columns: ColumnsType<ManagerShipperAssignment> = [
    {
      title: "Mã NV",
      dataIndex: ["employee", "code"],
      key: "code",
      align: "center",
      render: (_, record) => (
        <span className="custom-table-content-strong">{record.employee.code}</span>
      ),
    },
    {
      title: "Thông tin cá nân",
      dataIndex: "employee",
      key: "namePhoneEmail",
      align: "left",
      render: (_, record) => (
        <div>
          <div className='custom-table-content-limit'>{record.employee.lastName} {record.employee.firstName}</div>
          <div className="text-muted">{record.employee.phoneNumber}</div>
          <div className="custom-table-content-limit text-muted">{record.employee.email}</div>
        </div>
      ),
    },
    {
      title: "Thông tin công việc",
      dataIndex: "shiftStatus",
      key: "shiftStatus",
      align: "left",
      render: (_, record) => (
        <div>
          <div>{translateEmployeeShift(record.employee.shift)}</div>
          <div className="text-muted">{translateEmployeeStatus(record.employee.status)}</div>
        </div>
      ),
    },
    {
      title: "Vị trí phân công",
      dataIndex: "region",
      key: "region",
      align: "left",
      render: (_, record) => (
        <div>
          <span>{locations[record.id]?.ward || record.wardCode},</span><br />
          <span>{locations[record.id]?.city || record.cityCode}</span>
        </div>
      ),
    },
    {
      title: "Thời gian hiệu lực",
      dataIndex: "time",
      key: "time",
      align: "left",
      render: (_, record) => (
        <div>
          <span className="text-muted">
            {dayjs(record.startAt).format("DD/MM/YYYY HH:mm")} -
          </span><br />
          <span className="text-muted">
            {record.endAt ? dayjs(record.endAt).format("DD/MM/YYYY HH:mm") : "N/A"}
          </span>
        </div>
      ),
    },
    {
      title: "Ngày tạo",
      dataIndex: "createdAt",
      key: "createdAt",
      align: "center",
      render: (_, record) => (
        <span className="text-muted">
          {dayjs(record.createdAt).format("DD/MM/YYYY HH:mm")}
        </span>
      ),
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
  ];

  const tableData = data.map((record, idx) => ({ ...record, key: String(idx + 1 + (page - 1) * limit) }));

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
          onChange: (page, pageSize) => onPageChange(page, pageSize),
        }}
      />
    </div>
  );
};

export default AssignmentHistoryTable;
