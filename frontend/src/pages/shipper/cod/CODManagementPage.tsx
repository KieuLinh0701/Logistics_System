import React, { useEffect, useState } from "react";
import {
  Button,
  DatePicker,
  Form,
  Row,
  Select,
  Tabs,
  message,
} from "antd";
import {
  DollarOutlined,
  HistoryOutlined,
  SwapOutlined,
} from "@ant-design/icons";
import orderApi from "../../../api/orderApi";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";
import CODTransactionsStats from "./components/CODTransactionsStats";
import CODSubmissionsStats from "./components/CODSubmissionsStats";
import CODTransactionsTable from "./components/CODTransactionsTable";
import CODSubmissionsTable from "./components/CODSubmissionsTable";
import SubmitCODModal from "./components/SubmitCODModal";
import SubmissionDetailModal from "./components/SubmissionDetailModal";

const { Option } = Select;
const { RangePicker } = DatePicker;

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

const CODManagementPage: React.FC = () => {
  const [transactions, setTransactions] = useState<PaymentSubmissionItem[]>([]);
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
  const [submissions, setSubmissions] = useState<PaymentSubmissionItem[]>([]);
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
  const [selectedSubmission, setSelectedSubmission] = useState<PaymentSubmissionItem | null>(null);

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
      message.error("Lỗi khi tải danh sách giao dịch thu tiền");
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
      message.error("Lỗi khi tải lịch sử nộp tiền");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitCOD = async (values: any) => {
    try {
      setLoading(true);
      if (selectedTransactions.length === 0) {
        message.error("Vui lòng chọn ít nhất một giao dịch thu tiền");
        return;
      }

      await orderApi.submitShipperCOD({
        transactionIds: selectedTransactions,
        totalAmount: values.totalAmount,
        notes: values.notes,
      });

      message.success("Đã nộp tiền thu được thành công");
      setSubmitModal(false);
      submitForm.resetFields();
      setSelectedTransactions([]);
      fetchTransactions();
    } catch (error) {
      console.error("Error submitting COD:", error);
      message.error("Lỗi khi nộp tiền thu được");
    } finally {
      setLoading(false);
    }
  };

  const calculateTotalAmount = () => {
    return selectedTransactions.reduce((total, transactionId) => {
      const transaction = transactions.find((t) => t.id === transactionId);
      return total + (transaction?.systemAmount || 0);
    }, 0);
  };

  const handleTransactionsPageChange = (page: number, pageSize: number) => {
    setPagination({ ...pagination, current: page, pageSize });
  };

  const handleSubmissionsPageChange = (page: number, pageSize: number) => {
    setSubmissionPagination({ ...submissionPagination, current: page, pageSize });
  };

  const handleViewDetail = (record: PaymentSubmissionItem) => {
    setSelectedSubmission(record);
    setDetailModal(true);
  };

  const closeDetailModal = () => {
    setDetailModal(false);
    setSelectedSubmission(null);
  };

  const closeSubmitModal = () => {
    setSubmitModal(false);
    submitForm.resetFields();
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Quản lý tiền thu được</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">
                {activeTab === "transactions"
                  ? `Giao dịch: ${transactions.length} dòng`
                  : `Lịch sử nộp: ${submissions.length} dòng`}
              </div>
            </div>
          </div>
        </div>

        <Tabs
          className="shipper-cod-tabs"
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: "transactions",
              label: "Giao dịch thu tiền",
              icon: <DollarOutlined />,
              children: (
                <div className="shipper-cod-tab-block">
                  <div className="shipper-filter-panel">
                    <div className="shipper-filter-grow" />
                    <div className="shipper-filter-actions">
                      <Select
                        placeholder="Lọc theo trạng thái"
                        allowClear
                        style={{ width: 200 }}
                        value={filters.status}
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
                        <Button
                          type="primary"
                          className="primary-button"
                          icon={<SwapOutlined />}
                          onClick={() => setSubmitModal(true)}
                        >
                          Nộp tiền ({selectedTransactions.length})
                        </Button>
                      )}
                    </div>
                  </div>

                  <div className="shipper-stats-section">
                    <CODTransactionsStats summary={summary} />
                  </div>

                  <div className="list-page-table shipper-page-table">
                    <CODTransactionsTable
                      transactions={transactions}
                      selectedTransactions={selectedTransactions}
                      loading={loading}
                      pagination={pagination}
                      onSelectionChange={setSelectedTransactions}
                      onPageChange={handleTransactionsPageChange}
                    />
                  </div>
                </div>
              ),
            },
            {
              key: "submissions",
              label: "Lịch sử nộp tiền",
              icon: <HistoryOutlined />,
              children: (
                <div className="shipper-cod-tab-block">
                  <div className="shipper-filter-panel">
                    <div className="shipper-filter-grow" />
                    <div className="shipper-filter-actions">
                      <Select
                        placeholder="Lọc theo trạng thái"
                        allowClear
                        style={{ width: 200 }}
                        value={submissionFilters.status || undefined}
                        onChange={(value) =>
                          setSubmissionFilters({ ...submissionFilters, status: value || "" })
                        }
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
                    </div>
                  </div>

                  <div className="shipper-stats-section">
                    <CODSubmissionsStats summary={submissionSummary} />
                  </div>

                  <div className="list-page-table shipper-page-table">
                    <CODSubmissionsTable
                      submissions={submissions}
                      loading={loading}
                      pagination={submissionPagination}
                      onPageChange={handleSubmissionsPageChange}
                      onViewDetail={handleViewDetail}
                    />
                  </div>
                </div>
              ),
            },
          ]}
        />
      </div>

      <SubmitCODModal
        open={submitModal}
        loading={loading}
        form={submitForm}
        totalAmount={calculateTotalAmount()}
        onOk={() => submitForm.submit()}
        onCancel={closeSubmitModal}
      />

      <SubmissionDetailModal
        open={detailModal}
        submission={selectedSubmission}
        onClose={closeDetailModal}
      />
    </div>
  );
};

export default CODManagementPage;
