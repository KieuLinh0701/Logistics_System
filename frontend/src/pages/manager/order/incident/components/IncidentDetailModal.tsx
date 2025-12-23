import React, { useEffect, useState } from "react";
import { Modal, Descriptions, Typography, Upload, Tooltip, Space, Button } from "antd";
import type { Incident } from "../../../../../types/incidentReport";
import { formatAddress } from "../../../../../utils/locationUtils";
import { canEditManagerIncident, translateIncidentPriority, translateIncidentStatus, translateIncidentType } from "../../../../../utils/incidentUtils";
import type { UploadFile } from 'antd/es/upload/interface';
import { EditOutlined } from "@ant-design/icons";

const { Text } = Typography;

interface Props {
  incident: Incident | null;
  visible: boolean;
  loading: boolean;
  onClose: () => void;
  onEdit: () => void;
  onViewOrderDetail?: (trackingNumber: string) => void;
}

const IncidentDetailModalUser: React.FC<Props> = ({ incident, visible, onClose, loading, onViewOrderDetail, onEdit }) => {
  const [address, setAddress] = useState<string>('');
  const [files, setFiles] = useState<UploadFile[]>([]);

  useEffect(() => {
    if (!incident) return;

    const loadAddress = async () => {
      if (incident.address) {
        try {
          const full = await formatAddress(
            incident.address.detail || '',
            incident.address.wardCode || 0,
            incident.address.cityCode || 0
          );
          setAddress(full);
        } catch {
          setAddress(incident.address.detail || '');
        }
      } else {
        setAddress('N/A');
      }
    };
    loadAddress();

    const imgs = (incident.images || []).map((url, idx) => ({
      uid: idx.toString(),
      name: `image-${idx + 1}`,
      status: 'done' as const,
      url,
      thumbUrl: url
    }));
    setFiles(imgs);

  }, [incident]);

  if (!incident) return null;

  const handlePreview = (file: UploadFile) => {
    if (!file.url) return;
    const newWindow = window.open();
    if (!newWindow) return;
    newWindow.document.write(`
      <html>
        <head><title>Xem ảnh</title></head>
        <body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;">
          <img src="${file.url}" style="max-width:90%;max-height:90%;object-fit:contain;" />
        </body>
      </html>
    `);
  };

  const handleViewOrder = () => {
    if (incident.order.trackingNumber) {
      onViewOrderDetail?.(incident.order.trackingNumber);
    }
  };

  return (
    <Modal
      open={visible}
      onCancel={onClose}
      width={900}
      centered
      title={
        <span className="modal-title">{`Chi tiết sự cố #${incident.code}`}</span>}
      loading={loading}
      footer={[
        <Space key={`space-${incident.id}`}>
          {(canEditManagerIncident(incident.status)) && (
            <Button
              key={`handleRequest-${incident.id}`}
              type="primary"
              icon={<EditOutlined />}
              onClick={onEdit}
              className='modal-ok-button'
            >
              {incident.status === 'PENDING'
                ? 'Xử lý yêu cầu'
                : 'Cập nhật trạng thái'}
            </Button>
          )}
        </Space>
      ].filter(Boolean) as React.ReactNode[]}
      className="modal-hide-scrollbar"
    >
      <Descriptions bordered column={1} size="middle">
        {incident.order?.trackingNumber && (
          <Descriptions.Item label="Mã đơn hàng">
            <div onClick={handleViewOrder}>
              <Tooltip title="Click để xem chi tiết đơn hàng">
                <span className="navigate-link-default">
                  {incident.order.trackingNumber}
                </span>
              </Tooltip>
            </div>
          </Descriptions.Item>
        )}

        <Descriptions.Item label="Tiêu đề"><span className="custom-table-content-strong">{incident.title}</span></Descriptions.Item>
        <Descriptions.Item label="Loại sự cố">{translateIncidentType(incident.incidentType)}</Descriptions.Item>
        <Descriptions.Item label="Độ ưu tiên">{translateIncidentPriority(incident.priority)}</Descriptions.Item>
        <Descriptions.Item label="Thời gian báo cáo">
          <Text>{new Date(incident.createdAt).toLocaleString('vi-VN')}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="Trạng thái">{translateIncidentStatus(incident.status)}</Descriptions.Item>
        <Descriptions.Item label="Mô tả">
          <div className='incident-report-detail-modal-desc-background'>
            <Text>{incident.description || <span className="text-muted">'N/A'</span>}</Text>
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
            <Text>{incident.shipper.fullName}</Text><br />
            <Text className="text-muted">{incident.shipper.phoneNumber}</Text>
          </div>
        </Descriptions.Item>

        <Descriptions.Item label="Người xử lý">
          {incident.handler ? (
            <div>
              <Text strong>{incident.handler.fullName}</Text><br />
              <Text>{incident.handler.phoneNumber}</Text>
            </div>
          ) : <Text className="text-muted">N/A</Text>}
        </Descriptions.Item>

        {incident.resolution !== null && (
          <Descriptions.Item label="Hướng giải quyết">
            <div className='incident-report-detail-modal-handle-background'>
              <Text>{incident.resolution}</Text>
            </div>
          </Descriptions.Item>
        )}

        {incident.handledAt !== null && (
          <Descriptions.Item label="Thời gian xử lý">
            <Text>{new Date(incident.handledAt).toLocaleString('vi-VN')}</Text>
          </Descriptions.Item>
        )}
      </Descriptions>
    </Modal>
  );
};

export default IncidentDetailModalUser;