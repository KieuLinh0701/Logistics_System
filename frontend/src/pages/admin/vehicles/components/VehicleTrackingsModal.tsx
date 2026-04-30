import React from "react";
import { Modal } from "antd";

interface TrackingPoint {
  id: number;
  latitude: number;
  longitude: number;
  recordedAt: string;
}

interface VehicleTrackingsModalProps {
  open: boolean;
  loading: boolean;
  points: TrackingPoint[];
  onClose: () => void;
}

const VehicleTrackingsModal: React.FC<VehicleTrackingsModalProps> = ({
  open,
  loading,
  points,
  onClose,
}) => {
  return (
    <Modal title="Hành trình xe" open={open} onCancel={onClose} footer={null} width={800}>
      {loading ? (
        <div>Đang tải...</div>
      ) : points.length === 0 ? (
        <div>Không có dữ liệu hành trình</div>
      ) : (
        <div style={{ maxHeight: 420, overflow: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", borderBottom: "1px solid #e5e7eb" }}>
                <th style={{ padding: 8 }}>Thời gian</th>
                <th style={{ padding: 8 }}>Toạ độ</th>
              </tr>
            </thead>
            <tbody>
              {points.map((point) => (
                <tr key={point.id} style={{ borderBottom: "1px solid #f3f4f6" }}>
                  <td style={{ padding: 8 }}>{new Date(point.recordedAt).toLocaleString()}</td>
                  <td style={{ padding: 8 }}>
                    {point.latitude}, {point.longitude}
                    <a
                      style={{ marginLeft: 8 }}
                      href={`https://www.google.com/maps/search/?api=1&query=${point.latitude},${point.longitude}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Mở bản đồ
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  );
};

export default VehicleTrackingsModal;
