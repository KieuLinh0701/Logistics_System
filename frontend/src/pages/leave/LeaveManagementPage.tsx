import { Button, DatePicker, message, Popconfirm, Row, Space, Table, Tabs, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useState } from "react";
import { CheckCircleOutlined, CloseCircleOutlined, TeamOutlined } from "@ant-design/icons";
import dayjs, { type Dayjs } from "dayjs";
import leaveApi from "../../api/leaveApi";
import type { ApproveLeavePayload, LeaveItem } from "../../types/leave";
import "../../styles/ListPage.css";

const { Title } = Typography;

const getStatusColor = (status: string) => {
  switch (status) {
    case "PENDING":
      return "orange";
    case "APPROVED":
      return "green";
    case "REJECTED":
      return "red";
    case "CANCELLED":
      return "default";
    default:
      return "default";
  }
};

const prettyStatus = (status: string) => {
  switch (status) {
    case "PENDING":
      return "Chờ duyệt";
    case "APPROVED":
      return "Đã duyệt";
    case "REJECTED":
      return "Đã từ chối";
    case "CANCELLED":
      return "Đã hủy";
    default:
      return status;
  }
};

const prettyShift = (shift: string) => {
  switch (shift) {
    case "MORNING":
      return "Sáng";
    case "AFTERNOON":
      return "Chiều";
    case "EVENING":
      return "Tối";
    case "FULL_DAY":
      return "Cả ngày";
    default:
      return shift;
  }
};

const prettyReason = (reason: string) => {
  switch (reason) {
    case "SICK":
      return "Ốm bệnh";
    case "PERSONAL":
      return "Cá nhân";
    case "FAMILY":
      return "Gia đình";
    case "EMERGENCY":
      return "Khẩn cấp";
    case "OTHER":
      return "Khác";
    default:
      return reason;
  }
};

const LeaveManagementPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [processingAction, setProcessingAction] = useState<string | null>(null);
  const [rows, setRows] = useState<LeaveItem[]>([]);
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await leaveApi.getOfficeLeaves();
      if (!res.success) {
        message.error(res.message || "Không thể tải danh sách đơn nghỉ phép của bưu cục");
        return;
      }
      setRows(res.data || []);
    } catch {
      message.error("Không thể tải danh sách đơn nghỉ phép của bưu cục");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleApprove = async (leaveId: number, status: "APPROVED" | "REJECTED") => {
    const payload: ApproveLeavePayload = {
      status,
    };

    const actionKey = `${leaveId}-${status}`;
    setProcessingAction(actionKey);
    setSaving(true);
    try {
      const res = await leaveApi.approveLeave(leaveId, payload);
      if (!res.success) {
        message.error(res.message || "Cập nhật trạng thái đơn nghỉ phép thất bại");
        return;
      }
      message.success(res.message || "Cập nhật trạng thái đơn nghỉ phép thành công");
      await fetchData();
    } catch {
      message.error("Cập nhật trạng thái đơn nghỉ phép thất bại");
    } finally {
      setProcessingAction(null);
      setSaving(false);
    }
  };

  const columns: ColumnsType<LeaveItem> = [
    {
      title: "Nhân viên",
      dataIndex: "employeeName",
      key: "employeeName",
      align: "center",
    },
    {
      title: "Ngày nghỉ",
      dataIndex: "leaveDate",
      key: "leaveDate",
      align: "center",
      render: (value: string) => dayjs(value).format("DD/MM/YYYY"),
    },
    {
      title: "Ca nghỉ",
      dataIndex: "shift",
      key: "shift",
      align: "center",
      render: (value: string) => prettyShift(value),
    },
    {
      title: "Lý do",
      dataIndex: "reasonDisplay",
      key: "reasonDisplay",
      align: "center",
      render: (_: string, record) => (
        record.reasonType === "OTHER" ? (record.customReason || "Khác") : prettyReason(record.reasonType)
      ),
    },
    {
      title: "Ghi chú nhân viên",
      dataIndex: "employeeNote",
      key: "employeeNote",
      align: "center",
      render: (value: string | null) => value || "-",
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      align: "center",
      render: (value: string) => <Tag color={getStatusColor(value)}>{prettyStatus(value)}</Tag>,
    },
    {
      title: "Thao tác",
      key: "action",
      align: "center",
      render: (_, record) => (
        record.status === "PENDING" ? (
          <Space>
            <Popconfirm
              title="Xác nhận duyệt đơn nghỉ phép này?"
              onConfirm={() => handleApprove(record.id, "APPROVED")}
              okText="Duyệt"
              cancelText="Hủy"
            >
              <Button
                className="success-button"
                icon={<CheckCircleOutlined />}
                loading={saving && processingAction === `${record.id}-APPROVED`}
              >
                Duyệt
              </Button>
            </Popconfirm>
            <Popconfirm
              title="Xác nhận từ chối đơn nghỉ phép này?"
              onConfirm={() => handleApprove(record.id, "REJECTED")}
              okText="Từ chối"
              cancelText="Hủy"
            >
              <Button
                danger
                icon={<CloseCircleOutlined />}
                loading={saving && processingAction === `${record.id}-REJECTED`}
              >
                Từ chối
              </Button>
            </Popconfirm>
          </Space>
        ) : "-"
      ),
    },
  ];

  const leavesInSelectedDate = rows.filter(
    (item) => item.status === "APPROVED" && dayjs(item.leaveDate).isSame(selectedDate, "day"),
  );

  const onDateChange = (date: Dayjs | null) => {
    if (!date) {
      return;
    }
    setSelectedDate(date);
  };

  const dailyColumns: ColumnsType<LeaveItem> = [
    {
      title: "Nhân viên",
      dataIndex: "employeeName",
      key: "employeeName",
      align: "center",
    },
    {
      title: "Ca nghỉ",
      dataIndex: "shift",
      key: "shift",
      align: "center",
      render: (value: string) => prettyShift(value),
    },
    {
      title: "Lý do",
      dataIndex: "reasonDisplay",
      key: "reasonDisplay",
      align: "center",
      render: (_: string, record) => (
        record.reasonType === "OTHER" ? (record.customReason || "Khác") : prettyReason(record.reasonType)
      ),
    },
  ];

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <Row className="list-page-header" justify="space-between" align="middle">
          <Title level={3} className="list-page-title-main">
            <TeamOutlined className="title-icon" />
            Quản lý đơn nghỉ phép
          </Title>
        </Row>

        <Tabs
          defaultActiveKey="approval"
          items={[
            {
              key: "approval",
              label: "Duyệt nghỉ phép",
              children: (
                <Table
                  rowKey="id"
                  className="list-page-table"
                  columns={columns}
                  loading={loading}
                  dataSource={rows}
                  pagination={false}
                  bordered
                />
              ),
            },
            {
              key: "daily",
              label: "Danh sách nhân viên nghỉ trong ngày",
              children: (
                <>
                  <Row className="list-page-header" justify="space-between" align="middle" style={{ marginBottom: 12 }}>
                    <Title level={4} className="list-page-title-main" style={{ margin: 0 }}>
                      Nhân viên nghỉ trong ngày {selectedDate.format("DD/MM/YYYY")}
                    </Title>
                    <Space>
                      <DatePicker value={selectedDate} format="DD/MM/YYYY" onChange={onDateChange} allowClear={false} />
                      <Tag color="blue">{leavesInSelectedDate.length} nhân viên</Tag>
                    </Space>
                  </Row>

                  <Table
                    rowKey="id"
                    className="list-page-table"
                    columns={dailyColumns}
                    loading={loading}
                    dataSource={leavesInSelectedDate}
                    pagination={false}
                    bordered
                    locale={{ emptyText: "Không có nhân viên nghỉ trong ngày này" }}
                  />
                </>
              ),
            },
          ]}
        />
      </div>
    </div>
  );
};

export default LeaveManagementPage;
