import React from "react";
import { Table, Tag, Tooltip, Button, message, Dropdown } from "antd";
import { DownOutlined, EditOutlined, EyeOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { ColumnsType } from "antd/es/table";
import type { Incident } from "../../../../../types/incidentReport";
import { canEditManagerIncident, translateIncidentPriority, translateIncidentStatus, translateIncidentType } from "../../../../../utils/incidentUtils";

interface Props {
  incidents: Incident[];
  onViewIncident: (incident: Incident) => void;
  onEdit: (incident: Incident) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, pageSize?: number) => void;
  loading: boolean;
}

const IncidentTable: React.FC<Props> = ({
  incidents,
  onViewIncident,
  currentPage,
  pageSize,
  total,
  onPageChange,
  onEdit,
  loading
}) => {
  const navigate = useNavigate();

  const tableData = incidents.map((i) => ({ ...i, key: String(i.id) }));

  const columns: ColumnsType<any> = [
    {
      title: 'Mã sự cố',
      dataIndex: 'code',
      key: 'code',
      align: 'left',
      render: (_, record) => {
        return (
          <Tooltip title="Click để xem chi tiết sự cố">
            <span
              className="navigate-link"
              onClick={() => onViewIncident(record)}
            >
              {record.code}
            </span>
          </Tooltip>
        );
      }
    },
    {
      title: "Mã đơn hàng",
      key: "trackingNumber",
      align: "center",
      render: (_, record) => {
        const trackingNumber = record?.order?.trackingNumber;
        if (!trackingNumber) return <Tag color="default">N/A</Tag>;
        return (
          <Tooltip title="Click để xem chi tiết đơn hàng">
            <span
              className="navigate-link"
              onClick={() => navigate(`/orders/tracking/${record.orderTrackingNumber}`)}
            >
              {record.orderTrackingNumber}
            </span>
          </Tooltip>
        );
      },
    },
    { title: "Tiêu đề", dataIndex: "title", key: "title", align: "center" },
    { title: "Loại sự cố", dataIndex: "incidentType", key: "incidentType", align: "center", render: (t) => translateIncidentType(t) },
    { title: "Trạng thái", dataIndex: "status", key: "status", align: "center", render: (_, record) => translateIncidentStatus(record.status) },
    { title: "Độ ưu tiên", dataIndex: "priority", key: "priority", align: "center", render: (p) => translateIncidentPriority(p) },
    {
      key: "actions",
      align: "center",
      render: (_, record) => {
        const canEdit = canEditManagerIncident(record.status);

        const items = [
          {
            key: "detail",
            label: "Xem",
            onClick: () => onViewIncident(record),
          },
          {
            key: "process",
            label: "Xử lý",
            disabled: !canEdit,
            onClick: () => onEdit(record),
          },
        ];

        return (
          <Dropdown menu={{ items }} trigger={["click"]}>
            <Button className="dropdown-trigger-button">
              Thao tác <DownOutlined />
            </Button>
          </Dropdown>
        );
      },
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
}

export default IncidentTable;