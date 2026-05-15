import React, { useState, useEffect } from "react";
import {
  Table,
  Tag,
  Button,
  Space,
  Typography,
  Row,
  Col,
  Card,
  Select,
  DatePicker,
  Input,
  Statistic,
  message,
  Modal,
} from "antd";
import {
  EyeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  PhoneOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import orderApi from "../../api/orderApi";
import type { ShipperOrder, ShipperStats } from "../../api/orderApi";
import dayjs from "dayjs";
import { translateOrderCodStatus } from "../../utils/orderUtils";
import "../../styles/ListPage.css";
import "./ShipperPagesShared.css";

const { Text } = Typography;
const { Option } = Select;
const { RangePicker } = DatePicker;

interface FilterParams {
  status?: string;
  dateRange?: [dayjs.Dayjs, dayjs.Dayjs] | null;
  search?: string;
}

const ShipperDeliveryHistory: React.FC = () => {
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
  };

  const resetFilters = () => {
    setFilters({});
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handleViewDetail = (record: ShipperOrder) => {
    setSelectedRecord(record);
    setDetailModal(true);
  };

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case "PENDING":
        return "default";
      case "CONFIRMED":
      case "AT_DEST_OFFICE":
        return "blue";
      case "PICKED_UP":
        return "orange";
      case "DELIVERING":
        return "processing";
      case "DELIVERED":
        return "success";
      case "FAILED_DELIVERY":
      case "CANCELLED":
        return "error";
      case "RETURNED":
        return "warning";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status.toUpperCase()) {
      case "PENDING":
        return "Chờ xử lý";
      case "CONFIRMED":
        return "Đã xác nhận";
      case "AT_DEST_OFFICE":
        return "Đã đến bưu cục";
      case "PICKED_UP":
        return "Đã lấy hàng";
      case "DELIVERING":
        return "Đang giao hàng";
      case "DELIVERED":
        return "Đã giao";
      case "PARTIAL_DELIVERY":
        return "Giao 1 phần";
      case "PARTIAL_RETURN":
        return "Trả 1 phần";
      case "FAILED_DELIVERY":
        return "Giao thất bại";
      case "CANCELLED":
        return "Đã hủy";
      case "RETURNED":
        return "Đã hoàn";
      default:
        return status;
    }
  };

  const columns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => (
        <Text strong className="shipper-table-strong" style={{ fontSize: "13px" }}>
          {text}
        </Text>
      ),
    },
    {
      title: "Người nhận",
      key: "recipient",
      width: 200,
      render: (record: ShipperOrder) => (
        <Space direction="vertical" size={0}>
          <Text strong className="shipper-table-strong">
            {record.recipientName}
          </Text>
          <Space size={4}>
            <PhoneOutlined className="shipper-table-muted" style={{ fontSize: "12px" }} />
            <Text className="shipper-table-muted" style={{ fontSize: "12px" }}>
              {record.recipientPhone}
            </Text>
          </Space>
          <Text className="shipper-table-muted" style={{ fontSize: "11px" }} ellipsis>
            {record.recipientFullAddress ||
              (typeof record.recipientAddress === "string"
                ? record.recipientAddress
                : (record.recipientAddress as any)?.fullAddress) ||
              ""}
          </Text>
        </Space>
      ),
    },
    {
      title: "COD",
      dataIndex: "cod",
      key: "cod",
      width: 120,
      render: (amount: number, record: ShipperOrder & any) => (
        <div>
          {amount > 0 ? (
            <Text strong className="shipper-cod-value">
              {amount.toLocaleString()}đ
            </Text>
          ) : (
            <Text className="shipper-table-muted">—</Text>
          )}
          {record.codStatus && (
            <div style={{ marginTop: 6 }}>
              <Tag
                color={
                  record.codStatus === "PENDING"
                    ? "orange"
                    : record.codStatus === "SUBMITTED"
                      ? "blue"
                      : record.codStatus === "RECEIVED" || record.codStatus === "TRANSFERRED"
                        ? "green"
                        : "default"
                }
              >
                {translateOrderCodStatus(record.codStatus)}
              </Tag>
            </div>
          )}
        </div>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: string) => <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>,
    },
    {
      title: "Ngày giao",
      dataIndex: "deliveredAt",
      key: "deliveredAt",
      width: 150,
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
    {
      title: "Thao tác",
      key: "action",
      width: 100,
      render: (record: ShipperOrder) => (
        <Button icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
          Chi tiết
        </Button>
      ),
    },
  ];

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="shipper-filter-panel">
          <div className="shipper-filter-grow">
            <Input
              allowClear
              className="search-input"
              placeholder="Tìm kiếm theo mã đơn, tên người nhận..."
              prefix={<SearchOutlined />}
              style={{ width: "100%" }}
              value={filters.search}
              onChange={(e) => handleFilterChange("search", e.target.value || undefined)}
            />
          </div>
          <div className="shipper-filter-actions">
            <Select
              allowClear
              placeholder="Lọc theo trạng thái"
              style={{ width: 200 }}
              value={filters.status}
              onChange={(value) => handleFilterChange("status", value || undefined)}
            >
              <Option value="DELIVERED">Đã giao</Option>
              <Option value="PARTIAL_DELIVERY">Giao 1 phần</Option>
              <Option value="PARTIAL_RETURN">Trả 1 phần</Option>
              <Option value="FAILED_DELIVERY">Giao thất bại</Option>
              <Option value="RETURNED">Đã hoàn</Option>
            </Select>
            <RangePicker
              value={filters.dateRange ?? null}
              onChange={(dates) => {
                handleFilterChange("dateRange", dates);
              }}
            />
            <Button icon={<ReloadOutlined />} onClick={resetFilters}>
              Làm mới
            </Button>
          </div>
        </div>

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
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={history}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              onChange: (page, pageSize) =>
                setPagination({ ...pagination, current: page, pageSize: pageSize || 10 }),
            }}
            scroll={{ x: 960 }}
          />
        </div>
      </div>

      <Modal
        title="Chi tiết đơn hàng"
        open={detailModal}
        onCancel={() => {
          setDetailModal(false);
          setSelectedRecord(null);
        }}
        footer={null}
        width={700}
      >
        {selectedRecord && (
          <div>
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
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ShipperDeliveryHistory;
