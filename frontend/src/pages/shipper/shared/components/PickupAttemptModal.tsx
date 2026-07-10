import React, { useState } from "react";
import {Form, Input, Modal, Select, Button, Space, Alert} from "antd";
import {PictureOutlined} from "@ant-design/icons";
import {PICKUP_FAIL_REASONS, translatePickupFailReason} from "../../../../utils/orderUtils";

const { TextArea } = Input;

interface PickupAttemptModalProps {
  open: boolean;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: { failReason: string; note?: string; file?: File | null }) => void;
}

const PickupAttemptModal: React.FC<PickupAttemptModalProps> = ({
  open,
  loading,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);

  const handleSelectImage = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImageFile(file);
      const reader = new FileReader();
      reader.onload = () => setPreview(reader.result as string);
      reader.readAsDataURL(file);
    }
    e.target.value = "";
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    setPreview(null);
  };

  const handleClose = () => {
    form.resetFields();
    setImageFile(null);
    setPreview(null);
    onCancel();
  };

  return (
    <Modal
      title="Báo lấy hàng thất bại"
      open={open}
      onCancel={handleClose}
      footer={null}
      width={680}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => {
          onSubmit({ ...values, file: imageFile });
          form.resetFields();
          setImageFile(null);
          setPreview(null);
        }}
      >
        <Alert
          message="Lấy hàng không thành công"
          description="Đơn hàng sẽ được đánh dấu là lấy hàng thất bại. Hệ thống sẽ xử lý theo quy trình retry hoặc kết thúc tùy số lần lấy hàng."
          type="error"
          showIcon
          style={{
            marginBottom: 24,
            backgroundColor: "#fff2f0",
            borderColor: "#ffccc7",
          }}
        />
        <div style={{ marginBottom: 24 }}>
          <div style={{ marginBottom: 8 }}>Ảnh minh chứng lấy hàng thất bại (tuỳ chọn)</div>
          <input
            id="pickup-fail-image-input"
            type="file"
            accept="image/*"
            style={{ display: "none" }}
            onChange={handleSelectImage}
          />
          <Space>
            <Button
              icon={<PictureOutlined />}
              onClick={() => document.getElementById("pickup-fail-image-input")?.click()}
            >
              Chọn ảnh
            </Button>
            {preview && (
              <Button danger onClick={handleRemoveImage}>
                Xóa ảnh
              </Button>
            )}
          </Space>
          {preview && (
            <div style={{ marginTop: 12 }}>
              <img
                src={preview}
                alt="Preview"
                style={{ maxWidth: "100%", maxHeight: 200, borderRadius: 8, border: "1px solid #e5e7eb" }}
              />
            </div>
          )}
        </div>
        <Form.Item
          name="failReason"
          label="Lý do thất bại"
          rules={[{ required: true, message: "Vui lòng chọn lý do" }]}
        >
          <Select placeholder="Chọn lý do">
            {PICKUP_FAIL_REASONS.map((value) => (
              <Select.Option key={value} value={value}>
                {translatePickupFailReason(value)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item name="note" label="Ghi chú (tùy chọn)">
          <TextArea rows={3} placeholder="Nhập ghi chú thêm..." />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0, textAlign: "right" }}>
          <Space>
            <Button onClick={handleClose}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={loading}>
              Gửi báo cáo
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default PickupAttemptModal;
