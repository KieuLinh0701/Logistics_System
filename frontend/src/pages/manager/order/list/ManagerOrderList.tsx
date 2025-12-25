import { useEffect, useRef, useState } from "react";
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
import ShipmentModal from "./components/ShipmentModal";
import type { ManagerOrderShipment, ManagerShipment } from "../../../../types/shipment";
import type { SearchRequest } from "../../../../types/request";
import shipmentApi from "../../../../api/shipmentApi";
import shipmentOrderApi from "../../../../api/shipmentOrderApi";
import type { BulkResponse } from "../../../../types/response";
import BulkResult from "./components/BulkResult";

const ManagerOrderList = () => {
  const navigate = useNavigate();
  const latestRequestRef = useRef(0);
  const selectAllRequestRef = useRef(0);
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
  const [filterPickupType, setFilterPickupType] = useState("ALL");
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [modalConfirmOpen, setModalConfirmOpen] = useState(false);
  const [modalConfirmOpenAddOrders, setModalConfirmOpenAddOrders] = useState(false);
  const [selectedOrderIds, setSelectedOrderIds] = useState<number[] | []>([]);

  const [orderId, setOrderId] = useState<number | null>(null);

  const [isShipmentModalOpen, setIsShipmentModalOpen] = useState(false);
  const [shipments, setShipments] = useState<ManagerShipment[] | []>([]);
  const [limitShipment, setLimitShipment] = useState(10);
  const [pageShipment, setPageShipment] = useState(1);
  const [totalShipment, setTotalShipment] = useState<number>(0);
  const [loadingShipment, setLoadingShipment] = useState(false);
  const [hover, setHover] = useState(false);
  const [searchShipment, setSearchShipment] = useState("");
  const [filterTypeShipment, setFilterTypeShipment] = useState("ALL");
  const [filterSortShipment, setFilterSortShipment] = useState("NEWEST");
  const [selectedShipment, setSelectedShipment] = useState<ManagerShipment | null>(null);

  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkResult, setBulkResult] = useState<BulkResponse<ManagerOrderShipment>>();

  const updateURL = () => {
    const params: any = {};

    if (search) params.search = search;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterPayer !== "ALL") params.payer = filterPayer.toLowerCase();
    if (filterPaymentStatus !== "ALL") params.payment = filterPaymentStatus.toLowerCase();
    if (filterServiceType !== null) params.service = filterServiceType;
    if (filterCOD !== "ALL") params.cod = filterCOD.toLowerCase();
    if (filterPickupType !== "ALL") params.pickupType = filterPickupType.toLowerCase();
    params.sort = filterSort.toLowerCase();
    if (page) params.page = page;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const pageParam = Number(searchParams.get("page")) || 1;
    const s = searchParams.get("search");
    const status = searchParams.get("status")?.toLocaleUpperCase();
    const payer = searchParams.get("payer")?.toLocaleUpperCase();
    const payment = searchParams.get("payment")?.toLocaleUpperCase();
    const service = searchParams.get("service");
    const cod = searchParams.get("cod")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");
    const pick = searchParams.get("pickupType")?.toLocaleUpperCase();

    setPage(pageParam);
    if (s) setSearch(s);
    if (status) setFilterStatus(status);
    if (payer) setFilterPayer(payer);
    if (payment) setFilterPaymentStatus(payment);

    if (service) setFilterServiceType(Number(service));

    if (cod) setFilterCOD(cod);
    if (sort) setFilterSort(sort);
    if (pick) setFilterPickupType(pick);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  // --- Fetch Orders ---
  const fetchOrders = async (currentPage = page) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;
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
        pickupType: filterPickupType !== "ALL" ? filterPickupType : undefined,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await orderApi.listManagerOrders(param);

      if (requestId !== latestRequestRef.current) return;

      if (result.success && result.data) {
        const orderList = result.data?.list || [];
        setOrders(orderList);
        setTotal(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách đơn hàng");
      }
    } catch (error: any) {
      console.error(error.message || "Error fetching orders:", error);
    } finally {
      setLoading(false);
    }
  };

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
      message.error(error.message || "Lỗi server khi hủy đơn hàng");
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

  const handleSelectAllFiltered = async (select: boolean) => {
    if (!select) {
      setSelectedOrderIds([]);
      return;
    }
    const requestId = ++selectAllRequestRef.current;
    try {
      const param: ManagerOrderSearchRequest = {
        page: 1,
        limit: limit,
        search: search || undefined,
        payer: filterPayer !== "ALL" ? filterPayer : undefined,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        serviceTypeId: filterServiceType !== null ? filterServiceType : undefined,
        paymentStatus: filterPaymentStatus !== "ALL" ? filterPaymentStatus : undefined,
        cod: filterCOD !== "ALL" ? filterCOD : undefined,
        sort: filterSort,
        pickupType: filterPickupType !== "ALL" ? filterPickupType : undefined,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await orderApi.getAllManagerOrderIds(param);

      if (requestId !== selectAllRequestRef.current) return;

      if (result.success && result.data) {
        setSelectedOrderIds(result.data);
      } else {
        message.error(result.message || "Lấy toàn bộ ID thất bại");
      }
    } catch (err: any) {
      message.error(err.message || "Lỗi server khi lấy toàn bộ ID");
    }
  };

  useEffect(() => {
    setSelectedOrderIds([]);
  }, [orders]);

  const handleAddShipment = () => {
    setIsShipmentModalOpen(true);
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
        message.error(result.message || "Có lỗi khi xác nhận bàn giao đơn hàng cho bưu cục xuất phát!");
      }
    } catch (err: any) {
      message.error(err.message || "Có lỗi khi xác nhận bàn giao đơn hàng cho bưu cục xuất phát!");
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
    setFilterPickupType("ALL");
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
      case 'pickupType':
        setFilterPickupType(value);
        break;
    }
    setPage(1);
  };

  useEffect(() => {
    const fetchServiceTypes = async () => {
      try {
        setLoading(true);
        const response = await serviceTypeApi.getActiveServiceTypes();
        if (response.success && response.data) {
          setServiceTypes(response.data);
        }
      } catch (error: any) {
        message.error(error.message || "Lỗi khi lấy danh sách dịch vụ");
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
    fetchServiceTypes();
  }, []);

  useEffect(() => {

    updateURL();
    fetchOrders(page);
  }, [page, limit, search, filterStatus, filterServiceType, filterPayer, filterPaymentStatus, filterCOD, dateRange, filterSort, filterPickupType]);

  // Shipment
  const fetchShipments = async (currentPage = pageShipment) => {
    try {
      setLoadingShipment(true);
      const requestId = ++latestRequestRef.current;
      const param: SearchRequest = {
        page: currentPage,
        limit: limit,
        search: searchShipment,
        type: filterTypeShipment !== null ? filterTypeShipment : undefined,
        sort: filterSortShipment,
      };

      const result = await shipmentApi.listManagerPendingShipments(param);

      if (requestId !== latestRequestRef.current) return;

      if (result.success && result.data) {
        const shipmentList = result.data?.list || [];
        setShipments(shipmentList);
        setTotalShipment(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách chuyến hàng");
      }
    } catch (error: any) {
      console.error(error.message || "Có lỗi xảy ra. Vui lòng thử lại sau");
    } finally {
      setLoadingShipment(false);
    }
  };

  const handleFilterChangeShipment = (filter: string, value: string) => {
    switch (filter) {
      case 'type':
        setFilterTypeShipment(value);
        break;
      case 'sort':
        setFilterSortShipment(value);
        break;
    }
    setPage(1);
  };

  const handleClearFiltersShipment = () => {
    setSearchShipment("");
    setFilterTypeShipment("ALL");
    setFilterSortShipment("NEWEST");
    setPageShipment(1);
  };

  const handleConfirmAddOrdersInShipment = (shipment: ManagerShipment) => {
    if (!selectedOrderIds.length) {
      message.warning("Vui lòng chọn ít nhất một đơn hàng");
      return;
    }

    setSelectedShipment(shipment);
    setModalConfirmOpenAddOrders(true);
  };

  const confirmAddOrdersInShipment = async () => {
    if (!selectedShipment || !selectedOrderIds.length) {
      message.error("Bạn chưa chọn chuyến hàng hoặc đơn hàng");
      return;
    }

    setLoading(true);
    try {
      const result = await shipmentOrderApi.saveManagerShipmentOrders(
        selectedShipment.id,
        [],
        selectedOrderIds
      );

      if (result.success) {
        message.success(result.message || "Cập nhật thành công");
        selectAllRequestRef.current++;
        setSelectedOrderIds([]);
        setSelectedShipment(null);

        console.log("orderIds", setSelectedOrderIds);
      } else {
        message.warning(result.message || "Một số đơn hàng không thể thêm");
        if (result.results) {
          setBulkResult(result as any);
          setBulkModalOpen(true);
        }
      }
    } catch (error: any) {
      message.error(error.message || "Cập nhật thất bại");
    } finally {
      setLoading(false);
      setModalConfirmOpenAddOrders(false);
      setIsShipmentModalOpen(false);
    }
  };

  const handleConfirm = (id: number) => {
    setOrderId(id);
    setConfirmModalOpen(true);
  };

  const confirmConfirmOrder = async () => {
    if (!orderId) return;

    setConfirmModalOpen(false);

    try {
      setLoading(true);

      const result = await orderApi.confirmManagerOrder(orderId);

      if (result.success) {
        message.success(result.message || "Xác nhận đơn hàng thành công.");
        fetchOrders(page);
      } else {
        message.error(result.message || "Có lỗi khi xác nhận đơn hàng!");
      }
    } catch (err: any) {
      message.error(err.message || "Có lỗi khi xác nhận đơn hàng!");
    } finally {
      setLoading(false);
      setOrderId(null);
    }
  };

  useEffect(() => {
    if (!isShipmentModalOpen) return;
    fetchShipments(page);
  }, [pageShipment, limitShipment, searchShipment, filterTypeShipment, isShipmentModalOpen]);

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
          filters={{ status: filterStatus, payer: filterPayer, paymentStatus: filterPaymentStatus, serviceType: filterServiceType, cod: filterCOD, sort: filterSort, pickupType: filterPickupType }}
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
          onConfirm={handleConfirm}
          onAtOriginOffice={handleAtOriginOfficeOrder}
          page={page}
          total={total}
          loading={loading}
          limit={limit}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
          }}
          selectedOrderIds={selectedOrderIds}
          setSelectedOrderIds={setSelectedOrderIds}
          onSelectAllFiltered={handleSelectAllFiltered}
        />

      </div>

      <ShipmentModal
        open={isShipmentModalOpen}
        data={shipments}
        page={pageShipment}
        limit={limitShipment}
        total={totalShipment}
        onPageChange={(page, size) => {
          setPageShipment(page);
          if (size) setLimitShipment(size);
        }}

        loading={loadingShipment}
        onClose={() => setIsShipmentModalOpen(false)}
        hover={hover}
        searchText={searchShipment}
        onSearch={setSearchShipment}
        filterType={filterTypeShipment}
        sort={filterSortShipment}
        onHoverChange={setHover}
        onFilterChange={handleFilterChangeShipment}
        onSortChange={setFilterSortShipment}
        onClearFilters={handleClearFiltersShipment}
        onConfirm={handleConfirmAddOrdersInShipment}
      />

      <ConfirmModal
        title="Xác nhận thêm đơn hàng vào chuyến"
        message="Vui lòng xác nhận rằng bạn sẽ thêm các đơn hàng đã chọn vào chuyến này."
        open={modalConfirmOpenAddOrders}
        onOk={confirmAddOrdersInShipment}
        onCancel={() => setModalConfirmOpenAddOrders(false)}
        loading={loading}
      />

      <ConfirmModal
        title='Xác nhận nhận hàng'
        message='Bạn có chắc rằng bạn đã nhận đơn hàng này tại bưu cục để chuyển giao cho đơn vị vận chuyển không?'
        open={modalConfirmOpen}
        onOk={confirmAtOriginOfficeOrder}
        onCancel={() => setModalConfirmOpen(false)}
        loading={loading}
      />

      <ConfirmModal
        title="Xác nhận đơn hàng"
        message="Bạn có chắc muốn xác nhận đơn hàng này để bưu cục tiếp nhận và xử lý không?"
        open={confirmModalOpen}
        onOk={confirmConfirmOrder}
        onCancel={() => setConfirmModalOpen(false)}
        loading={loading}
      />

      <ConfirmCancelModal
        open={cancelModalOpen}
        onOk={confirmCancelOrder}
        onCancel={() => setCancelModalOpen(false)}
        loading={loading}
      />

      {bulkResult && (
        <BulkResult
          open={bulkModalOpen}
          results={bulkResult}
          onClose={() => setBulkModalOpen(false)}
        />
      )}
    </div>
  );
};

export default ManagerOrderList;