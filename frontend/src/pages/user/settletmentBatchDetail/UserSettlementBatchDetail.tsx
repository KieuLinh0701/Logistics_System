import { useEffect, useRef, useState } from "react";
import { Col, Form, Row, Tabs, Tag, message } from "antd";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import Actions from "./components/Actions";
import Title from "antd/es/typography/Title";
import { CheckCircleOutlined, PayCircleOutlined, ShoppingOutlined } from "@ant-design/icons";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import type { SearchRequest } from "../../../types/request";
import "./UserSettlementBatchDetail.css";
import settlementBatchApi from "../../../api/settlementBatchApi";
import type { Order } from "../../../types/order";
import DataTable from "./components/Table";
import SettlementTransactionTable from "./components/SettlementTransactionTable";
import type { SettlementTransaction } from "../../../types/settlementTransaction";
import type { SettlementBatch } from "../../../types/settlementBatch";
import paymentApi from "../../../api/paymentApi";
import type { PaymentCheck, PaymentRequest } from "../../../types/payment";
import { canPayUserSettlementBatch } from "../../../utils/settlementBatchUtils";
import PaymentModal from "./components/PaymentModal";


const UserSettlementBatchDetail = () => {
  const { id } = useParams<{ id: string }>();
  const settlementId = Number(id);
  const navigate = useNavigate();
  const latestRequestRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();
  const [currentTab, setCurrentTab] = useState("orders");

  const [loading, setLoading] = useState(false);
  const [datas, setDatas] = useState<Order[] | []>([]);
  const [settlement, setSettlement] = useState<SettlementBatch | null>(null);

  const [transactions, setTransaction] = useState<SettlementTransaction[] | []>([]);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [searchText, setSearchText] = useState("");
  const [filterSort, setFilterSort] = useState("NEWEST");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterPayer, setFilterPayer] = useState("ALL");
  const [filterCod, setFilterCod] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const [canPay, setCanPay] = useState(false);

  const [processModalVisible, setProcessModalVisible] = useState(false);

  const updateURL = () => {
    const params: any = {};
    params.tab = currentTab;

    if (currentTab === "payments") {
      setSearchParams(params, { replace: true });
      return;
    }

    if (searchText) params.search = searchText;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterCod !== "ALL") params.cod = filterCod.toLowerCase();
    if (filterPayer !== "ALL") params.payer = filterPayer.toLowerCase();
    params.sort = (filterSort ?? "NEWEST").toLowerCase();
    if (currentPage) params.page = currentPage;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const pageParam = Number(searchParams.get("page")) || 1;
    const tabFromUrl = searchParams.get("tab");

    if (tabFromUrl === "payments") {
      return;
    }

    const s = searchParams.get("search");
    const p = searchParams.get("payer")?.toLocaleUpperCase();
    const st = searchParams.get("status")?.toLocaleUpperCase();
    const cod = searchParams.get("cod")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    setCurrentPage(pageParam);
    if (s) setSearchText(s);
    if (p) setFilterPayer(p);
    if (st) setFilterStatus(st);
    if (cod) setFilterCod(cod);
    if (sort) setFilterSort(sort);
    if (tabFromUrl) setCurrentTab(tabFromUrl);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetchdata = async (page = currentPage) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;

      const payload: SearchRequest = {
        page,
        limit: pageSize,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        search: searchText,
        sort: filterSort,
        payer: filterPayer !== "ALL" ? filterPayer : undefined,
        cod: filterCod !== "ALL" ? filterCod : undefined,
      };
      if (dateRange) {
        payload.startDate = dateRange[0]
          .startOf("day")
          .format("YYYY-MM-DDTHH:mm:ss");

        payload.endDate = dateRange[1]
          .endOf("day")
          .format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await settlementBatchApi.listUserOrdersBySettlementBatchId(settlementId, payload);

      if (requestId !== latestRequestRef.current) return;

      if (result.success && result.data) {
        const list = result.data?.list || [];
        setDatas(list);
        setTotal(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách đơn hàng thuộc phiên đối soát của bạn");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách đơn hàng thuộc phiên đối soát của bạn");
    } finally {
      setLoading(false);
    }
  };

  const fetchSettlement = async () => {
    if (!settlementId) return;
    try {
      setLoading(true);

      const result = await settlementBatchApi.getbyUserSettlementBatchId(settlementId);
      if (result.success && result.data) {
        setSettlement(result.data);
      } else {
        setSettlement(null);
        message.error(result.message || "Lỗi khi lấy chi tiết phiên đối soát của bạn");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy chi tiết phiên đối soát của bạn");
      setSettlement(null);
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
        payer: filterPayer !== "ALL" ? filterPayer : undefined,
        cod: filterCod !== "ALL" ? filterCod : undefined,
      };

      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await settlementBatchApi.exportUserSettlementBatchDetail(settlementId, param);

      if (!result.success) {
        message.error("Xuất báo cáo thất bại");
        console.error("Export thất bại:", result.error);
      }

    } catch (error: any) {
      message.error(error.message || "Xuất báo cáo thất bại")
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitPayment = async (amount: number) => {
    if (!settlement) return;

    try {
      const param: PaymentRequest = {
        settlementId: settlement.id,
        amount
      };

      const result = await paymentApi.createVNPayURLFromDetail(param);

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
    if (settlement) {
      setCanPay(canPayUserSettlementBatch(settlement));
    }
  }, [settlement]);

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
            fetchTransaction();
            setCurrentPage(1);
            fetchdata(1);
            fetchSettlement();
          } else {
            message.error(result.message || "Thanh toán phiên đối soát thất bại");
          }
          fetchSettlement();
        } else {
          message.error(result.message || "Có lỗi xảy ra khi thanh toán phiên đối soát");
        }
      } catch (error: any) {
        message.error(error.message || "Có lỗi xảy ra khi thanh toán phiên đối soát");
      }
    };

    checkPayment();
  }, []);

  const handleDetail = (trackingNumber: string) => {
    navigate(`/orders/tracking/${trackingNumber}`);
  };

  const handleOpenModalPay = () => {
    setProcessModalVisible(true);
  };

  const fetchTransaction = async () => {
    try {
      setLoading(true);
      const result = await settlementBatchApi.listUserSettlementTransactionsBySettlementBatchId(settlementId);
      if (result.success && result.data) {
        const list = result.data || [];
        setTransaction(list);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách lịch sử thanh toán thuộc phiên đối soát của bạn");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách lịch sử thanh toán thuộc phiên đối soát của bạn");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransaction();
    fetchSettlement();
  }, []);

  useEffect(() => {
    if (currentTab !== "orders") return;

    fetchdata(currentPage);
    updateURL();
  }, [pageSize, currentPage, searchText, dateRange, filterSort, filterStatus, filterPayer, filterCod]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <CheckCircleOutlined className="title-icon" />
              Chi tiết phiên đối soát #{settlement?.code}
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                canPay={canPay}
                onPay={handleOpenModalPay}
                onExport={handleExport}
              />
            </div>
          </Col>
        </Row>

        <Tabs
          className="custom-tabs"
          activeKey={currentTab}
          onChange={(key) => {
            setCurrentTab(key);

            if (key === "payments") {
              setCurrentPage(1);
              setSearchText("");
              setFilterStatus("ALL");
              setFilterSort("NEWEST");
              setFilterPayer("ALL");
              setFilterCod("ALL");
              setDateRange(null);

              setSearchParams({ tab: "payments" }, { replace: true });
            } else {
              setSearchParams({ tab: "orders", page: "1" }, { replace: true });
            }
          }}
          items={[
            {
              key: 'orders',
              label: (
                <span className="tab-label">
                  <ShoppingOutlined /> Danh sách đơn hàng
                </span>
              ),
              children:
                <>
                  <SearchFilters
                    searchText={searchText}
                    setSearchText={setSearchText}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                    filters={{
                      sort: filterSort,
                      status: filterStatus,
                      payer: filterPayer,
                      cod: filterCod
                    }}
                    setFilters={(key, val) => {
                      if (key === "sort") setFilterSort(val as string);
                      if (key === "status") setFilterStatus(val as string);
                      if (key === "payer") setFilterPayer(val as string);
                      if (key === "cod") setFilterCod(val as string);
                      setCurrentPage(1);
                    }}
                    onReset={() => {
                      setSearchText("");
                      setFilterStatus("ALL");
                      setFilterSort("NEWEST");
                      setFilterPayer("ALL");
                      setFilterCod("ALL");
                      setDateRange(null);
                      setCurrentPage(1);
                    }}
                  />

                  <div className="list-page-header" />

                  <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>

                  <DataTable
                    datas={datas}
                    onDetail={handleDetail}
                    currentPage={currentPage}
                    pageSize={pageSize}
                    total={total}
                    loading={loading}
                    onPageChange={(page, size) => {
                      setCurrentPage(page);
                      if (size) setPageSize(size);
                    }}
                  />
                </>
            },
            {
              key: 'payments',
              label: (
                <span className="tab-label">
                  <PayCircleOutlined /> Hóa đơn thanh toán
                </span>
              ),
              children:
                <>
                  <div className="list-page-header" />

                  {transactions.length > 0 && (<Tag className="list-page-tag">{transactions.length} giao dịch</Tag>)}

                  <SettlementTransactionTable
                    datas={transactions}
                    loading={loading}
                  />
                </>
            }
          ]}
        />

        <PaymentModal
          visible={processModalVisible}
          settlementCode={settlement?.code}
          remainAmount={settlement?.remainAmount || 0}
          onCancel={() => {
            setProcessModalVisible(false);
            fetchSettlement();
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

export default UserSettlementBatchDetail;