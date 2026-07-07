import React, {useState} from "react";
import {Image, Table, Tag, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import dayjs from "dayjs";
import defaultImage from "../../../../assets/images/imageDefault.jpg";

const { Text } = Typography;

interface IncidentReport {
  id: number;
  orderId?: number;
  trackingNumber?: string;
  incidentType?: string;
  title: string;
  description?: string;
  priority?: string;
  status?: string;
  createdAt: string;
  handledAt?: string;
  images?: string[];
}

interface IncidentReportsTableProps {
  reports: IncidentReport[];
  loading: boolean;
}

const getPriorityColor = (priority: string) => {
  switch (priority?.toLowerCase()) {
    case "low": return "green";
    case "medium": return "orange";
    case "high": return "red";
    default: return "default";
  }
};

const getPriorityText = (priority: string) => {
  switch (priority?.toLowerCase()) {
    case "low": return "Thấp";
    case "medium": return "Trung bình";
    case "high": return "Cao";
    default: return priority;
  }
};

const getStatusColor = (status: string) => {
  switch (status?.toUpperCase()) {
    case "PENDING": return "orange";
    case "PROCESSING": return "blue";
    case "RESOLVED": return "success";
    case "REJECTED": return "red";
    default: return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status?.toUpperCase()) {
    case "PENDING": return "Chờ xử lý";
    case "PROCESSING": return "Đang xử lý";
    case "RESOLVED": return "Đã giải quyết";
    case "REJECTED": return "Từ chối";
    default: return status;
  }
};

const getIncidentTypeText = (incidentType: string) => {
  switch (incidentType?.toUpperCase()) {
    case "RECIPIENT_NOT_AVAILABLE": return "Người nhận không có mặt";
    case "WRONG_ADDRESS": return "Sai địa chỉ";
    case "PACKAGE_DAMAGED": return "Hàng hóa bị hỏng";
    case "RECIPIENT_REFUSED": return "Người nhận từ chối";
    case "SECURITY_ISSUE": return "Vấn đề an ninh";
    case "OTHER": return "Khác";
    default: return incidentType;
  }
};

const IncidentReportsTable: React.FC<IncidentReportsTableProps> = ({ reports, loading }) => {
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewImages, setPreviewImages] = useState<string[]>([]);
  const [previewIndex, setPreviewIndex] = useState(0);

  const handleImageClick = (images: string[], index: number = 0) => {
    setPreviewImages(images);
    setPreviewIndex(index);
    setPreviewVisible(true);
  };

  const handleModalClose = () => {
    setPreviewVisible(false);
  };

  const renderImageCell = (images?: string[]) => {
    const hasImages = images && images.length > 0;
    const firstImage = hasImages ? images[0] : defaultImage;
    const remainingCount = hasImages ? images.length - 1 : 0;

    return (
      <div
        style={{
          position: "relative",
          display: "inline-block",
          cursor: hasImages ? "pointer" : "default",
        }}
        onClick={() => hasImages && handleImageClick(images, 0)}
      >
        <img
          src={firstImage}
          alt="incident"
          className="table-image"
        />
        {remainingCount > 0 && (
          <div
            style={{
              position: "absolute",
              bottom: 2,
              right: 2,
              background: "rgba(0, 0, 0, 0.65)",
              color: "#fff",
              fontSize: 10,
              padding: "1px 5px",
              borderRadius: 4,
              fontWeight: 500,
              lineHeight: 1.4,
            }}
          >
            +{remainingCount}
          </div>
        )}
      </div>
    );
  };

  const columns: ColumnsType<IncidentReport> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => (
        <Text strong className="shipper-table-strong" style={{ fontSize: "13px" }}>
          {text || "—"}
        </Text>
      ),
    },
    {
      title: "Tiêu đề",
      dataIndex: "title",
      key: "title",
      width: 200,
      render: (text: string) => (
        <Text ellipsis style={{ maxWidth: 200 }} className="shipper-table-strong">
          {text}
        </Text>
      ),
    },
    {
      title: "Loại sự cố",
      dataIndex: "incidentType",
      key: "incidentType",
      width: 150,
      render: (type: string) => (
        <span className="shipper-table-strong">{getIncidentTypeText(type || "")}</span>
      ),
    },
    {
      title: "Mức độ",
      dataIndex: "priority",
      key: "priority",
      width: 100,
      render: (priority: string) => (
        <Tag color={getPriorityColor(priority || "")}>{getPriorityText(priority || "")}</Tag>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: string) => (
        <Tag color={getStatusColor(status || "")}>{getStatusText(status || "")}</Tag>
      ),
    },
    {
      title: "Ảnh",
      key: "images",
      width: 95,
      align: "center",
      render: (_: any, record: IncidentReport) => renderImageCell(record.images),
    },
    {
      title: "Ngày tạo",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 150,
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
  ];

  return (
    <>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={reports}
        pagination={{ pageSize: 10 }}
      />
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
    </>
  );
};

export default IncidentReportsTable;
