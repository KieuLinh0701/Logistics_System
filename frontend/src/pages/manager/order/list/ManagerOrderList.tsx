import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { message, Tag, Row, Col } from "antd";
import dayjs from "dayjs";
import OrderActions from "./components/Actions";
import SearchFilters from "./components/SearchFilters";
import OrderTable from "./components/Table";
import Title from "antd/es/typography/Title";
import type { ManagerOrderSearchRequest, Order } from "../../../../types/order";
import orderApi from "../../../../api/orderApi";
import "../../../../styles/ListPage.css";
import type { ServiceType } from "../../../../types/serviceType";
import serviceTypeApi from "../../../../api/serviceTypeApi";
import { ShoppingOutlined } from "@ant-design/icons";
import ConfirmCancelModal from "../detail/components/ConfirmCancelModal";
import ConfirmModal from "../../../common/ConfirmModal";

const ManagerOrderList = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [limit, setLimit] = useState(10);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState<number>(0);

  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);

  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);

  const [search, setSearch] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterSort, setFilterSort] = useState("NEWEST");
  const [filterPayer, setFilterPayer] = useState("ALL");
  const [filterPaymentStatus, setFilterPaymentStatus] = useState("ALL");
  const [filterServiceType, setFilterServiceType] = useState<number | null>(null);
  const [filterCOD, setFilterCOD] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [modalConfirmOpen, setModalConfirmOpen] = useState(false);
  const [selectedOrderIds, setSelectedOrderIds] = useState<number[] | []>([]);

  const [orderId, setOrderId] = useState<number | null>(null);
  const [initialized, setInitialized] = useState(false);

  const updateURL = () => {
    const params: any = {};

    if (search) params.search = search;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterPayer !== "ALL") params.payer = filterPayer.toLowerCase();
    if (filterPaymentStatus !== "ALL") params.payment = filterPaymentStatus.toLowerCase();
    if (filterServiceType !== null) params.service = filterServiceType;
    if (filterCOD !== "ALL") params.cod = filterCOD.toLowerCase();
    params.sort = filterSort.toLowerCase();
    if (page) params.page = page;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
    setInitialized(true);
  };

  useEffect(() => {
    const s = searchParams.get("search");
    const status = searchParams.get("status")?.toLocaleUpperCase();
    const payer = searchParams.get("payer")?.toLocaleUpperCase();
    const payment = searchParams.get("payment")?.toLocaleUpperCase();
    const service = searchParams.get("service");
    const cod = searchParams.get("cod")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    if (s) setSearch(s);
    if (status) setFilterStatus(status);
    if (payer) setFilterPayer(payer);
    if (payment) setFilterPaymentStatus(payment);

    if (service) setFilterServiceType(Number(service));

    if (cod) setFilterCOD(cod);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, []);

  // --- Fetch Orders ---
  const fetchOrders = async (currentPage = page) => {
    try {
      setLoading(true);

      const param: ManagerOrderSearchRequest = {
        page: currentPage,
        limit: limit,
        search: search,
        payer: filterPayer !== "ALL" ? filterPayer : undefined,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        serviceTypeId: filterServiceType !== null ? filterServiceType : undefined,
        paymentStatus: filterPaymentStatus !== "ALL" ? filterPaymentStatus : undefined,
        cod: filterCOD !== "ALL" ? filterCOD : undefined,
        sort: filterSort,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await orderApi.listManagerOrders(param);
      if (result.success && result.data) {
        const orderList = result.data?.list || [];
        setOrders(orderList);
        setTotal(result.data.pagination?.total || 0);
        setPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách đơn hàng");
      }
    } catch (error) {
      console.error("Error fetching orders:", error);
    } finally {
      setLoading(false);
    }
  };

  // --- Cancel Order ---
  const handleCancelOrder = (id: number) => {
    setOrderId(id);
    setCancelModalOpen(true);
  };

  const confirmCancelOrder = async () => {
    setLoading(true);
    try {
      if (!orderId) return;
      const result = await orderApi.cancelManagerOrder(orderId);
      if (result.success && result.data) {
        message.success("Hủy đơn hàng thành công");
        fetchOrders(page);
      } else {
        message.error(result.message || "Hủy đơn thất bại");
      }
    } catch (error: any) {
      message.error("Lỗi server khi hủy đơn hàng");
      console.log("Lỗi server khi hủy đơn hàng: ", error.message);
    } finally {
      setCancelModalOpen(false);
      setLoading(false);
    }
  };

  const handlePrintOrder = (id: number) => {
    if (!id) return;

    navigate(`/orders/print?orderIds=${id}`);
  };

  const handlePrintSelectedOrders = () => {
    if (!selectedOrderIds.length) {
      message.warning("Vui lòng chọn đơn hàng để in phiếu vận đơn");
      return;
    }

    navigate(`/orders/print?orderIds=${selectedOrderIds.join(",")}`);
  };

  const handleAddShipment = () => {
    message.warning("Chức năng này đang được phát triển");
  };

  const handleEditOrder = (id: number, tracking: string) => {
    if (tracking !== null) {
      navigate(`/orders/tracking/${tracking}/edit`);
    } else {
      navigate(`/orders/id/${id}/edit`);
    }
  };

  const handleAdd = () => {
    navigate(`/orders/create`);
  };

  const handleAtOriginOfficeOrder = (id: number) => {
    setOrderId(id);
    setModalConfirmOpen(true);
  };

  const confirmAtOriginOfficeOrder = async () => {
    if (!orderId) return;

    setModalConfirmOpen(false);

    try {
      setLoading(true);

      const result = await orderApi.setManagerOrderAtOriginOffice(orderId);

      if (result.success) {
        message.success(result.message || "Đơn hàng đã bàn giao cho bưu cục xuất phát thành công.");
        fetchOrders(page);
      } else {
        message.error(result.message || "Có lỗi khi bàn giao đơn hàng cho bưu cục xuất phát!");
      }
    } catch (err: any) {
      message.error(err.message || "Có lỗi khi khi bàn giao đơn hàng cho bưu cục xuất phát!");
    } finally {
      setLoading(false);
      setOrderId(null);
    }
  };

  const handleClearFilters = () => {
    setSearch("");
    setFilterStatus("ALL");
    setFilterServiceType(null);
    setFilterSort("NEWEST");
    setFilterPayer("ALL");
    setFilterPaymentStatus("ALL");
    setFilterCOD("ALL");
    setDateRange(null);
    setPage(1);
  };

  const handleFilterChange = (filter: string, value: string) => {
    switch (filter) {
      case 'status':
        setFilterStatus(value);
        break;
      case 'payer':
        setFilterPayer(value);
        break;
      case 'paymentStatus':
        setFilterPaymentStatus(value);
        break;
      case 'serviceType':
        setFilterServiceType(Number(value));
        break;
      case 'cod':
        setFilterCOD(value);
        break;
      case 'sort':
        setFilterSort(value);
        break;
    }
  };

  useEffect(() => {
    const fetchServiceTypes = async () => {
      try {
        setLoading(true);
        const response = await serviceTypeApi.getActiveServiceTypes();
        if (response.success && response.data) {
          setServiceTypes(response.data);
        }
      } catch (error) {
        console.error("Error fetching Service types:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
    fetchServiceTypes();
  }, []);

  useEffect(() => {
    if (!initialized) return;
    
    updateURL();
    setPage(1);
    fetchOrders(1);
  }, [search, filterStatus, filterServiceType, filterPayer, filterPaymentStatus, filterCOD, dateRange, filterSort]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          search={search}
          setSearch={setSearch}
          dateRange={dateRange}
          setDateRange={setDateRange}
          showAdvancedFilters={showAdvancedFilters}
          setShowAdvancedFilters={setShowAdvancedFilters}
          filters={{ status: filterStatus, payer: filterPayer, paymentStatus: filterPaymentStatus, serviceType: filterServiceType, cod: filterCOD, sort: filterSort }}
          setFilters={handleFilterChange}
          serviceTypes={serviceTypes}
          onReset={handleClearFilters}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <ShoppingOutlined className="title-icon" />
              Danh sách đơn hàng
            </Title>
          </Col>
          <Col>
            <div className="list-page-actions">
              <OrderActions
                onAdd={handleAdd}
                onPrint={handlePrintSelectedOrders}
                onAddShipment={handleAddShipment}
                disabled={selectedOrderIds.length !== 0}
                recordNumber={selectedOrderIds.length}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>

        <OrderTable
          orders={orders}
          onCancel={handleCancelOrder}
          onPrint={handlePrintOrder}
          onEdit={handleEditOrder}
          onAtOriginOffice={handleAtOriginOfficeOrder}
          page={page}
          total={total}
          loading={loading}
          limit={limit}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
            fetchOrders(page);
          }}
          selectedOrderIds={selectedOrderIds}
          setSelectedOrderIds={setSelectedOrderIds}
        />

      </div>

      <ConfirmModal
        title='Xác nhận nhận hàng'
        message='Vui lòng xác nhận rằng bạn đã nhận đơn hàng tại bưu cục để chuyển giao cho đơn vị vận chuyển.'
        open={modalConfirmOpen}
        onOk={confirmAtOriginOfficeOrder}
        onCancel={() => setModalConfirmOpen(false)}
        loading={loading}
      />

      <ConfirmCancelModal
        open={cancelModalOpen}
        onOk={confirmCancelOrder}
        onCancel={() => setCancelModalOpen(false)}
        loading={loading}
      />
    </div>
  );
};

export default ManagerOrderList;