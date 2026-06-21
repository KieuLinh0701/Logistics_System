import {Button, message, Popconfirm, Row, Table, Tag, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import {useEffect, useState} from "react";
import {CalendarOutlined, PlusOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import LeaveFormModal from "./LeaveFormModal";
import leaveApi from "../../api/leaveApi";
import type {CreateLeavePayload, LeaveItem} from "../../types/leave";
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

const MyLeavePage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [rows, setRows] = useState<LeaveItem[]>([]);
  const [openModal, setOpenModal] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await leaveApi.getMyLeaves();
      if (!res.success) {
        message.error(res.message || "Không thể tải danh sách đơn nghỉ phép");
        return;
      }
      setRows(res.data || []);
    } catch {
      message.error("Không thể tải danh sách đơn nghỉ phép");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateLeave = async (payload: CreateLeavePayload) => {
    setSubmitting(true);
    try {
      const res = await leaveApi.createLeave(payload);
      if (!res.success) {
        message.error(res.message || "Gửi đơn xin nghỉ thất bại");
        return;
      }
      message.success("Gửi đơn xin nghỉ thành công");
      setOpenModal(false);
      await fetchData();
    } catch {
      message.error("Gửi đơn xin nghỉ thất bại");
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelLeave = async (id: number) => {
    setLoading(true);
    try {
      const res = await leaveApi.cancelLeave(id);
      if (!res.success) {
        message.error(res.message || "Hủy đơn xin nghỉ thất bại");
        return;
      }
      message.success("Hủy đơn xin nghỉ thành công");
      await fetchData();
    } catch {
      message.error("Hủy đơn xin nghỉ thất bại");
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<LeaveItem> = [
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
      render: (_: string, record) => record.reasonType === "OTHER"
        ? record.customReason || "Khác"
        : prettyReason(record.reasonType),
    },
    {
      title: "Ghi chú bạn đã nhập",
      dataIndex: "employeeNote",
      key: "employeeNote",
      align: "center",
      render: (value: string | null) => value || "Chưa nhập",
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
          <Popconfirm
            title="Bạn có chắc muốn hủy đơn nghỉ phép này?"
            onConfirm={() => handleCancelLeave(record.id)}
            okText="Đồng ý"
            cancelText="Không"
          >
            <Button danger>Hủy đơn</Button>
          </Popconfirm>
        ) : "-"
      ),
    },
  ];

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <Row className="list-page-header" justify="space-between" align="middle">
          <Title level={3} className="list-page-title-main">
            <CalendarOutlined className="title-icon" />
            Đơn xin nghỉ của tôi
          </Title>
          <Button className="primary-button" icon={<PlusOutlined />} onClick={() => setOpenModal(true)}>
            Xin nghỉ phép
          </Button>
        </Row>

        <Table
          rowKey="id"
          className="list-page-table"
          columns={columns}
          loading={loading}
          dataSource={rows}
          pagination={false}
          bordered
        />

        <LeaveFormModal
          open={openModal}
          loading={submitting}
          onCancel={() => setOpenModal(false)}
          onSubmit={handleCreateLeave}
        />
      </div>
    </div>
  );
};

export default MyLeavePage;
