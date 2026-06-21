import React from "react";
import {Button, Table, Tooltip} from "antd";
import type {ColumnsType} from "antd/es/table";
import type {ManagerEmployeePerformanceData} from "../../../../../types/employee";
import {translateEmployeeShift, translateEmployeeStatus} from "../../../../../utils/employeeUtils";

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

    const tableData = data.map((o) => ({...o, key: String(o.id)}));

    const columns: ColumnsType<ManagerEmployeePerformanceData> = [
        {
            title: "Mã nhân viên",
            key: "employeeCode",
            dataIndex: "employeeCode",
            align: "left",
            render: (_, record) => {
                return (
                    <Tooltip title="Click để xem danh sách chuyến hàng của nhân viên">
                        <span
                            className="navigate-link"
                            onClick={() => onDetail(record.id)}
                        >
                          {record.employeeCode}
                        </span>
                    </Tooltip>
                );
            }
        },
        {
            title: "Tên nhân viên",
            key: "employeeName",
            dataIndex: 'employeeName',
            align: "left",
        },
        {
            title: "Số điện thoại",
            key: "employeePhone",
            dataIndex: "employeePhone",
            align: "left",
        },
        {
            title: "Ca làm việc",
            key: "workingInfo",
            align: "center",
            render: (_, record) => (
                <div>
                    {translateEmployeeShift(record.employeeShift)}
                </div>
            ),
        },
        {
            title: "Trạng thái",
            key: "status",
            align: "center",
            render: (_, record) => (

                <div>
                    {translateEmployeeStatus(record.employeeStatus)}
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
        // Thay thế đoạn code cũ bằng 2 cột này:

        {
            title: "Tỉ lệ thành công",
            dataIndex: "completionRate", // Trỏ trực tiếp vào key của record
            key: "completionRate",
            align: "center",
            render: (value) => (
                <span>
                  {(value ?? 0).toFixed(2)}%
              </span>
            ),
        },
        {
            title: "Thời gian TB / đơn",
            dataIndex: "avgTimePerOrder",
            key: "avgTimePerOrder",
            align: "center",
            render: (value) => (
                <span>
            {(value ?? 0).toFixed(2)} phút
        </span>
            ),
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
                    DS chuyến hàng
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
                scroll={{x: "max-content"}}
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