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
import type { PaymentCheck, PaymentRequest } from "../../../types/payment";
import paymentApi from "../../../api/paymentApi";
import PaymentModal from "./components/PaymentModal";


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

  const handleSubmitPayment = async (amount: number) => {
    if (!selectedSettlementBatch) return;

    try {
      const param: PaymentRequest = {
        settlementId: selectedSettlementBatch.id,
        amount
      };

      const result = await paymentApi.createVNPayURLFromList(param);

      if (result.success && result.data) {
        window.location.href = result.data;
        message.info("Đang chuyển tới VNPay để thanh toán...");
      } else {
        const errMsg = result.message || "Không tạo được link thanh toán";
        message.error(errMsg);
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi tạo link thanh toán VNPay");
    }
  };

  useEffect(() => {
    const checkPayment = async () => {
      const queryParams = new URLSearchParams(window.location.search);

      // Map các param VNPay sang PaymentCheck
      const paymentCheck: PaymentCheck = {
        transactionCode: queryParams.get("vnp_TxnRef") || "",
        responseCode: queryParams.get("vnp_ResponseCode") || "",
        referenceCode: queryParams.get("vnp_TransactionNo") || "",
        secureHash: queryParams.get("vnp_SecureHash") || "",
        amount: queryParams.get("vnp_Amount") || undefined,
        bankCode: queryParams.get("vnp_BankCode") || undefined,
        bankTranNo: queryParams.get("vnp_BankTranNo") || undefined,
        cardType: queryParams.get("vnp_CardType") || undefined,
        orderInfo: queryParams.get("vnp_OrderInfo") || undefined,
        payDate: queryParams.get("vnp_PayDate") || undefined,
        tmnCode: queryParams.get("vnp_TmnCode") || undefined,
        transactionStatus: queryParams.get("vnp_TransactionStatus") || undefined,
        secureHashType: queryParams.get("vnp_SecureHashType") || undefined,
      };

      // Kiểm tra param bắt buộc
      const requiredFields = ["transactionCode", "responseCode", "referenceCode", "secureHash"];
      const hasAllFields = requiredFields.every((field) => (paymentCheck as any)[field]);
      if (!hasAllFields) return;

      try {
        // Gọi backend để kiểm tra và cập nhật giao dịch
        const result = await paymentApi.checkPaymentVPN(paymentCheck);

        if (result.success) {
          if (result.data) {
            message.success(result.message || "Thanh toán phiên đối soát thành công");
            setProcessModalVisible(false);
            fetch(currentPage);
          } else {
            message.error(result.message || "Thanh toán phiên đối soát thất bại");
          }
          fetch(currentPage);
        } else {
          message.error(result.message || "Có lỗi xảy ra khi thanh toán phiên đối soát");
        }
      } catch (error) {
        message.error("Có lỗi xảy ra khi thanh toán phiên đối soát");
        console.error(error);
      }
    };

    checkPayment();
  }, []);


  const handleDetail = (id: number) => {
    navigate(`/settlements/${id}`);
  };

  const handleOpenModalSetSchedule = async () => {
    setModalSettlementScheduleVisible(true);
  };

  const handleSaveSettlementSchedule = async (selectedDays: string[]) => {
    try {
      setLoadingSchedule(true);
      const result = await userSettlementScheduleApi.updateUserSchedule(selectedDays);

      if (result.success) {
        message.success(result.message || "Cập nhật trạng thái thành công phiên đối soát");
        setUserWeekdays(selectedDays);
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
    const fetchUserSchedule = async () => {
      try {
        setLoadingSchedule(true);

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

    fetchUserSchedule();
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
                weekdays={userWeekdays}
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

        <PaymentModal
          visible={processModalVisible}
          settlementCode={selectedSettlementBatch?.code}
          remainAmount={selectedSettlementBatch?.remainAmount || 0}
          onCancel={() => {
            setProcessModalVisible(false);
            setSelectedSettlementBatch(null);
          }}
          onSubmit={(amount) => {
            handleSubmitPayment(amount);
            setProcessModalVisible(false);
          }}
        />
      </div>
    </div>
  );
};

export default UserSettlementBatchs;