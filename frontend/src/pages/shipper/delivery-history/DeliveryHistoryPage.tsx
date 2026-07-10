import React, {useEffect, useState} from "react";
import {Card, Col, message, Modal, Row, Space, Statistic, Table, Tag, Typography,} from "antd";
import {CheckCircleOutlined, ClockCircleOutlined, DollarOutlined,} from "@ant-design/icons";
import type {ShipperOrder, ShipperStats} from "../../../api/orderApi";
import orderApi from "../../../api/orderApi";
import dayjs from "dayjs";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";
import DeliveryHistoryToolbar from "./components/DeliveryHistoryToolbar";
import DeliveryHistoryTable from "./components/DeliveryHistoryTable";

const { Text } = Typography;

interface FilterParams {
  status?: string;
  dateRange?: [dayjs.Dayjs, dayjs.Dayjs] | null;
  search?: string;
}

const getStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
    case "PENDING": return "default";
    case "CONFIRMED":
    case "AT_DEST_OFFICE": return "blue";
    case "PICKED_UP": return "orange";
    case "DELIVERING": return "processing";
    case "DELIVERED": return "success";
    case "DELIVERY_RETRY":
    case "DELIVERY_FAILED_FINAL":
    case "FAILED_DELIVERY":
    case "CANCELLED": return "error";
    case "RETURNING": return "warning";
    case "RETURN_READY_FOR_PICKUP": return "gold";
    case "RETURN_PICKED_UP": return "cyan";
    case "RETURNED": return "gold";
    default: return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status.toUpperCase()) {
    case "DELIVERED": return "Đã giao";
    case "DELIVERY_RETRY": return "Chờ giao lại";
    case "AT_DEST_OFFICE": return "Tại bưu cục đích";
    case "DELIVERY_FAILED_FINAL": return "Giao thất bại";
    case "FAILED_DELIVERY": return "Giao thất bại";
    case "RETURNING": return "Đang hoàn trả";
    case "RETURN_AT_ORIGIN_OFFICE": return "Đã hoàn về bưu cục gốc";
    case "RETURN_READY_FOR_PICKUP": return "Sẵn sàng lấy hàng hoàn";
    case "RETURN_PICKED_UP": return "Đã lấy hàng hoàn lên xe";
    case "RETURN_RETRY": return "Hoàn lại";
    case "RETURN_FAILED_FINAL": return "Hoàn thất bại";
    case "RETURNED": return "Đã hoàn";
    case "PENDING": return "Chờ xử lý";
    case "CONFIRMED": return "Đã xác nhận";
    case "PICKED_UP": return "Đã lấy hàng";
    case "DELIVERING": return "Đang giao";
    case "CANCELLED": return "Đã hủy";
    default: return status;
  }
};

const DeliveryHistoryPage: React.FC = () => {
  const [history, setHistory] = useState<ShipperOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<FilterParams>({});
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [stats, setStats] = useState<ShipperStats>({
    totalAssigned: 0,
    inProgress: 0,
    delivered: 0,
    failed: 0,
    codCollected: 0,
  });
  const [selectedRecord, setSelectedRecord] = useState<ShipperOrder | null>(null);
  const [detailModal, setDetailModal] = useState(false);

  useEffect(() => {
    fetchHistory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagination.current, pagination.pageSize, filters]);

  const fetchHistory = async () => {
    try {
      setLoading(true);
      const params: any = {
        page: pagination.current,
        limit: pagination.pageSize,
      };
      if (filters.status) params.status = filters.status;
      if (filters.search) params.search = filters.search;

      const response = await orderApi.getShipperDeliveryHistory(params);
      setHistory(response.orders || []);
      setStats(response.stats || stats);
      setPagination((prev) => ({ ...prev, total: response.pagination?.total || 0 }));
    } catch (error) {
      console.error("Error fetching delivery history:", error);
      message.error("Lỗi khi tải lịch sử giao hàng");
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (key: keyof FilterParams, value: any) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
    if (key !== "dateRange") {
      setPagination((prev) => ({ ...prev, current: 1 }));
    }
  };

  const resetFilters = () => {
    setFilters({});
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handleViewDetail = (record: ShipperOrder) => {
    setSelectedRecord(record);
    setDetailModal(true);
  };

  const handlePageChange = (page: number, pageSize: number) => {
    setPagination((prev) => ({ ...prev, current: page, pageSize: pageSize || 10 }));
  };

  const closeDetailModal = () => {
    setDetailModal(false);
    setSelectedRecord(null);
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <DeliveryHistoryToolbar
          filters={filters}
          onFilterChange={handleFilterChange}
          onReset={resetFilters}
        />

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Lịch sử giao hàng</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {history.length} đơn</div>
            </div>
          </div>
        </div>

        <div className="shipper-stats-section">
          <Row gutter={16}>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Tổng đơn đã giao" value={stats.delivered} prefix={<CheckCircleOutlined />} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Giao thất bại" value={stats.failed} prefix={<ClockCircleOutlined />} />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="COD đã thu"
                  value={stats.codCollected}
                  prefix={<DollarOutlined />}
                  formatter={(value) => `${value?.toLocaleString()}đ`}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Tổng đơn" value={stats.totalAssigned} />
              </Card>
            </Col>
          </Row>
        </div>

        <div className="list-page-table shipper-page-table">
          <DeliveryHistoryTable
            orders={history}
            loading={loading}
            pagination={pagination}
            onPageChange={handlePageChange}
            onViewDetail={handleViewDetail}
          />
        </div>
      </div>

      <Modal
        title="Chi tiết đơn hàng"
        open={detailModal}
        onCancel={closeDetailModal}
        footer={null}
        width={700}
      >
        {selectedRecord && (
          <Space direction="vertical" size="large" style={{ width: "100%" }}>
            <div>
              <Text strong>Mã đơn hàng: </Text>
              <Text>{selectedRecord.trackingNumber}</Text>
            </div>
            <div>
              <Text strong>Người nhận: </Text>
              <Text>{selectedRecord.recipientName}</Text>
            </div>
            <div>
              <Text strong>SĐT: </Text>
              <Text>{selectedRecord.recipientPhone}</Text>
            </div>
            <div>
              <Text strong>Địa chỉ: </Text>
              <Text>
                {typeof selectedRecord.recipientAddress === "string"
                  ? selectedRecord.recipientAddress
                  : (selectedRecord.recipientAddress as any)?.fullAddress ?? ""}
              </Text>
            </div>
            <div>
              <Text strong>COD: </Text>
              <Text className="shipper-cod-value">{selectedRecord.cod?.toLocaleString()}đ</Text>
            </div>
            <div>
              <Text strong>Trạng thái: </Text>
              <Tag color={getStatusColor(selectedRecord.status)}>{getStatusText(selectedRecord.status)}</Tag>
            </div>
            {selectedRecord.deliveredAt && (
              <div>
                <Text strong>Ngày giao: </Text>
                <Text>{dayjs(selectedRecord.deliveredAt).format("DD/MM/YYYY HH:mm")}</Text>
              </div>
            )}
            {selectedRecord.notes && (
              <div>
                <Text strong>Ghi chú: </Text>
                <Text>{selectedRecord.notes}</Text>
              </div>
            )}
            <div>
              <Text strong>Giao dịch COD:</Text>
              {selectedRecord.paymentSubmissions && selectedRecord.paymentSubmissions.length > 0 ? (
                <Table
                  dataSource={selectedRecord.paymentSubmissions}
                  rowKey="id"
                  pagination={false}
                  columns={[
                    { title: "Mã", dataIndex: "code", key: "code" },
                    {
                      title: "Số hệ thống",
                      dataIndex: "systemAmount",
                      key: "systemAmount",
                      render: (v: number) => v?.toLocaleString() + "đ",
                    },
                    {
                      title: "Số thực thu",
                      dataIndex: "actualAmount",
                      key: "actualAmount",
                      render: (v: number) => v?.toLocaleString() + "đ",
                    },
                    { title: "Trạng thái", dataIndex: "status", key: "status", render: (s: string) => <Tag>{s}</Tag> },
                    {
                      title: "Ngày",
                      dataIndex: "paidAt",
                      key: "paidAt",
                      render: (d: string) => (d ? dayjs(d).format("DD/MM/YYYY HH:mm") : "—"),
                    },
                  ]}
                />
              ) : (
                <div>
                  <Text type="secondary">Chưa có giao dịch COD.</Text>
                </div>
              )}
            </div>
          </Space>
        )}
      </Modal>
    </div>
  );
};

export default DeliveryHistoryPage;
