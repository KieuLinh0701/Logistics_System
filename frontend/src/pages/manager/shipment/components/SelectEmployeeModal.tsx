import React, { useEffect, useState } from "react";
import { Modal, Input, Table, Tag, Row, Col, message, Form } from "antd";
import { SearchOutlined } from "@ant-design/icons";
import type { ManagerEmployee } from "../../../../types/employee";
import type { ColumnsType } from "antd/es/table";
import { translateEmployeeShift } from "../../../../utils/employeeUtils";
import employeeApi from "../../../../api/employeeApi";

interface Props {
  open: boolean;
  role: string | null;
  value?: ManagerEmployee | null;
  onSelect: (employee: ManagerEmployee) => void;
  onCancel: () => void;
}

const SelectEmployeeModal: React.FC<Props> = ({
  open,
  role,
  value,
  onSelect,
  onCancel
}) => {

  const [employees, setEmployees] = useState<ManagerEmployee[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState("");
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [total, setTotal] = useState(0);
  const [tempSelected, setTempSelected] = useState<ManagerEmployee | null>(value || null);

  useEffect(() => {
    console.log("role", role);
    if (!open || !role) return;

    const fetchEmployees = async () => {
      try {
        setLoading(true);
        const res = await employeeApi.getManagerActiveEmployeesByShipmentType({
          type: role,
          search: searchText || undefined,
          page,
          limit
        });

        if (res.success && res.data) {
          setEmployees(res.data.list || []);
          setTotal(res.data.pagination?.total || 0);
        } else {
          message.error(res.message || "Lấy danh sách nhân viên thất bại");
        }
      } catch (err: any) {
        message.error(err.message || "Lỗi khi lấy danh sách nhân viên");
      } finally {
        setLoading(false);
      }
    };

    fetchEmployees();
  }, [open, role, searchText, page, limit]);

  useEffect(() => {
    if (open) {
      setTempSelected(value || null);
      setPage(1);
      setSearchText("");
    }
  }, [open, value]);

  const handleConfirm = () => {
    if (tempSelected) onSelect(tempSelected);
  };

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
      onCancel={onCancel}
      onOk={handleConfirm}
      okText="Chọn"
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
              onChange={(e) => {
                setSearchText(e.target.value);
                setPage(1);
              }}
              prefix={<SearchOutlined />}
            />
          </Col>
        </Row>
      </div>

      <div className="manager-shipper-assigns-divide" />

      <Tag className="list-page-tag">Kết quả trả về: {total} nhân viên</Tag>

      <div className="table-container">
        <Table
          rowKey="id"
          dataSource={employees}
          loading={loading}
          scroll={{ x: "max-content" }}
          className="list-page-table"
          pagination={{
            current: page,
            pageSize: limit,
            total: total,
            onChange: (p, l) => {
              setPage(p);
              if (l) setLimit(l);
            },
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