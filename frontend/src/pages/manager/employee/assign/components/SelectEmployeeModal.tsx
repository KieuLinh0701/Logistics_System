import React, { useEffect } from "react";
import { Modal, Input, Table, Select, Tag, Row, Col } from "antd";
import { SearchOutlined } from "@ant-design/icons";
import type { ManagerEmployee } from "../../../../../types/employee";
import type { ColumnsType } from "antd/es/table";
import { translateEmployeeShift } from "../../../../../utils/employeeUtils";

interface Props {
  open: boolean;
  employees: ManagerEmployee[];
  page: number;
  limit: number;
  total: number;
  selectedEmployee: ManagerEmployee | null;
  loading?: boolean;
  onClose: () => void;
  onSearch: (value: string) => void;
  onSelectEmployee: (employee: ManagerEmployee) => void;
  onPageChange: (page: number, limit?: number) => void;
}

const SelectEmployeeModal: React.FC<Props> = ({
  open,
  employees,
  page,
  limit,
  total,
  selectedEmployee,
  loading,
  onClose,
  onSearch,
  onSelectEmployee,
  onPageChange,
}) => {

  const [tempSelected, setTempSelected] = React.useState<ManagerEmployee | null>(selectedEmployee);

  const handleConfirm = () => {
    if (tempSelected) {
      onSelectEmployee(tempSelected);
      onClose();
    }
  };

  useEffect(() => {
    if (open) {
      setTempSelected(selectedEmployee);
    }
  }, [open, selectedEmployee]);

  const mergedEmployees = React.useMemo(() => {
    if (!selectedEmployee) return employees;
    if (page !== 1) return employees;

    const map = new Map<number, ManagerEmployee>();

    map.set(selectedEmployee.id, selectedEmployee);

    employees.forEach(emp => {
      map.set(emp.id, emp);
    });

    return Array.from(map.values());
  }, [employees, selectedEmployee, page]);

  const columns: ColumnsType<ManagerEmployee> = [
    {
      title: "Họ & Tên",
      key: "fullName",
      render: (_, record) => `${record.lastName} ${record.firstName}`,
    },
    {
      title: "Số điện thoại",
      dataIndex: "phoneNumber",
      key: "phoneNumber",
      align: "center",
    },
    {
      title: "Email",
      dataIndex: "email",
      key: "email",
    },
    {
      title: "Ca làm",
      dataIndex: "shift",
      key: "shift",
      align: "center",
      render: (shift) => {
        return translateEmployeeShift(shift);
      },
    }
  ];

  return (
    <Modal
      title={<span className="modal-title">Chọn nhân viên</span>}
      open={open}
      onCancel={onClose}
      onOk={handleConfirm}
      okText={
        tempSelected
          ? `Chọn: ${tempSelected.lastName} ${tempSelected.firstName}`
          : "Chọn"
      }
      okButtonProps={{
        className: "modal-ok-button",
        disabled: !tempSelected,
        loading,
      }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      cancelText="Hủy"
      width={800}
      className="modal-hide-scrollbar"
      zIndex={1100}
    >
      <div className="search-filters-container">
        <Row className="search-filters-row" gutter={16}>
          <Col span={24}>
            <Input
              className="search-input"
              placeholder="Tìm theo tên hoặc số điện thoại..."
              allowClear
              onChange={(e) => onSearch(e.target.value)}
              prefix={<SearchOutlined />}
            />
          </Col>
        </Row>
      </div>

      <div className="manager-shipper-assigns-divide"/>

      <Tag className="list-page-tag">Kết quả trả về: {total} nhân viên</Tag>

      <div className="table-container">
        <Table
          rowKey="id"
          dataSource={mergedEmployees}
          loading={loading}
          scroll={{ x: "max-content" }}
          className="list-page-table"
          pagination={{
            current: page,
            pageSize: limit,
            total: total,
            onChange: (newPage, newLimit) => onPageChange(newPage, newLimit),
          }}
          rowSelection={{
            type: "radio",
            selectedRowKeys: tempSelected ? [tempSelected.id] : [],
            onChange: (_, selectedRows) => {
              setTempSelected(selectedRows[0] as ManagerEmployee);
            },
          }}
          columns={columns}
        />
      </div>
    </Modal>
  );
};

export default SelectEmployeeModal;