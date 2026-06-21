import {useEffect, useState} from "react";
import {Alert, Button, Form, Input, message, Modal, Select, Spin, Typography,} from "antd";
import {UserOutlined, WarningOutlined} from "@ant-design/icons";
import type {
    AssignTicketPayload,
    SupportAssignManagerOption,
    SupportAssignOfficeOption,
    SupportTicket,
} from "../../types/support";
import supportApi from "../../api/supportApi";
import "./SupportModals.css";

const { Text } = Typography;
const { TextArea } = Input;

type Props = {
  open: boolean;
  ticket: SupportTicket | null;
  onCancel: () => void;
  onSuccess: (updatedTicket: SupportTicket) => void;
};

const TicketAssignModal: React.FC<Props> = ({ open, ticket, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [loadingOptions, setLoadingOptions] = useState(false);
  const [loadingManagers, setLoadingManagers] = useState(false);

  const [suggestedOffices, setSuggestedOffices] = useState<SupportAssignOfficeOption[]>([]);
  const [allOffices, setAllOffices] = useState<SupportAssignOfficeOption[]>([]);
  const [manager, setManager] = useState<SupportAssignManagerOption | null>(null);

  const selectedOfficeId = Form.useWatch("officeId", form);

  useEffect(() => {
    if (open && ticket) {
      void loadAssignOptions(ticket.id);
    }
  }, [open, ticket]);

  useEffect(() => {
    if (selectedOfficeId) {
      void loadManager(selectedOfficeId);
    } else {
      setManager(null);
    }
  }, [selectedOfficeId]);

  const loadAssignOptions = async (ticketId: number) => {
    setLoadingOptions(true);
    try {
      const res = await supportApi.getAssignOptions(ticketId);
      if (res.success && res.data) {
        setSuggestedOffices(res.data.suggestedOffices || []);
        setAllOffices(res.data.allOffices || []);

        if (res.data.suggestedOffices?.length > 0) {
          const firstOffice = res.data.suggestedOffices[0];
          form.setFieldValue("officeId", firstOffice.id);
          void loadManager(firstOffice.id);
        }
      }
    } catch {
      message.error("Không thể tải danh sách bưu cục");
    } finally {
      setLoadingOptions(false);
    }
  };

  const loadManager = async (officeId: number) => {
    setLoadingManagers(true);
    setManager(null);
    try {
      const res = await supportApi.getManagersByOffice(officeId);
      if (res.success && res.data && res.data.length > 0) {
        // Each office has only 1 manager
        setManager(res.data[0]);
      }
    } catch {
      message.error("Không thể tải thông tin Manager");
    } finally {
      setLoadingManagers(false);
    }
  };

  const handleOfficeChange = () => {
    setManager(null);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!ticket) return;

      if (!manager || !manager.accountId) {
        message.error("Bưu cục này chưa có Manager. Không thể phân công.");
        return;
      }

      const payload: AssignTicketPayload = {
        assigneeAccountId: manager.accountId,
        officeId: values.officeId,
        note: values.note,
      };

      setLoading(true);
      const res = await supportApi.assignTicket(ticket.id, payload);

      if (!res.success || !res.data) {
        message.error(res.message || "Không thể phân công ticket");
        return;
      }

      message.success("Đã phân công ticket thành công");
      form.resetFields();
      setSuggestedOffices([]);
      setAllOffices([]);
      setManager(null);
      onSuccess(res.data);
    } catch {
      // validation failed
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    setSuggestedOffices([]);
    setAllOffices([]);
    setManager(null);
    onCancel();
  };

  const canSubmit = selectedOfficeId && manager && manager.accountId;

  const officeOptions: { label: string; options: { label: string; value: number }[] }[] = [];

  if (suggestedOffices.length > 0) {
    officeOptions.push({
      label: "Bưu cục gợi ý",
      options: suggestedOffices.map((o) => ({
        label: o.name,
        value: o.id,
      })),
    });
  }

  if (allOffices.length > 0) {
    officeOptions.push({
      label: "Tất cả bưu cục",
      options: allOffices
        .filter((o) => !suggestedOffices.some((s) => s.id === o.id))
        .map((o) => ({
          label: o.name,
          value: o.id,
        })),
    });
  }

  return (
    <Modal
      title={
        <div style={{ display: "flex", justifyContent: "center", alignItems: "center", gap: 8 }}>
          <UserOutlined />
          <span>Phân công ticket</span>
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
          disabled={!canSubmit}
          onClick={() => void handleSubmit()}
          style={{ background: "#1E4DB7", borderColor: "#1E4DB7" }}
          className="support-primary-btn"
        >
          Phân công
        </Button>,
      ]}
      destroyOnClose
    >
      {ticket && (
        <div
          style={{
            marginBottom: 16,
            padding: 12,
            background: "#f5f5f5",
            borderRadius: 8,
          }}
        >
          <Text strong>{ticket.code}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>
            {ticket.subject || ticket.latestMessage || "(Không có tiêu đề)"}
          </Text>
        </div>
      )}

      <Spin spinning={loadingOptions}>
        <Form
          form={form}
          layout="vertical"
          initialValues={{ note: "" }}
        >
          <Form.Item
            name="officeId"
            label="Bưu cục xử lý"
            rules={[{ required: true, message: "Vui lòng chọn bưu cục" }]}
          >
            <Select
              placeholder="Chọn bưu cục xử lý"
              options={officeOptions}
              onChange={handleOfficeChange}
              showSearch
              filterOption={(input, option) =>
                (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
              }
              disabled={loadingOptions}
            />
          </Form.Item>

          <div style={{ marginBottom: 16 }}>
            <Text strong style={{ display: "block", marginBottom: 8 }}>
              Manager xử lý
            </Text>
            {loadingManagers ? (
              <Spin size="small" />
            ) : manager ? (
              <div
                style={{
                  padding: "12px",
                  background: "#f6ffed",
                  border: "1px solid #b7eb8f",
                  borderRadius: 6,
                }}
              >
                <Text strong>{manager.fullName}</Text>
                <br />
                {manager.email && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {manager.email}
                  </Text>
                )}
                {manager.phone && (
                  <>
                    <br />
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {manager.phone}
                    </Text>
                  </>
                )}
              </div>
            ) : selectedOfficeId ? (
              <Alert
                message="Bưu cục này chưa có Manager"
                description="Vui lòng chọn bưu cục khác hoặc liên hệ quản trị viên."
                type="warning"
                showIcon
                icon={<WarningOutlined />}
              />
            ) : (
              <Text type="secondary">Vui lòng chọn bưu cục trước</Text>
            )}
          </div>

          <Form.Item name="note" label="Ghi chú (tùy chọn)">
            <TextArea
              rows={3}
              placeholder="VD: Chuyển xử lý vấn đề giao chậm"
              maxLength={1000}
            />
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
};

export default TicketAssignModal;
