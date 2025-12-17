import { useEffect, useState } from "react";
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


const UserSettlementBatchDetail = () => {
  const { id } = useParams<{ id: string }>();
  const settlementId = Number(id);
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [searchParams, setSearchParams] = useSearchParams();
  const [currentTab, setCurrentTab] = useState("orders");

  const [loading, setLoading] = useState(false);
  const [datas, setDatas] = useState<Order[] | []>([]);

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

  const updateURL = () => {
    const params: any = {};
    params.tab = currentTab;
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
    const tabFromUrl = searchParams.get("tab");
    const s = searchParams.get("search");
    const p = searchParams.get("payer")?.toLocaleUpperCase();
    const st = searchParams.get("status")?.toLocaleUpperCase();
    const cod = searchParams.get("cod")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

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
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setDatas(list);
        setTotal(result.data.pagination?.total || 0);
        setCurrentPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách đơn hàng thuộc phiên đối soát của bạn");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách đơn hàng thuộc phiên đối soát của bạn");
      console.error("Error fetchdataing orders:", error);
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
      console.error("Export lỗi:", error.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePayment = () => {

  };

  const handleDetail = (trackingNumber: string) => {
    navigate(`/orders/tracking/${trackingNumber}`);
  };

  const handleOpenModalPay = () => {
    // setIsModalOpen(true);
  }

  useEffect(() => {
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
        console.error("Error fetchdataing transactions:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchTransaction();
    fetchdata();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
    fetchdata(1);
    updateURL();
  }, [searchText, dateRange, filterSort, filterStatus, filterPayer, filterCod]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
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
                onPay={handleOpenModalPay}
                onExport={handleExport}
              />
            </div>
          </Col>
        </Row>

        <Tabs
          className="custom-tabs"
          activeKey={currentTab}
          onChange={(key) => setSearchParams({ ...Object.fromEntries(searchParams), tab: key })}
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
                      fetchdata(page);
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
      </div>
    </div>
  );
};

export default UserSettlementBatchDetail;