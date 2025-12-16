import React, { useState, useEffect } from "react";
import { Modal, Descriptions, Tag, Typography, Button, Select, Input } from "antd";
import { LeftOutlined, RightOutlined, CloseOutlined, EditOutlined, ArrowRightOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";
import type { Incident } from "../../../../../types/incidentReport";
import { INCIDENT_STATUSES, translateIncidentPriority, translateIncidentStatus, translateIncidentType } from "../../../../../utils/incidentUtils";

const { Text } = Typography;

interface Props {
  incident: Incident | null;
  visible: boolean;
  onClose: () => void;
  mode?: 'view' | 'edit';
  onUpdate?: (resolution: string, status: string) => void;
  initialMode?: 'view' | 'edit';
}

const IncidentDetailModal: React.FC<Props> = ({
  incident,
  visible,
  onClose,
  mode: propMode = 'view',
  onUpdate,
  initialMode
}) => {
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [resolution, setResolution] = useState<string>('');
  const [imagePreviewIndex, setImagePreviewIndex] = useState<number | null>(null);
  const navigate = useNavigate();

  const [mode, setMode] = useState<'view' | 'edit'>(initialMode || 'view');

  useEffect(() => {
    if (incident) {
      setStatus(incident.status);
      setResolution(incident.resolution || '');
      setMode(initialMode || 'view'); // ưu tiên initialMode
    }
  }, [incident, initialMode]);

  if (!incident) return null;
  const images = incident.images || [];

  const handleImageClick = (index: number) => setImagePreviewIndex(index);
  const closeImagePreview = () => setImagePreviewIndex(null);
  const showPrevImage = () => { if (imagePreviewIndex !== null) setImagePreviewIndex((imagePreviewIndex - 1 + images.length) % images.length); };
  const showNextImage = () => { if (imagePreviewIndex !== null) setImagePreviewIndex((imagePreviewIndex + 1) % images.length); };

  const handleEditClick = () => setMode('edit');
  const handleSave = () => {
    onUpdate?.(resolution, status!);
    setMode('view');
  };

  return (
    <Modal
      title={<span style={{ color: '#1C3D90', fontWeight: 'bold', fontSize: 18 }}>Chi tiết sự cố #{incident.id}</span>}
      open={visible}
      onCancel={onClose}
      width={800}
      centered
      footer={null}
      bodyStyle={{ padding: 0, position: 'relative', maxHeight: '80vh' }}
    >
      <div style={{ maxHeight: 'calc(80vh - 70px)', overflowY: 'scroll', padding: 16 }}>
        <Descriptions bordered column={1} size="middle" labelStyle={{ fontWeight: 'bold', width: 170 }}>
          {incident.order?.trackingNumber && (
            <Descriptions.Item label="Mã đơn hàng">
              <Text style={{ color: '#1890ff', cursor: 'pointer' }} onClick={() => navigate(`/orders/tracking/${incident.order.trackingNumber}`)}>
                {incident.order.trackingNumber}
              </Text>
            </Descriptions.Item>
          )}
          <Descriptions.Item label="Tiêu đề"><Text>{incident.title}</Text></Descriptions.Item>
          <Descriptions.Item label="Loại sự cố">{translateIncidentType(incident.incidentType)}</Descriptions.Item>
          <Descriptions.Item label="Độ ưu tiên">{translateIncidentPriority(incident.priority)}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            {mode === 'view' ? (
              translateIncidentStatus(status!)
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                {translateIncidentStatus(incident.status)}
                <ArrowRightOutlined style={{ fontSize: 16, color: '#1C3D90' }} />
                <Select
                  value={status}
                  onChange={setStatus}
                  style={{ width: 180 }}
                  placeholder="Chọn trạng thái"
                >
                  {INCIDENT_STATUSES.map(s => (
                    <Select.Option key={s} value={s}>
                      {translateIncidentStatus(s)}
                    </Select.Option>
                  ))}
                </Select>
              </div>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Kết quả xử lý">
            {mode === 'view'
              ? <div style={{ backgroundColor: '#fffbe6', padding: 10, borderRadius: 6, whiteSpace: 'pre-wrap' }}>{resolution || 'N/A'}</div>
              : <Input.TextArea value={resolution} onChange={e => setResolution(e.target.value)} rows={5} maxLength={1000} />
            }
          </Descriptions.Item>
          <Descriptions.Item label="Người nhận">{incident.recipientName || <Tag color="default">N/A</Tag>}</Descriptions.Item>
          <Descriptions.Item label="SĐT người nhận">{incident.recipientPhone || <Tag color="default">N/A</Tag>}</Descriptions.Item>
          <Descriptions.Item label="Người xử lý">{incident.handledBy ? `${incident.handledBy.lastName} ${incident.handledBy.firstName}` : <Tag color="default">N/A</Tag>}</Descriptions.Item>
          <Descriptions.Item label="Thời điểm xử lý">{incident.handledAt ? dayjs(incident.handledAt).format('DD/MM/YYYY HH:mm:ss') : <Tag color="default">N/A</Tag>}</Descriptions.Item>
          <Descriptions.Item label="Thời điểm tạo">{incident.createdAt ? dayjs(incident.createdAt).format('DD/MM/YYYY HH:mm:ss') : <Tag color="default">N/A</Tag>}</Descriptions.Item>
          <Descriptions.Item label="Mô tả">
            <div style={{ backgroundColor: '#e6f7ff', padding: 10, borderRadius: 6, whiteSpace: 'pre-wrap' }}>
              <Text>{incident.description || 'N/A'}</Text>
            </div>
          </Descriptions.Item>
          {images.length > 0 && (
            <Descriptions.Item label="Hình ảnh">
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                {images.map((img, index) => (
                  <img key={img} src={img} alt="Incident" style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 8, cursor: 'pointer', border: '1px solid #e0e0e0' }} onClick={() => handleImageClick(index)} />
                ))}
              </div>
            </Descriptions.Item>
          )}
        </Descriptions>
      </div>

      {/* Footer nút cố định đáy */}
      <div
        style={{
          position: 'sticky',
          bottom: 0,
          background: '#fff',
          padding: 16,
          borderTop: '1px solid #f0f0f0',
          display: 'flex',
          justifyContent: 'flex-end',
          zIndex: 10,
        }}
      >
        {mode === 'view' ? (
          ["pending", "processing"].includes(status || '') && (
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={handleEditClick}
              style={{ backgroundColor: '#1C3D90', color: '#fff' }}
            >
              Xử lý
            </Button>
          )
        ) : (
          <>
            <Button
              type="primary"
              onClick={handleSave}
              style={{ backgroundColor: '#1C3D90', color: '#fff', marginRight: 8 }}
            >
              Lưu
            </Button>
            <Button onClick={() => setMode('view')}>Hủy</Button>
          </>
        )}
      </div>

      {/* Image overlay */}
      {imagePreviewIndex !== null && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh', background: 'rgba(0,0,0,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 }}>
          <Button type="text" icon={<CloseOutlined />} onClick={closeImagePreview} style={{ position: 'absolute', top: 20, right: 20, color: '#fff', fontSize: 24 }} />
          <Button type="text" icon={<LeftOutlined />} onClick={showPrevImage} style={{ position: 'absolute', left: 20, color: '#fff', fontSize: 36 }} />
          <img src={images[imagePreviewIndex]} alt="Preview" style={{ maxWidth: '90vw', maxHeight: '90vh', objectFit: 'contain' }} />
          <Button type="text" icon={<RightOutlined />} onClick={showNextImage} style={{ position: 'absolute', right: 20, color: '#fff', fontSize: 36 }} />
        </div>
      )}

      <style>
        {`
          div::-webkit-scrollbar { display: none; }
          div { -ms-overflow-style: none; scrollbar-width: none; }
        `}
      </style>
    </Modal>
  );
};

export default IncidentDetailModal;