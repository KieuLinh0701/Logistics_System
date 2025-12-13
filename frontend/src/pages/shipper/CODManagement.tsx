import React, { useState, useEffect } from "react";
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Typography,
  Row,
  Col,
  Select,
  DatePicker,
  Input,
  Statistic,
  message,
  Modal,
  Form,
  InputNumber,
  Checkbox,
  Tabs,
  Descriptions,
} from "antd";
import {
  DollarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SwapOutlined,
  HistoryOutlined,
  EyeOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import orderApi from "../../api/orderApi";
import dayjs from "dayjs";

const { Title, Text } = Typography;
const { Option } = Select;
const { RangePicker } = DatePicker;

interface CODTransaction {
  id: number;
  trackingNumber: string;
  recipientName: string;
  recipientPhone: string;
  codAmount: number;
  status: string;
  collectedAt?: string;
  notes?: string;
}

interface CODSubmission {
  id: number;
  orderId: number;
  trackingNumber: string;
  systemAmount: number;
  actualAmount: number;
  discrepancy: number;
  status: string;
  notes?: string;
  paidAt: string;
  checkedAt?: string;
}

const ShipperCODManagement: React.FC = () => {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<CODTransaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<{ status?: string; dateFrom?: string; dateTo?: string }>({});
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [summary, setSummary] = useState({
    totalCollected: 0,
    totalSubmitted: 0,
    totalPending: 0,
    transactionCount: 0,
  });
  const [selectedTransactions, setSelectedTransactions] = useState<number[]>([]);
  const [submitModal, setSubmitModal] = useState(false);
  const [submitForm] = Form.useForm();
  const [activeTab, setActiveTab] = useState("transactions");
  const [submissions, setSubmissions] = useState<CODSubmission[]>([]);
  const [submissionSummary, setSubmissionSummary] = useState({
    totalSubmitted: 0,
    totalDiscrepancy: 0,
    totalSubmissions: 0,
  });
  const [submissionPagination, setSubmissionPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [submissionFilters, setSubmissionFilters] = useState({
    status: "",
    dateFrom: "",
    dateTo: "",
  });
  const [detailModal, setDetailModal] = useState(false);
  const [selectedSubmission, setSelectedSubmission] = useState<CODSubmission | null>(null);

  useEffect(() => {
    if (activeTab === "transactions") {
      fetchTransactions();
    } else {
      fetchSubmissions();
    }
  }, [activeTab, pagination.current, pagination.pageSize, filters, submissionPagination.current, submissionPagination.pageSize, submissionFilters]);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const params: any = {
        page: pagination.current,
        limit: pagination.pageSize,
      };
      if (filters.status) params.status = filters.status;
      if (filters.dateFrom) params.dateFrom = filters.dateFrom;
      if (filters.dateTo) params.dateTo = filters.dateTo;

      const response = await orderApi.getShipperCODTransactions(params);
      setTransactions(response.transactions || []);
      setSummary(response.summary || summary);
      setPagination((prev) => ({ ...prev, total: response.pagination?.total || 0 }));
    } catch (error) {
      console.error("Error fetching COD transactions:", error);
      message.error("Lỗi khi tải danh sách giao dịch COD");
    } finally {
      setLoading(false);
    }
  };

  const fetchSubmissions = async () => {
    try {
      setLoading(true);
      const params: any = {
        page: submissionPagination.current,
        limit: submissionPagination.pageSize,
      };
      if (submissionFilters.status) params.status = submissionFilters.status;
      if (submissionFilters.dateFrom) params.dateFrom = submissionFilters.dateFrom;
      if (submissionFilters.dateTo) params.dateTo = submissionFilters.dateTo;

      const response = await orderApi.getShipperCODSubmissionHistory(params);
      setSubmissions(response.submissions || []);
      setSubmissionSummary(response.summary || submissionSummary);
      setSubmissionPagination((prev) => ({ ...prev, total: response.pagination?.total || 0 }));
    } catch (error) {
      console.error("Error fetching COD submissions:", error);
      message.error("Lỗi khi tải lịch sử nộp tiền COD");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitCOD = async (values: any) => {
    try {
      setLoading(true);
      if (selectedTransactions.length === 0) {
        message.error("Vui lòng chọn ít nhất một giao dịch COD");
        return;
      }

      await orderApi.submitShipperCOD({
        transactionIds: selectedTransactions,
        totalAmount: values.totalAmount,
        notes: values.notes,
      });

      message.success("Đã nộp tiền COD thành công");
      setSubmitModal(false);
      submitForm.resetFields();
      setSelectedTransactions([]);
      fetchTransactions();
    } catch (error) {
      console.error("Error submitting COD:", error);
      message.error("Lỗi khi nộp tiền COD");
    } finally {
      setLoading(false);
    }
  };

  const calculateTotalAmount = () => {
    return selectedTransactions.reduce((total, transactionId) => {
      const transaction = transactions.find((t) => t.id === transactionId);
      return total + (transaction?.codAmount || 0);
    }, 0);
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case "pending":
        return "default";
      case "success":
      case "collected":
        return "processing";
      case "submitted":
        return "success";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status.toLowerCase()) {
      case "pending":
        return "Chờ thu";
      case "success":
      case "collected":
        return "Đã thu";
      case "submitted":
        return "Đã nộp";
      default:
        return status;
    }
  };

  const getSubmissionStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case "PENDING":
        return "orange";
      case "MATCHED":
        return "success";
      case "ADJUSTED":
        return "blue";
      case "MISMATCHED":
        return "red";
      default:
        return "default";
    }
  };

  const getSubmissionStatusText = (status: string) => {
    switch (status.toUpperCase()) {
      case "PENDING":
        return "Chờ xác nhận";
      case "MATCHED":
        return "Khớp";
      case "ADJUSTED":
        return "Đã điều chỉnh";
      case "MISMATCHED":
        return "Không khớp";
      default:
        return status;
    }
  };

  const transactionColumns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => <Text strong>{text}</Text>,
    },
    {
      title: "Người nhận",
      key: "recipient",
      render: (record: CODTransaction) => (
        <Space direction="vertical" size={0}>
          <Text>{record.recipientName}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.recipientPhone}
          </Text>
        </Space>
      ),
    },
    {
      title: "Số tiền COD",
      dataIndex: "codAmount",
      key: "codAmount",
      render: (amount: number) => (
        <Text strong style={{ color: "#f50" }}>
          {amount.toLocaleString()}đ
        </Text>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status: string) => <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>,
    },
    {
      title: "Ngày thu",
      dataIndex: "collectedAt",
      key: "collectedAt",
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (record: CODTransaction) => (
        <Checkbox
          checked={selectedTransactions.includes(record.id)}
          onChange={(e) => {
            if (e.target.checked) {
              setSelectedTransactions([...selectedTransactions, record.id]);
            } else {
              setSelectedTransactions(selectedTransactions.filter((id) => id !== record.id));
            }
          }}
        >
          Chọn
        </Checkbox>
      ),
    },
  ];

  const submissionColumns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => <Text strong>{text}</Text>,
    },
    {
      title: "Số tiền hệ thống",
      dataIndex: "systemAmount",
      key: "systemAmount",
      render: (amount: number) => <Text>{amount.toLocaleString()}đ</Text>,
    },
    {
      title: "Số tiền thực nộp",
      dataIndex: "actualAmount",
      key: "actualAmount",
      render: (amount: number) => <Text strong>{amount.toLocaleString()}đ</Text>,
    },
    {
      title: "Chênh lệch",
      dataIndex: "discrepancy",
      key: "discrepancy",
      render: (discrepancy: number) => (
        <Text style={{ color: discrepancy !== 0 ? "#f50" : "#52c41a" }}>
          {discrepancy > 0 ? "+" : ""}
          {discrepancy.toLocaleString()}đ
        </Text>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status: string) => <Tag color={getSubmissionStatusColor(status)}>{getSubmissionStatusText(status)}</Tag>,
    },
    {
      title: "Ngày nộp",
      dataIndex: "paidAt",
      key: "paidAt",
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (record: CODSubmission) => (
        <Button icon={<EyeOutlined />} onClick={() => {
          setSelectedSubmission(record);
          setDetailModal(true);
        }}>
          Chi tiết
        </Button>
      ),
    },
  ];

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Title level={2} style={{ color: "#1C3D90", marginBottom: 24 }}>
        Quản lý COD
      </Title>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: "transactions",
            label: "Giao dịch COD",
            icon: <DollarOutlined />,
            children: (
              <Card>
                <Row gutter={16} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={12} lg={6}>
                    <Card>
                      <Statistic title="Tổng đã thu" value={summary.totalCollected} prefix={<DollarOutlined />} formatter={(value) => `${value?.toLocaleString()}đ`} />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} lg={6}>
                    <Card>
                      <Statistic title="Đã nộp" value={summary.totalSubmitted} prefix={<CheckCircleOutlined />} formatter={(value) => `${value?.toLocaleString()}đ`} />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} lg={6}>
                    <Card>
                      <Statistic title="Còn nợ" value={summary.totalPending} prefix={<ClockCircleOutlined />} formatter={(value) => `${value?.toLocaleString()}đ`} />
                    </Card>
                  </Col>
                  <Col xs={24} sm={12} lg={6}>
                    <Card>
                      <Statistic title="Tổng giao dịch" value={summary.transactionCount} />
                    </Card>
                  </Col>
                </Row>

                <Space style={{ marginBottom: 16 }}>
                  <Select
                    placeholder="Lọc theo trạng thái"
                    allowClear
                    style={{ width: 200 }}
                    onChange={(value) => setFilters({ ...filters, status: value || undefined })}
                  >
                    <Option value="PENDING">Chờ thu</Option>
                    <Option value="SUCCESS">Đã thu</Option>
                  </Select>
                  <RangePicker
                    onChange={(dates) => {
                      if (dates) {
                        setFilters({
                          ...filters,
                          dateFrom: dates[0]?.format("YYYY-MM-DD"),
                          dateTo: dates[1]?.format("YYYY-MM-DD"),
                        });
                      } else {
                        setFilters({ ...filters, dateFrom: undefined, dateTo: undefined });
                      }
                    }}
                  />
                  <Button onClick={fetchTransactions}>Tải lại</Button>
                  {selectedTransactions.length > 0 && (
                    <Button type="primary" icon={<SwapOutlined />} onClick={() => setSubmitModal(true)}>
                      Nộp tiền ({selectedTransactions.length})
                    </Button>
                  )}
                </Space>

                <Table
                  rowKey="id"
                  loading={loading}
                  columns={transactionColumns}
                  dataSource={transactions}
                  pagination={{
                    current: pagination.current,
                    pageSize: pagination.pageSize,
                    total: pagination.total,
                    onChange: (page, pageSize) => setPagination({ ...pagination, current: page, pageSize: pageSize || 10 }),
                  }}
                />
              </Card>
            ),
          },
          {
            key: "submissions",
            label: "Lịch sử nộp tiền",
            icon: <HistoryOutlined />,
            children: (
              <Card>
                <Row gutter={16} style={{ marginBottom: 16 }}>
                  <Col xs={24} sm={8}>
                    <Card>
                      <Statistic title="Tổng đã nộp" value={submissionSummary.totalSubmitted} prefix={<DollarOutlined />} formatter={(value) => `${value?.toLocaleString()}đ`} />
                    </Card>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Card>
                      <Statistic title="Tổng chênh lệch" value={submissionSummary.totalDiscrepancy} formatter={(value) => `${value?.toLocaleString()}đ`} />
                    </Card>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Card>
                      <Statistic title="Số lần nộp" value={submissionSummary.totalSubmissions} />
                    </Card>
                  </Col>
                </Row>

                <Space style={{ marginBottom: 16 }}>
                  <Select
                    placeholder="Lọc theo trạng thái"
                    allowClear
                    style={{ width: 200 }}
                    onChange={(value) => setSubmissionFilters({ ...submissionFilters, status: value || "" })}
                  >
                    <Option value="PENDING">Chờ xác nhận</Option>
                    <Option value="MATCHED">Khớp</Option>
                    <Option value="ADJUSTED">Đã điều chỉnh</Option>
                    <Option value="MISMATCHED">Không khớp</Option>
                  </Select>
                  <RangePicker
                    onChange={(dates) => {
                      if (dates) {
                        setSubmissionFilters({
                          ...submissionFilters,
                          dateFrom: dates?.[0]?.format("YYYY-MM-DD") ?? "",
                          dateTo: dates?.[1]?.format("YYYY-MM-DD") ?? "",
                        });
                      } else {
                        setSubmissionFilters({ ...submissionFilters, dateFrom: "", dateTo: "" });
                      }
                    }}
                  />
                  <Button onClick={fetchSubmissions}>Tải lại</Button>
                </Space>

                <Table
                  rowKey="id"
                  loading={loading}
                  columns={submissionColumns}
                  dataSource={submissions}
                  pagination={{
                    current: submissionPagination.current,
                    pageSize: submissionPagination.pageSize,
                    total: submissionPagination.total,
                    onChange: (page, pageSize) => setSubmissionPagination({ ...submissionPagination, current: page, pageSize: pageSize || 10 }),
                  }}
                />
              </Card>
            ),
          },
        ]}
      />

      <Modal
        title="Nộp tiền COD"
        open={submitModal}
        onOk={() => submitForm.submit()}
        onCancel={() => {
          setSubmitModal(false);
          submitForm.resetFields();
        }}
        width={600}
      >
        <Form form={submitForm} layout="vertical" onFinish={handleSubmitCOD}>
          <Form.Item label="Tổng số tiền hệ thống">
            <InputNumber
              value={calculateTotalAmount()}
              formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",") + "đ"}
              disabled
              style={{ width: "100%" }}
            />
          </Form.Item>
          <Form.Item
            name="totalAmount"
            label="Số tiền thực nộp"
            rules={[{ required: true, message: "Vui lòng nhập số tiền thực nộp" }]}
          >
            <InputNumber<number>
              formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",") + "đ"}
              parser={(value) => Number(value?.replace(/[^\d]/g, "") || 0)}
              style={{ width: "100%" }}
              min={0}
            />
          </Form.Item>
          <Form.Item name="notes" label="Ghi chú">
            <Input.TextArea rows={3} placeholder="Ghi chú về việc nộp tiền (nếu có)" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Chi tiết nộp tiền COD"
        open={detailModal}
        onCancel={() => {
          setDetailModal(false);
          setSelectedSubmission(null);
        }}
        footer={null}
        width={600}
      >
        {selectedSubmission && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="Mã đơn hàng">{selectedSubmission.trackingNumber}</Descriptions.Item>
            <Descriptions.Item label="Số tiền hệ thống">{selectedSubmission.systemAmount.toLocaleString()}đ</Descriptions.Item>
            <Descriptions.Item label="Số tiền thực nộp">{selectedSubmission.actualAmount.toLocaleString()}đ</Descriptions.Item>
            <Descriptions.Item label="Chênh lệch">
              <Text style={{ color: selectedSubmission.discrepancy !== 0 ? "#f50" : "#52c41a" }}>
                {selectedSubmission.discrepancy > 0 ? "+" : ""}
                {selectedSubmission.discrepancy.toLocaleString()}đ
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color={getSubmissionStatusColor(selectedSubmission.status)}>{getSubmissionStatusText(selectedSubmission.status)}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Ngày nộp">{dayjs(selectedSubmission.paidAt).format("DD/MM/YYYY HH:mm")}</Descriptions.Item>
            {selectedSubmission.checkedAt && (
              <Descriptions.Item label="Ngày xác nhận">{dayjs(selectedSubmission.checkedAt).format("DD/MM/YYYY HH:mm")}</Descriptions.Item>
            )}
            {selectedSubmission.notes && <Descriptions.Item label="Ghi chú">{selectedSubmission.notes}</Descriptions.Item>}
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default ShipperCODManagement;
