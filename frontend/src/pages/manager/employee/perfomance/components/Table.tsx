import React from "react";
import { Table, Button } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { ManagerEmployeePerformanceData } from "../../../../../types/employee";

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
      key: "employeeInfo",
      align: "left",
      render: (_, record) => {
        return (
          <div>
            <div className="custom-table-content-strong">
              {record.employeeName} ({record.employeeCode})
            </div>

            <div>{record.employeeRole}</div>

            <div className="text-muted">
              {record.employeePhone || "N/A"}
            </div>
          </div>
        );
      }
    },
    {
      title: "Hiệu suất giao hàng",
      key: "performance",
      align: "center",
      render: (_, record) => {
        return (
          <div>
            <div>
              <span className="custom-table-content-strong">Tổng chuyến:</span>{" "}
              {record.totalShipments.toLocaleString("vi-VN")}
            </div>

            <div>
              <span className="custom-table-content-strong">Tổng đơn:</span>{" "}
              {record.totalOrders.toLocaleString("vi-VN")}
            </div>

            <div>
              <span className="custom-table-content-strong">Thành công:</span>{" "}
              {record.completedOrders.toLocaleString("vi-VN")}
            </div>
          </div>
        );
      }
    },
    {
      title: "Tỉ lệ & Thời gian",
      key: "rate",
      align: "center",
      render: (_, record) => {
        return (
          <div>
            <div>
              <span className="custom-table-content-strong">Tỉ lệ thành công:</span>{" "}
              {record.completionRate.toFixed(2)}%
            </div>

            <div>
              <span className="custom-table-content-strong">Thời gian TB/đơn:</span>{" "}
              {record.avgTimePerOrder.toFixed(2)} phút
            </div>
          </div>
        );
      }
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => {
        return (
          <Button
          className="action-button-link"
            type="link"
            size="small"
            onClick={() => onDetail(record.id)}
          >
            DS chuyến đi
          </Button>
        );
      }
    }
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