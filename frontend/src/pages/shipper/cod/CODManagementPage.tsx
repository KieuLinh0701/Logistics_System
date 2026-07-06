import React, { useEffect, useState, useMemo } from "react";
import {
  Alert,
  Button,
  DatePicker,
  Empty,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tabs,
  Tooltip,
  Typography,
  message,
} from "antd";
import {
  DollarOutlined,
  EyeOutlined,
  HistoryOutlined,
  ReloadOutlined,
  SwapOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import orderApi from "../../../api/orderApi";
import CODTransactionsStats from "./components/CODTransactionsStats";
import "../../../styles/ListPage.css";

const { RangePicker } = DatePicker;
const { Text } = Typography;

interface PaymentSubmissionItem {
  id: number;
  code?: string;
  orderId?: number;
  trackingNumber?: string;
  systemAmount: number;
  actualAmount: number;
  discrepancy?: number;
  status: string;
  notes?: string;
  paidAt?: string;
  checkedAt?: string;
}

interface BatchInfo {
  id: number;
  code?: string;
  status: string;
  totalSystemAmount: number;
  totalActualAmount: number;
  createdAt?: string;
  submissionCount: number;
}

interface OrderGroup {
  trackingNumber: string;
  orderId?: number;
  submissions: PaymentSubmissionItem[];
  totalSystem: number;
  totalActual: number;
  discrepancy: number;
  hasCod: boolean;
  hasShippingFee: boolean;
  codAmount: number;
  shippingFeeAmount: number;
}

interface BatchListItem {
  id: number;
  code?: string;
  status: string;
  totalSystemAmount: number;
  totalActualAmount: number;
  createdAt?: string;
  submissionCount: number;
}

const CODManagementPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<"current" | "history">("current");
  const [loading, setLoading] = useState(false);

  // --- Tab "Giao dich thu tien" ---
  const [openBatch, setOpenBatch] = useState<BatchInfo | null>(null);
  const [openBatchSubmissions, setOpenBatchSubmissions] = useState<PaymentSubmissionItem[]>([]);
  const [currentStatus, setCurrentStatus] = useState<string | undefined>(undefined);
  const [currentDateRange, setCurrentDateRange] = useState<[string, string] | null>(null);

  // --- Tab "Lich su" ---
  const [historyBatches, setHistoryBatches] = useState<BatchListItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyPagination, setHistoryPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [historyDateRange, setHistoryDateRange] = useState<[string, string] | null>(null);
  const [historyStatus, setHistoryStatus] = useState<string | undefined>(undefined);

  // --- Modal nop tien ---
  const [submitModal, setSubmitModal] = useState(false);
  const [submitForm] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);

  // --- Chi tiet batch ---
  const [detailModal, setDetailModal] = useState(false);
  const [detailBatch, setDetailBatch] = useState<BatchInfo | null>(null);
  const [detailSubmissions, setDetailSubmissions] = useState<PaymentSubmissionItem[]>([]);

  useEffect(() => {
    fetchOpenBatch();
  }, []);

  // === GROUP SUBMISSIONS BY ORDER ===
  const orderGroups = useMemo<OrderGroup[]>(() => {
    if (!openBatchSubmissions.length) return [];

    const map = new Map<string, OrderGroup>();

    for (const sub of openBatchSubmissions) {
      const key = sub.trackingNumber || `order_${sub.orderId}`;
      if (!map.has(key)) {
        map.set(key, {
          trackingNumber: sub.trackingNumber || key,
          orderId: sub.orderId,
          submissions: [],
          totalSystem: 0,
          totalActual: 0,
          discrepancy: 0,
          hasCod: false,
          hasShippingFee: false,
          codAmount: 0,
          shippingFeeAmount: 0,
        });
      }
      const group = map.get(key)!;
      group.submissions.push(sub);
      group.totalSystem += sub.systemAmount || 0;
      group.totalActual += sub.actualAmount || 0;

      const notes = (sub.notes || "").toLowerCase();
      if (notes.includes("cod")) {
        group.hasCod = true;
        group.codAmount += sub.actualAmount || 0;
      } else if (notes.includes("phí vận chuyển") || notes.includes("shipping")) {
        group.hasShippingFee = true;
        group.shippingFeeAmount += sub.actualAmount || 0;
      } else {
        if (sub.systemAmount > group.codAmount + group.shippingFeeAmount) {
          group.hasCod = true;
          group.codAmount += sub.actualAmount || 0;
        } else {
          group.hasShippingFee = true;
          group.shippingFeeAmount += sub.actualAmount || 0;
        }
      }
    }

    for (const group of map.values()) {
      group.discrepancy = group.totalActual - group.totalSystem;
    }

    return Array.from(map.values());
  }, [openBatchSubmissions]);

  const uniqueOrderCount = orderGroups.length;

  // === TAB "GIAO DICH THU TIEN" ===
  const fetchOpenBatch = async () => {
    try {
      setLoading(true);
      const response = await orderApi.getShipperCODTransactions({ page: 1, limit: 100 });
      const summary = response.summary || {};
      const batch: BatchInfo | null = summary.openBatch || null;
      setOpenBatch(batch);
      setOpenBatchSubmissions(response.transactions || []);
    } catch (error) {
      console.error("Error fetching COD transactions:", error);
      message.error("Lỗi khi tải thông tin đối soát");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitCOD = async (values: any) => {
    if (!openBatch) return;
    try {
      setSubmitLoading(true);
      await orderApi.submitShipperCOD({
        batchId: openBatch.id,
        notes: values.notes,
      });
      message.success("Đã nộp tiền thu được thành công. Đang chờ quản lý đối soát.");
      setSubmitModal(false);
      submitForm.resetFields();
      fetchOpenBatch();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi nộp tiền");
    } finally {
      setSubmitLoading(false);
    }
  };

  // === TAB "LICH SU" ===
  const fetchHistoryBatches = async () => {
    try {
      setHistoryLoading(true);
      const params: any = {
        page: historyPagination.current,
        limit: historyPagination.pageSize,
      };
      if (historyStatus) params.status = historyStatus;
      if (historyDateRange) {
        params.dateFrom = historyDateRange[0];
        params.dateTo = historyDateRange[1];
      }

      const response = await orderApi.getShipperCODBatchHistory(params);
      setHistoryBatches(response.batches || []);
      setHistoryPagination((prev) => ({ ...prev, total: response.pagination?.total || 0 }));
    } catch (error) {
      console.error("Error fetching batch history:", error);
      message.error("Lỗi khi tải lịch sử nộp tiền");
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleViewBatchDetail = async (batch: BatchListItem) => {
    setDetailBatch(batch);
    setDetailSubmissions([]);
    try {
      const response = await orderApi.getShipperCODBatchDetail(batch.id);
      setDetailSubmissions(response.submissions || []);
    } catch {
      setDetailSubmissions([]);
    }
    setDetailModal(true);
  };

  const getBatchStatusColor = (status: string) => {
    switch (status) {
      case "OPEN": return "cyan";
      case "PROCESSING": return "orange";
      case "COMPLETED": return "success";
      default: return "default";
    }
  };

  const getBatchStatusText = (status: string) => {
    switch (status) {
      case "OPEN": return "Đang gom đơn";
      case "PROCESSING": return "Đã nộp - Chờ đối soát";
      case "COMPLETED": return "Hoàn tất";
      default: return status;
    }
  };

  const hasBatch = openBatch !== null && orderGroups.length > 0;

  // Summary for stats component
  const currentSummary = {
    totalCollected: openBatch?.totalActualAmount || 0,
    totalSubmitted: 0,
    totalPending: openBatch?.totalActualAmount || 0,
    transactionCount: uniqueOrderCount,
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Quản lý tiền thu được</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">
                {activeTab === "current"
                  ? `Giao dịch: ${uniqueOrderCount} dòng`
                  : `Lịch sử nộp: ${historyBatches.length} dòng`}
              </div>
            </div>
          </div>
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={(key) => {
            setActiveTab(key as "current" | "history");
            if (key === "history") fetchHistoryBatches();
          }}
          className="shipper-cod-tabs"
          items={[
            {
              key: "current",
              label: (
                <span className="tab-label">
                  <DollarOutlined />
                  Giao dịch thu tiền
                </span>
              ),
              children: (
                <div className="shipper-cod-tab-block">
                  {/* Filter row */}
                  <div className="shipper-filter-panel">
                    <div className="shipper-filter-grow" />
                    <div className="shipper-filter-actions">
                      <Select
                        placeholder="Lọc theo trạng thái"
                        allowClear
                        style={{ width: 160 }}
                        value={currentStatus}
                        onChange={(v) => setCurrentStatus(v || undefined)}
                      >
                        <Select.Option value="PENDING">Chờ nộp</Select.Option>
                        <Select.Option value="SUBMITTED">Đã nộp</Select.Option>
                      </Select>
                      <RangePicker
                        onChange={(dates) => {
                          if (dates && dates[0] && dates[1]) {
                            setCurrentDateRange([
                              dates[0].format("YYYY-MM-DD"),
                              dates[1].format("YYYY-MM-DD"),
                            ]);
                          } else {
                            setCurrentDateRange(null);
                          }
                        }}
                      />
                      <Button icon={<ReloadOutlined />} onClick={fetchOpenBatch}>
                        Tải lại
                      </Button>
                      {hasBatch && (
                        <Button
                          type="primary"
                          icon={<SwapOutlined />}
                          onClick={() => setSubmitModal(true)}
                        >
                          Nộp tiền ({uniqueOrderCount})
                        </Button>
                      )}
                    </div>
                  </div>

                  {/* Stats cards */}
                  <div className="shipper-stats-section">
                    <CODTransactionsStats summary={currentSummary} />
                  </div>

                  {/* Table */}
                  <CurrentBatchTable
                    orderGroups={orderGroups}
                    loading={loading}
                  />
                </div>
              ),
            },
            {
              key: "history",
              label: (
                <span className="tab-label">
                  <HistoryOutlined />
                  Lịch sử nộp tiền
                </span>
              ),
              children: (
                <div className="shipper-cod-tab-block">
                  {/* Filter toolbar */}
                  <div className="shipper-filter-panel">
                    <div className="shipper-filter-grow" />
                    <div className="shipper-filter-actions">
                      <Select
                        placeholder="Lọc theo trạng thái"
                        allowClear
                        style={{ width: 180 }}
                        value={historyStatus}
                        onChange={(v) => {
                          setHistoryStatus(v || undefined);
                          setHistoryPagination((p) => ({ ...p, current: 1 }));
                        }}
                      >
                        <Select.Option value="PROCESSING">Đã nộp - Chờ đối soát</Select.Option>
                        <Select.Option value="COMPLETED">Hoàn tất</Select.Option>
                      </Select>
                      <RangePicker
                        onChange={(dates) => {
                          if (dates && dates[0] && dates[1]) {
                            setHistoryDateRange([
                              dates[0].format("YYYY-MM-DD"),
                              dates[1].format("YYYY-MM-DD"),
                            ]);
                          } else {
                            setHistoryDateRange(null);
                          }
                        }}
                      />
                      <Button icon={<ReloadOutlined />} onClick={fetchHistoryBatches}>
                        Tải lại
                      </Button>
                    </div>
                  </div>

                  {/* Table */}
                  <HistoryBatchTable
                    batches={historyBatches}
                    loading={historyLoading}
                    pagination={historyPagination}
                    onPageChange={(page, pageSize) =>
                      setHistoryPagination({ current: page, pageSize, total: historyPagination.total })
                    }
                    onViewDetail={handleViewBatchDetail}
                  />
                </div>
              ),
            },
          ]}
        />
      </div>

      {/* Modal nop tien */}
      <Modal
        title={
          <Space>
            <SwapOutlined />
            <Text strong style={{ fontSize: 16 }}>Xác nhận nộp tiền thu được</Text>
          </Space>
        }
        open={submitModal}
        onOk={() => submitForm.submit()}
        confirmLoading={submitLoading}
        onCancel={() => { setSubmitModal(false); submitForm.resetFields(); }}
        width={500}
        className="shipper-modal"
      >
        {openBatch && (
          <Alert
            message="Xác nhận nộp tiền"
            description={
              <div>
                <p>
                  Bạn sắp nộp <strong>{(openBatch.totalActualAmount || 0).toLocaleString()}đ</strong>{" "}
                  cho <strong>{uniqueOrderCount} đơn hàng</strong>.
                </p>
                <p style={{ margin: 0 }}>
                  Số tiền này sẽ được chuyển đến quản lý để đối soát.
                </p>
              </div>
            }
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}
        <Form form={submitForm} layout="vertical" onFinish={handleSubmitCOD}>
          <Form.Item name="notes" label="Ghi chú (không bắt buộc)">
            <Input.TextArea rows={3} placeholder="Ghi chú về việc nộp tiền..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal chi tiet batch */}
      <Modal
        title={`Chi tiết phiên đối soát: ${detailBatch?.code || ""}`}
        open={detailModal}
        onCancel={() => setDetailModal(false)}
        footer={null}
        width={700}
        className="shipper-modal"
      >
        {detailBatch && (
          <div style={{ marginBottom: 16 }}>
            <Space wrap style={{ marginBottom: 12 }}>
              <Text><Text strong>Trạng thái:</Text>{" "}
                <Tag color={getBatchStatusColor(detailBatch.status)}>
                  {getBatchStatusText(detailBatch.status)}
                </Tag>
              </Text>
              <Text><Text strong>Ngày nộp:</Text>{" "}
                {detailBatch.createdAt ? dayjs(detailBatch.createdAt).format("DD/MM/YYYY HH:mm") : "—"}
              </Text>
            </Space>
            <div style={{ display: "flex", gap: 16 }}>
              <Text><Text strong>Tiền hệ thống:</Text> {(detailBatch.totalSystemAmount || 0).toLocaleString()}đ</Text>
              <Text><Text strong>Tiền nộp:</Text> {(detailBatch.totalActualAmount || 0).toLocaleString()}đ</Text>
            </div>
          </div>
        )}
        <Table
          dataSource={detailSubmissions.map((s) => ({ ...s, key: s.id }))}
          columns={[
            { title: "Mã vận đơn", dataIndex: "trackingNumber", key: "trackingNumber", width: 160 },
            {
              title: "Tiền hệ thống",
              dataIndex: "systemAmount",
              key: "systemAmount",
              width: 150,
              align: "right",
              render: (v: number) => `${(v || 0).toLocaleString()}đ`,
            },
            {
              title: "Tiền thực thu",
              dataIndex: "actualAmount",
              key: "actualAmount",
              width: 150,
              align: "right",
              render: (v: number) => `${(v || 0).toLocaleString()}đ`,
            },
            {
              title: "Chênh lệch",
              dataIndex: "discrepancy",
              key: "discrepancy",
              width: 120,
              align: "right",
              render: (v: number) => {
                if (!v) return "—";
                return <Text type={v > 0 ? "success" : "danger"}>{v > 0 ? "+" : ""}{v.toLocaleString()}đ</Text>;
              },
            },
            { title: "Ghi chú", dataIndex: "notes", key: "notes", ellipsis: true },
          ]}
          pagination={false}
          size="small"
          scroll={{ x: 700 }}
          locale={{
            emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không có chi tiết giao dịch trong phiên này." />,
          }}
        />
      </Modal>
    </div>
  );
};

// === Bang don hien tai ===
interface CurrentBatchTableProps {
  orderGroups: OrderGroup[];
  loading: boolean;
}

const CurrentBatchTable: React.FC<CurrentBatchTableProps> = ({
  orderGroups,
  loading,
}) => {
  const columns: ColumnsType<OrderGroup> = [
    {
      title: "Mã vận đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 160,
      render: (text: string) => (
        <Text className="shipper-table-strong">{text}</Text>
      ),
    },
    {
      title: "COD",
      key: "cod",
      width: 130,
      align: "right",
      render: (_, record) => (
        record.hasCod ? (
          <Text className="shipper-cod-value">{record.codAmount.toLocaleString()}đ</Text>
        ) : (
          <Text type="secondary">—</Text>
        )
      ),
    },
    {
      title: "Phí vận chuyển",
      key: "shippingFee",
      width: 140,
      align: "right",
      render: (_, record) => (
        record.hasShippingFee ? (
          <Text>{record.shippingFeeAmount.toLocaleString()}đ</Text>
        ) : (
          <Text type="secondary">—</Text>
        )
      ),
    },
    {
      title: "Tổng thực thu",
      key: "totalActual",
      width: 150,
      align: "right",
      render: (_, record) => (
        <Text strong>{(record.totalActual || 0).toLocaleString()}đ</Text>
      ),
    },
    {
      title: "Chênh lệch",
      key: "discrepancy",
      width: 120,
      align: "right",
      render: (_, record) => {
        if (!record.discrepancy) return <Text type="secondary">—</Text>;
        return (
          <Text type={record.discrepancy > 0 ? "success" : "danger"}>
            {record.discrepancy > 0 ? "+" : ""}{record.discrepancy.toLocaleString()}đ
          </Text>
        );
      },
    },
    {
      title: "Trạng thái",
      key: "status",
      width: 120,
      align: "center",
      render: () => <Tag color="cyan">Chờ nộp</Tag>,
    },
  ];

  const expandedRowRender = (record: OrderGroup) => {
    const subColumns: ColumnsType<PaymentSubmissionItem> = [
      {
        title: "Loại",
        key: "type",
        width: 160,
        render: (_, sub) => {
          const notes = (sub.notes || "").toLowerCase();
          if (notes.includes("cod")) return <Text style={{ color: "#1E4DB7" }}>COD hàng hóa</Text>;
          if (notes.includes("phí vận chuyển") || notes.includes("shipping")) return <Text style={{ color: "#1E4DB7" }}>Phí vận chuyển</Text>;
          return <Text type="secondary">Khác</Text>;
        },
      },
      {
        title: "Mã submission",
        dataIndex: "code",
        key: "code",
        width: 180,
        render: (code: string) => (
          <Tooltip title={code}>
            <Text type="secondary" style={{ fontSize: 12 }}>{code || "—"}</Text>
          </Tooltip>
        ),
      },
      {
        title: "Tiền hệ thống",
        dataIndex: "systemAmount",
        key: "systemAmount",
        width: 130,
        align: "right",
        render: (v: number) => `${(v || 0).toLocaleString()}đ`,
      },
      {
        title: "Tiền thực thu",
        dataIndex: "actualAmount",
        key: "actualAmount",
        width: 130,
        align: "right",
        render: (v: number) => `${(v || 0).toLocaleString()}đ`,
      },
      {
        title: "Chênh lệch",
        dataIndex: "discrepancy",
        key: "discrepancy",
        width: 110,
        align: "right",
        render: (v: number) => {
          if (!v) return "—";
          return <Text type={v > 0 ? "success" : "danger"}>{v > 0 ? "+" : ""}{v.toLocaleString()}đ</Text>;
        },
      },
      {
        title: "Ghi chú",
        dataIndex: "notes",
        key: "notes",
        ellipsis: true,
        render: (notes: string) => (
          <Tooltip title={notes || ""}>
            <Text type="secondary" style={{ maxWidth: 200 }} ellipsis>{notes || "—"}</Text>
          </Tooltip>
        ),
      },
    ];

    return (
      <Table
        dataSource={record.submissions.map((s) => ({ ...s, key: s.id }))}
        columns={subColumns}
        pagination={false}
        size="small"
        rowKey="id"
        style={{ marginLeft: 32, marginRight: 32 }}
      />
    );
  };

  return (
    <div className="list-page-table shipper-page-table">
      <Table
        dataSource={orderGroups.map((g) => ({ ...g, key: g.trackingNumber }))}
        columns={columns}
        loading={loading}
        pagination={false}
        expandable={{
          expandedRowRender,
          rowExpandable: () => true,
          expandRowByClick: true,
        }}
        size="middle"
        scroll={{ x: 920 }}
        locale={{
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có giao dịch thu tiền nào." />,
        }}
      />
    </div>
  );
};

// === Bang lich su batch ===
interface HistoryBatchTableProps {
  batches: BatchListItem[];
  loading: boolean;
  pagination: { current: number; pageSize: number; total: number };
  onPageChange: (page: number, pageSize: number) => void;
  onViewDetail: (batch: BatchListItem) => void;
}

const HistoryBatchTable: React.FC<HistoryBatchTableProps> = ({
  batches,
  loading,
  pagination,
  onPageChange,
  onViewDetail,
}) => {
  const columns: ColumnsType<BatchListItem> = [
    {
      title: "Mã phiên",
      key: "code",
      width: 140,
      render: (_, record) => (
        <Text className="shipper-table-strong">{record.code || `Phiên #${record.id}`}</Text>
      ),
    },
    {
      title: "Trạng thái",
      key: "status",
      width: 150,
      render: (_, record) => (
        <Tag color={getStatusColor(record.status)}>
          {getStatusText(record.status)}
        </Tag>
      ),
    },
    {
      title: "Ngày nộp",
      key: "createdAt",
      width: 150,
      render: (_, record) => (
        <Text type="secondary">
          {record.createdAt ? dayjs(record.createdAt).format("DD/MM/YYYY HH:mm") : "—"}
        </Text>
      ),
    },
    {
      title: "Số đơn",
      key: "submissionCount",
      width: 80,
      align: "center",
      render: (_, record) => <Text>{record.submissionCount}</Text>,
    },
    {
      title: "Tiền hệ thống",
      key: "totalSystemAmount",
      width: 130,
      align: "right",
      render: (_, record) => (
        <Text>{(record.totalSystemAmount || 0).toLocaleString()}đ</Text>
      ),
    },
    {
      title: "Tiền nộp",
      key: "totalActualAmount",
      width: 130,
      align: "right",
      render: (_, record) => (
        <Text strong>{(record.totalActualAmount || 0).toLocaleString()}đ</Text>
      ),
    },
    {
      title: "Thao tác",
      key: "actions",
      width: 140,
      align: "center",
      render: (_, record) => (
        <Button size="middle" icon={<EyeOutlined />} onClick={() => onViewDetail(record)}>
          Chi tiết
        </Button>
      ),
    },
  ];

  return (
    <div className="list-page-table shipper-page-table">
      <Table
        dataSource={batches.map((b) => ({ ...b, key: b.id }))}
        columns={columns}
        loading={loading}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: onPageChange,
          showSizeChanger: true,
          showTotal: (total, range) => `${range[0]}-${range[1]} / ${total} phiên`,
        }}
        size="middle"
        scroll={{ x: 920 }}
        locale={{
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có lịch sử nộp tiền." />,
        }}
      />
    </div>
  );
};

const getStatusColor = (status: string) => {
  switch (status) {
    case "OPEN": return "cyan";
    case "PROCESSING": return "orange";
    case "COMPLETED": return "success";
    default: return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status) {
    case "OPEN": return "Đang gom đơn";
    case "PROCESSING": return "Đã nộp - Chờ đối soát";
    case "COMPLETED": return "Hoàn tất";
    default: return status;
  }
};

export default CODManagementPage;
