import React, {useState} from "react";
import {Button, Descriptions, Image, Modal, Space, Tooltip, Typography} from "antd";
import type {Incident} from "../../../../../types/incidentReport";
import {
    canEditManagerIncident,
    translateIncidentPriority,
    translateIncidentStatus,
    translateIncidentType
} from "../../../../../utils/incidentUtils";
import {EditOutlined} from "@ant-design/icons";

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
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewImages, setPreviewImages] = useState<string[]>([]);
  const [previewIndex, setPreviewIndex] = useState(0);

  if (!incident) return null;

  const images = incident.images || [];
  const hasImages = images.length > 0;

  const handleViewOrder = () => {
    if (incident.order?.trackingNumber) {
      onViewOrderDetail?.(incident.order.trackingNumber);
    }
  };

  const handleImageClick = (index: number) => {
    setPreviewImages(images);
    setPreviewIndex(index);
    setPreviewVisible(true);
  };

  const handleModalClose = () => {
    setPreviewVisible(false);
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
        <Descriptions.Item label="Mã đơn hàng">
          {incident.order?.trackingNumber ? (
            <div onClick={handleViewOrder}>
              <Tooltip title="Click để xem chi tiết đơn hàng">
                <span className="navigate-link-default">
                  {incident.order.trackingNumber}
                </span>
              </Tooltip>
            </div>
          ) : (
            <Text className="text-muted">Không liên quan đơn hàng</Text>
          )}
        </Descriptions.Item>

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

        <Descriptions.Item label="Ảnh đính kèm">
          {hasImages ? (
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
              {images.map((url, index) => (
                <Image
                  key={index}
                  src={url}
                  width={80}
                  height={80}
                  style={{ objectFit: "cover", cursor: "pointer", borderRadius: 4 }}
                  onClick={() => handleImageClick(index)}
                  preview={false}
                />
              ))}
            </div>
          ) : (
            <Text className="text-muted">Không có ảnh đính kèm</Text>
          )}
        </Descriptions.Item>

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
      <Image.PreviewGroup
        preview={{
          visible: previewVisible,
          onVisibleChange: (vis) => setPreviewVisible(vis),
          current: previewIndex,
        }}
      >
        {previewImages.map((url, index) => (
          <Image key={index} src={url} style={{ display: "none" }} />
        ))}
      </Image.PreviewGroup>
    </Modal>
  );
};

export default IncidentDetailModalUser;