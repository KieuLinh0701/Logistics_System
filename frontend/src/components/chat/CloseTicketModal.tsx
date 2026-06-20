import { useState } from "react";
import { Modal, Form, Input, Button, Typography, message } from "antd";
import { CheckCircleOutlined } from "@ant-design/icons";
import type { SupportTicket, CloseTicketPayload } from "../../types/support";
import supportApi from "../../api/supportApi";

const { Text } = Typography;
const { TextArea } = Input;

type Props = {
  open: boolean;
  ticket: SupportTicket | null;
  onCancel: () => void;
  onSuccess: (updatedTicket: SupportTicket) => void;
};

const CloseTicketModal: React.FC<Props> = ({ open, ticket, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!ticket) return;

      const payload: CloseTicketPayload = {
        note: values.note || undefined,
      };

      setLoading(true);
      const res = await supportApi.closeTicket(ticket.id, payload);

      if (!res.success || !res.data) {
        message.error(res.message || "Không thể đánh dấu giải quyết");
        return;
      }

      message.success("Đã đánh dấu giải quyết thành công");
      form.resetFields();
      onSuccess(res.data);
    } catch {
      // validation failed
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title={
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <CheckCircleOutlined style={{ color: "#52c41a" }} />
          <span>Đánh dấu đã giải quyết</span>
        </div>
      }
      open={open}
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          icon={<CheckCircleOutlined />}
          onClick={() => void handleSubmit()}
        >
          Đã giải quyết
        </Button>,
      ]}
      destroyOnClose
    >
      {ticket && (
        <div style={{ marginBottom: 16, padding: 12, background: "#f6ffed", borderRadius: 8, border: "1px solid #b7eb8f" }}>
          <Text strong>{ticket.code}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>
            {ticket.subject || ticket.latestMessage || "(Không có tiêu đề)"}
          </Text>
        </div>
      )}

      <Text type="secondary" style={{ display: "block", marginBottom: 12 }}>
        Bạn có chắc muốn đánh dấu yêu cầu này là đã giải quyết? Khách hàng vẫn có thể nhắn tiếp nếu cần.
      </Text>

      <Form form={form} layout="vertical" initialValues={{ note: "" }}>
        <Form.Item name="note" label="Ghi chú (tùy chọn)">
          <TextArea rows={3} placeholder="VD: Đã xử lý xong vấn đề giao chậm" maxLength={1000} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CloseTicketModal;
