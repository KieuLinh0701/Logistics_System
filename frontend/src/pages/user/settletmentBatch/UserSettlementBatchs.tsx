import { useEffect, useState } from "react";
import { Col, Form, Row, Tag, message } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import SubmissionTable from "./components/Table";
import Actions from "./components/Actions";
import Title from "antd/es/typography/Title";
import { CheckCircleOutlined } from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import type { SearchRequest } from "../../../types/request";
import "./UserSettlementBatchs.css";
import type { SettlementBatch } from "../../../types/settlementBatch";
import settlementBatchApi from "../../../api/settlementBatchApi";
import UserScheduleModal from "./components/UserScheduleModal";
import userSettlementScheduleApi from "../../../api/userSettlementScheduleApi";


const UserSettlementBatchs = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [searchParams, setSearchParams] = useSearchParams();

  const [loading, setLoading] = useState(false);
  const [loadingSchedule, setLoadingSchedule] = useState(false);
  const [settlementBatchs, setSettlementBatchs] = useState<SettlementBatch[] | []>([]);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState("NEWEST");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterType, setFilterType] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [modalSettlementScheduleVisible, setModalSettlementScheduleVisible] = useState(false);
  const [userWeekdays, setUserWeekdays] = useState<string[]>([]);

  const [processModalVisible, setProcessModalVisible] = useState(false);
  const [selectedSettlementBatch, setSelectedSettlementBatch] = useState<SettlementBatch | null>(null);

  const [isModalOpen, setIsModalOpen] = useState(false);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterType !== "ALL") params.type = filterType.toLowerCase();
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
    const t = searchParams.get("type")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    if (s) setSearchText(s);
    if (t) setFilterType(t);
    if (st) setFilterStatus(st);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetch = async (page = currentPage) => {
    try {
      setLoading(true);
      const payload: SearchRequest = {
        page,
        limit: pageSize,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        search: searchText,
        sort: filterSort,
        type: filterType,
      };
      if (dateRange) {
        payload.startDate = dateRange[0]
          .startOf("day")
          .format("YYYY-MM-DDTHH:mm:ss");

        payload.endDate = dateRange[1]
          .endOf("day")
          .format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await settlementBatchApi.listUserSettlementBatchs(payload);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setSettlementBatchs(list);
        setTotal(result.data.pagination?.total || 0);
        setCurrentPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách phiên đối soát của bưu cục");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách phiên đối soát của bạn");
      console.error("Error fetching settlement batchs:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    try {
      setLoading(true);
      const param: SearchRequest = {
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        search: searchText,
        sort: filterSort,
        type: filterType,
      };

      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await settlementBatchApi.exportUserSettlementBatchs(param);

      if (!result.success) {
        message.error("Xuất báo cáo thất bại");
        console.error("Export thất bại:", result.error);
      }

    } catch (error: any) {
      message.error(error.message || "Xuất báo cáo thất bại")
      console.error("Export lỗi:", error.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePayment = (batch: SettlementBatch) => {
    setSelectedSettlementBatch(batch);
    setProcessModalVisible(true);
  };

  const handleSubmitProcess = async (values: string[]) => {
    if (!selectedSettlementBatch) return;

    try {
      // const param: ManagerPaymentSubmissionBatchEditRequest = {
      //   status,
      //   notes
      // }
      // const result = await paymentSubmissionBatchApi.updateManagerPaymentSubmissionBatch(selectedSettlementBatch.id, param);

      // if (result.success) {
      //   message.success(result.message || "Cập nhật trạng thái thành công phiên đối soát");
      //   fetch(currentPage);
      //   setSelectedSettlementBatch(null);
      // } else {
      //   message.error(result.message || "Cập nhật trạng thái thất bại phiên đối soát");
      // }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi cập nhật trạng thái phiên đối soát");
    }
  };

  const handleDetail = (id: number) => {
    navigate(`/settlements/${id}`);
  };

  const handleOpenModalSetSchedule = async () => {
    try {
      setLoadingSchedule(true);
      setModalSettlementScheduleVisible(true);

      const result = await userSettlementScheduleApi.getUserSchedule();
      if (result.success && result.data) {
        setUserWeekdays(result.data.weekdays || []);
      } else {
        message.error(result.message || "Không lấy được lịch đối soát");
        setUserWeekdays([]);
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy lịch đối soát");
      setUserWeekdays([]);
    } finally {
      setLoadingSchedule(false);
    }
  };

  const handleSaveSettlementSchedule = async (selectedDays: string[]) => {
    try {
      setLoadingSchedule(true);
      const result = await userSettlementScheduleApi.updateUserSchedule(selectedDays);

      if (result.success) {
        message.success(result.message || "Cập nhật trạng thái thành công phiên đối soát");
        setSelectedSettlementBatch(null);
        setModalSettlementScheduleVisible(false);
      } else {
        message.error(result.message || "Cập nhật trạng thái thất bại phiên đối soát");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi cập nhật trạng thái phiên đối soát");
    } finally {
      setLoadingSchedule(false);
    }
  };

  useEffect(() => {
    fetch();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
    fetch(1);
    updateURL();
  }, [searchText, dateRange, filterSort, filterStatus, filterType]);

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
            type: filterType,
          }}
          setFilters={(key, val) => {
            if (key === "sort") setFilterSort(val as string);
            if (key === "status") setFilterStatus(val as string);
            if (key === "type") setFilterType(val as string);
          }}
          onReset={() => {
            setSearchText("");
            setFilterStatus("ALL");
            setFilterSort("NEWEST");
            setFilterType("ALL");
            setDateRange(null);
            setCurrentPage(1);
          }}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <CheckCircleOutlined className="title-icon" />
              Lịch sử đối soát
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                onSetSchedule={handleOpenModalSetSchedule}
                onExport={handleExport}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} phiên đối soát</Tag>

        <SubmissionTable
          datas={settlementBatchs}
          onProcess={handlePayment}
          onDetail={handleDetail}
          currentPage={currentPage}
          pageSize={pageSize}
          total={total}
          loading={loading}
          onPageChange={(page, size) => {
            setCurrentPage(page);
            if (size) setPageSize(size);
            fetch(page);
          }}
        />

        <UserScheduleModal
          visible={modalSettlementScheduleVisible}
          initialWeekdays={userWeekdays}
          loading={loadingSchedule}
          onCancel={() => setModalSettlementScheduleVisible(false)}
          onSave={handleSaveSettlementSchedule}
        />
      </div>
    </div>
  );
};

export default UserSettlementBatchs;