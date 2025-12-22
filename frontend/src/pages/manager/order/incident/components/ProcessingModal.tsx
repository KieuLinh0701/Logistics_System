import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Descriptions,
  Typography,
  Tooltip,
  Upload,
  message,
} from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import { useNavigate } from 'react-router-dom';
import type { Incident, ManagerIncidentUpdateRequest } from '../../../../../types/incidentReport';
import { formatAddress } from '../../../../../utils/locationUtils';
import {
  getAllowedManagerIncidentReportStatuses,
  translateIncidentPriority,
  translateIncidentStatus,
  translateIncidentType,
} from '../../../../../utils/incidentUtils';
import incidentReportApi from '../../../../../api/incidentReportApi';

const { Text } = Typography;

interface ProcessingModalProps {
  open: boolean;
  data: Incident;
  onSuccess: () => void;
  onCancel: () => void;
}

const ProcessingModal: React.FC<ProcessingModalProps> = ({
  open,
  data,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [address, setAddress] = useState<string>('N/A');
  const [files, setFiles] = useState<UploadFile[]>([]);

  useEffect(() => {
    console.log("hi")
    console.log("data", data);
    console.log("open", open);
    if (!data || !open) return;

    const loadAddress = async () => {
      if (data.address) {
        try {
          const full = await formatAddress(
            data.address.detail || '',
            data.address.wardCode || 0,
            data.address.cityCode || 0
          );
          setAddress(full);
        } catch {
          setAddress(data.address.detail || '');
        }
      } else {
        setAddress('N/A');
      }
    };
    loadAddress();

    const imgs = (data.images || []).map((url: any, idx: number) => ({
      uid: idx.toString(),
      name: `image-${idx + 1}`,
      status: 'done' as const,
      url,
      thumbUrl: url,
    }));
    setFiles(imgs);


    form.setFieldsValue({
      resolution: data.resolution || '',
    });
  }, [data, open, form]);

  useEffect(() => {
    if (open) {
      form.resetFields(['status', 'resolution']); // reset cả 2
      form.setFieldsValue({ resolution: data.resolution || '' });
    }
  }, [open, data]);

  const handlePreview = (file: UploadFile) => {
    if (!file.url) return;
    const win = window.open();
    if (!win) return;
    win.document.write(`
      <html>
        <head><title>Xem ảnh</title></head>
        <body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;">
          <img src="${file.url}" style="max-width:90%;max-height:90%;object-fit:contain;" />
        </body>
      </html>
    `);
  };

  const handleViewOrder = () => {
    if (data.order?.trackingNumber) {
      navigate(`/orders/tracking/${data.order.trackingNumber}`);
    }
  };
  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      setLoading(true);
      const payload = new FormData();
      payload.append('status', values.status);
      payload.append('resolution', values.resolution || '');

      const param: ManagerIncidentUpdateRequest = {
        status: values.status,
        resolution: values.resolution
      }

      const result = await incidentReportApi.processingManagerIncidentReport(data.id, param)

      if (result.success) {
        message.success('Cập nhật xử lý thành công!');
        form.resetFields();
        onSuccess();
        onCancel();
      } else {
        message.error(result.message || 'Có lỗi xảy ra');
      }
    } catch (error: any) {
      message.error(error.message || 'Có lỗi xảy ra');
    } finally {
      setLoading(false);
    }
  };

  if (!data) return null;

  return (
    <Modal
      open={open}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      title={<span className="modal-title">Xử lý báo cáo #{data.code}</span>}
      width={700}
      centered
      className="modal-hide-scrollbar"
      okButtonProps={{ className: "modal-ok-button", loading }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      okText="Cập nhật xử lý"
    >
      <Form form={form} layout="vertical">

        <Descriptions bordered column={1} size="middle">
          {data.order?.trackingNumber && (
            <Descriptions.Item label="Mã đơn hàng">
              <Tooltip title="Click để xem chi tiết đơn hàng">
                <span
                  className="navigate-link-default"
                  onClick={handleViewOrder}
                >
                  {data.order.trackingNumber}
                </span>
              </Tooltip>
            </Descriptions.Item>
          )}

          <Descriptions.Item label="Tiêu đề">
            <span className="custom-table-content-strong">
              {data.title}
            </span>
          </Descriptions.Item>

          <Descriptions.Item label="Loại sự cố">
            {translateIncidentType(data.incidentType)}
          </Descriptions.Item>

          <Descriptions.Item label="Độ ưu tiên">
            {translateIncidentPriority(data.priority)}
          </Descriptions.Item>

          <Descriptions.Item label="Thời gian báo cáo">
            <Text>
              {data.createdAt
                ? new Date(data.createdAt).toLocaleString('vi-VN')
                : 'N/A'}
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Trạng thái">
            {translateIncidentStatus(data.status)}
          </Descriptions.Item>

          <Descriptions.Item label="Mô tả">
            <div className="incident-report-detail-modal-desc-background">
              <Text>
                {data.description || <span className="text-muted">'N/A'</span>}
              </Text>
            </div>
          </Descriptions.Item>

          <Descriptions.Item label="Địa chỉ">
            <Text>{address}</Text>
          </Descriptions.Item>

          {files.length > 0 && (
            <Descriptions.Item label="Ảnh đính kèm">
              <Upload
                listType="picture-card"
                fileList={files}
                showUploadList={{ showRemoveIcon: false }}
                onPreview={handlePreview}
              />
            </Descriptions.Item>
          )}

          <Descriptions.Item label="Người gửi">
            <div>
              <Text>{data.shipper?.fullName}</Text><br />
              <Text className="text-muted">
                {data.shipper?.phoneNumber}
              </Text>
            </div>
          </Descriptions.Item>
        </Descriptions>

        <Form.Item
          label={<span className="modal-lable">Trạng thái xử lý</span>}
          name="status"
          rules={[{ required: true, message: 'Chọn trạng thái!' }]}
        >
          <Select
            className="modal-custom-select"
            placeholder="Chọn trạng thái xử lý...">
            {getAllowedManagerIncidentReportStatuses(data.status).map(s => (
              <Select.Option key={s} value={s}>
                {translateIncidentStatus(s)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Nội dung xử lý</span>}
          name="resolution"
          rules={[{ max: 1000, message: 'Tối đa 1000 ký tự!' }]}
          getValueFromEvent={(e) => {
            const value = e.target.value;
            return value === "" ? null : value;
          }}
        >
          <Input.TextArea
            className="modal-custom-input-textarea"
            rows={4}
            placeholder="Nhập nội dung xử lý..." />
        </Form.Item>

      </Form>
    </Modal>
  );
};

export default ProcessingModal;