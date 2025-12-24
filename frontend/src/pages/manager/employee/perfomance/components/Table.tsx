import React from "react";
import { Table, Button, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { ManagerEmployeePerformanceData } from "../../../../../types/employee";
import { translateEmployeeShift, translateEmployeeStatus } from "../../../../../utils/employeeUtils";
import { translateRoleName } from "../../../../../utils/roleUtils";

interface Props {
  data: ManagerEmployeePerformanceData[];
  onDetail: (employeeId: number) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, pageSize?: number) => void;
}

const EmployeeTable: React.FC<Props> = ({
  data,
  onDetail,
  currentPage,
  pageSize,
  total,
  loading,
  onPageChange,
}) => {

  const tableData = data.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<ManagerEmployeePerformanceData> = [
    {
      title: "Nhân viên",
      key: "employee",
      align: "left",
      render: (_, record) => (
        <div>
          <div className="custom-table-content-strong">
            {record.employeeName}
          </div>
          <div className="text-muted text-sm">
            {record.employeePhone}
          </div>
          <div className="text-muted text-sm">
            {record.employeeCode} - {
              (() => {
                const r = record.employeeRole ? String(record.employeeRole).toLowerCase() : "";
                const ROLE_LABELS: Record<string, string> = {
                  admin: "Quản trị viên",
                  manager: "Quản lý bưu cục",
                  user: "Cửa hàng",
                  shipper: "Nhân viên giao hàng",
                  driver: "Tài xế lái xe",
                  staff: "Nhân viên",
                  employee: "Nhân viên",
                };
                const getRoleColor = (role: string) => {
                  const colors: Record<string, string> = {
                    admin: "red",
                    manager: "blue",
                    staff: "green",
                    driver: "purple",
                    user: "gold",
                    shipper: "orange",
                    employee: "cyan",
                  };
                  return colors[role] || "default";
                };
                return <Tag color={getRoleColor(r)}>{ROLE_LABELS[r] || record.employeeRole}</Tag>;
              })()
            }
          </div>
        </div>
      ),
    },
    {
      title: "Làm việc",
      key: "workingInfo",
      align: "left",
      render: (_, record) => (
        <div>
          <div>
            {translateEmployeeShift(record.employeeShift)}
          </div>
          <div className="text-muted text-sm">
            ({translateEmployeeStatus(record.employeeStatus)})
          </div>
        </div>
      ),
    },
    {
      title: "Số Chuyến",
      key: "shipments",
      align: "center",
      render: (_, record) => (
        <div>
          {record.totalShipments.toLocaleString("vi-VN")}
        </div>
      ),
    },
    {
      title: "Tổng đơn",
      dataIndex: "totalOrders",
      key: "totalOrders",
      align: "center",
      render: (value) => (value ?? 0).toLocaleString("vi-VN"),
    },
    {
      title: "Đơn thành công",
      dataIndex: "completedOrders",
      key: "completedOrders",
      align: "center",
      render: (value) => (value ?? 0).toLocaleString("vi-VN"),
    },
    {
      title: "Hiệu suất",
      key: "performance",
      align: "left",
      render: (_, record) => {
        const completionRate = record.completionRate ?? 0;
        const avgTimePerOrder = record.avgTimePerOrder ?? 0;

        return (
          <div>
            <div>
              <span className="custom-table-content-strong">
                Tỉ lệ giao thành công:
              </span>{" "}
              {completionRate.toFixed(2)}%
            </div>

            <div>
              <span className="custom-table-content-strong">
                Thời gian TB / đơn:
              </span>{" "}
              {avgTimePerOrder.toFixed(2)} phút
            </div>
          </div>
        );
      },
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => (
        <Button
          className="action-button-link"
          type="link"
          size="small"
          onClick={() => onDetail(record.id)}
        >
          DS chuyến đi
        </Button>
      ),
    },
  ];

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="id"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize,
          total,
          onChange: onPageChange,
        }}
      />
    </div>
  );
};

export default EmployeeTable;