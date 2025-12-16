import { useEffect, useState } from "react";
import { Col, Row, Tag, message } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import SubmissionTable from "./components/Table";
import Actions from "./components/Actions";
import Title from "antd/es/typography/Title";
import { CheckCircleOutlined } from "@ant-design/icons";
import { useParams, useSearchParams } from "react-router-dom";
import type { SearchRequest } from "../../../types/request";
import "./ManagerPaymentSubmissions.css";
import paymentSubmissionApi from "../../../api/paymentSubmissionApi";
import type { ManagerPaymentSubmission, ManagerPaymentSubmissionEditRequest } from "../../../types/paymentSubmission";
import ProcessPaymentSubmissionModal from "./components/ProcessPaymentSubmissionModal";


const ManagerPaymentSubmissions = () => {
  const { id } = useParams<{ id: string }>();
  const batchId = Number(id);
  const [searchParams, setSearchParams] = useSearchParams();

  const [loading, setLoading] = useState(false);
  const [paymentSubmissions, setPaymentSubmissions] = useState<ManagerPaymentSubmission[] | []>([]);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState("NEWEST");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [processModalVisible, setProcessModalVisible] = useState(false);
  const [selectedSubmission, setSelectedSubmission] = useState<ManagerPaymentSubmission | null>(null);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    params.sort = (filterSort ?? "NEWEST").toLowerCase();
    if (currentPage) params.page = currentPage;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const s = searchParams.get("search");
    const st = searchParams.get("status")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    if (s) setSearchText(s);
    if (st) setFilterStatus(st);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetchPaymentSubmissions = async (page = currentPage) => {
    if (isNaN(batchId)) return;
    try {
      setLoading(true);
      const payload: SearchRequest = {
        page,
        limit: pageSize,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        search: searchText,
        sort: filterSort,
      };
      if (dateRange) {
        payload.startDate = dateRange[0]
          .startOf("day")
          .format("YYYY-MM-DDTHH:mm:ss");

        payload.endDate = dateRange[1]
          .endOf("day")
          .format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await paymentSubmissionApi.listManagerPaymentSubmissions(batchId, payload);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setPaymentSubmissions(list);
        setTotal(result.data.pagination?.total || 0);
        setCurrentPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách đối soát của bưu cục");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách đối soát của bưu cục");
      console.error("Error fetching payment submission:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPaymentSubmissions();
  }, [batchId]);

  const handleExportPaymentSubmissions = async () => {
    if (isNaN(batchId)) return;
    try {
      setLoading(true);
      const param: SearchRequest = {
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        search: searchText,
        sort: filterSort,
      };

      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await paymentSubmissionApi.exportManagerPaymentSubmissions(batchId, param);

      if (!result.success) {
        console.error("Export thất bại:", result.error);
      }

    } catch (error: any) {
      console.error("Export lỗi:", error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleProcessPaymentSubmission = (submission: ManagerPaymentSubmission) => {
    setSelectedSubmission(submission);
    setProcessModalVisible(true);
  };

  const handleSubmitProcess = async (status: string, notes: string) => {
    if (!selectedSubmission) return;

    try {
      const param: ManagerPaymentSubmissionEditRequest = {
        status,
        notes
      }
      const result = await paymentSubmissionApi.updateManagerPaymentSubmission(selectedSubmission.id, param);

      if (result.success) {
        message.success(result.message || "Cập nhật trạng thái thành công phiên đối soát");
        fetchPaymentSubmissions(currentPage);
        setSelectedSubmission(null);
      } else {
        message.error(result.message || "Cập nhật trạng thái thất bại phiên đối soát");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi cập nhật trạng thái phiên đối soát");
    }
  };

  useEffect(() => {
    fetchPaymentSubmissions();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
    fetchPaymentSubmissions(1);
    updateURL();
  }, [searchText, dateRange, filterSort, filterStatus]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          setSearchText={setSearchText}
          dateRange={dateRange}
          setDateRange={setDateRange}
          filters={{
            sort: filterSort,
            status: filterStatus,
          }}
          setFilters={(key, val) => {
            if (key === "sort") setFilterSort(val as string);
            if (key === "status") setFilterStatus(val as string);
          }}
          onReset={() => {
            setSearchText("");
            setFilterStatus("ALL");
            setFilterSort("NEWEST");
            setDateRange(null);
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <CheckCircleOutlined className="title-icon" />
              Chi tiết phiên đối soát
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                onExport={handleExportPaymentSubmissions}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} đối soát</Tag>

        <SubmissionTable
          submissions={paymentSubmissions}
          onProcess={handleProcessPaymentSubmission}
          currentPage={currentPage}
          pageSize={pageSize}
          total={total}
          loading={loading}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetchPaymentSubmissions(page);
          }}
        />

        <ProcessPaymentSubmissionModal
          visible={processModalVisible}
          submission={selectedSubmission}
          onClose={() => setProcessModalVisible(false)}
          onSubmit={handleSubmitProcess}
          loading={loading}
        />
      </div>
    </div>
  );
};

export default ManagerPaymentSubmissions;