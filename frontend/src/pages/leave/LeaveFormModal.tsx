import {DatePicker, Form, Input, Modal, Select} from "antd";
import dayjs, {Dayjs} from "dayjs";
import {useEffect} from "react";
import type {CreateLeavePayload, LeaveReasonType, LeaveShift} from "../../types/leave";

const { TextArea } = Input;

type LeaveFormValues = {
  leaveDate: Dayjs;
  shift: LeaveShift;
  reasonType: LeaveReasonType;
  customReason?: string;
  employeeNote?: string;
};

type Props = {
  open: boolean;
  loading: boolean;
  onCancel: () => void;
  onSubmit: (payload: CreateLeavePayload) => Promise<void>;
};

const LeaveFormModal: React.FC<Props> = ({ open, loading, onCancel, onSubmit }) => {
  const [form] = Form.useForm<LeaveFormValues>();

  const reasonType = Form.useWatch("reasonType", form);

  useEffect(() => {
    if (!open) {
      form.resetFields();
    }
  }, [open, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await onSubmit({
      leaveDate: values.leaveDate.format("YYYY-MM-DD"),
      shift: values.shift,
      reasonType: values.reasonType,
      customReason: values.customReason?.trim() || undefined,
      employeeNote: values.employeeNote?.trim() || undefined,
    });
  };

  return (
    <Modal
      title="Xin nghỉ phép"
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText="Gửi đơn"
      cancelText="Hủy"
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          label="Ngày nghỉ"
          name="leaveDate"
          rules={[{ required: true, message: "Vui lòng chọn ngày nghỉ" }]}
        >
          <DatePicker
            style={{ width: "100%" }}
            format="DD/MM/YYYY"
            disabledDate={(current) => !!current && current < dayjs().startOf("day")}
          />
        </Form.Item>

        <Form.Item
          label="Ca nghỉ"
          name="shift"
          rules={[{ required: true, message: "Vui lòng chọn ca nghỉ" }]}
        >
          <Select
            options={[
              { label: "Sáng", value: "MORNING" },
              { label: "Chiều", value: "AFTERNOON" },
              { label: "Tối", value: "EVENING" },
              { label: "Cả ngày", value: "FULL_DAY" },
            ]}
          />
        </Form.Item>

        <Form.Item
          label="Lý do nghỉ"
          name="reasonType"
          rules={[{ required: true, message: "Vui lòng chọn lý do nghỉ" }]}
        >
          <Select
            options={[
              { label: "Ốm bệnh", value: "SICK" },
              { label: "Cá nhân", value: "PERSONAL" },
              { label: "Gia đình", value: "FAMILY" },
              { label: "Khẩn cấp", value: "EMERGENCY" },
              { label: "Khác", value: "OTHER" },
            ]}
          />
        </Form.Item>

        {reasonType === "OTHER" && (
          <Form.Item
            label="Lý do cụ thể"
            name="customReason"
            rules={[
              { required: true, message: "Vui lòng nhập lý do cụ thể" },
              { whitespace: true, message: "Lý do cụ thể không được để trống" },
            ]}
          >
            <TextArea rows={4} maxLength={500} showCount />
          </Form.Item>
        )}

        <Form.Item
          label="Ghi chú thêm"
          name="employeeNote"
          rules={[{ max: 1000, message: "Ghi chú không được vượt quá 1000 ký tự" }]}
        >
          <TextArea rows={4} maxLength={1000} showCount placeholder="Nhập ghi chú thêm (nếu có)" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default LeaveFormModal;
