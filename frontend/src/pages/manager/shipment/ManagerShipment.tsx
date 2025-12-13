import React, { useEffect, useState } from 'react';
import { Col, Form, message, Row, Tag } from 'antd';
import dayjs from 'dayjs';
import SearchFilters from './components/SearchFilters';
import Actions from './components/Actions';
import RequestTable from './components/Table';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Title from 'antd/es/typography/Title';
import { TruckOutlined } from '@ant-design/icons';
import "./ManagerShipment.css"
import ConfirmModal from '../../common/ConfirmModal';
import type { ManagerOrderShipment, ManagerOrderShipmentSearchRequest, ManagerShipment, ManagerShipmentSearchRequest } from '../../../types/shipment';
import shipmentApi from '../../../api/shipmentApi';
import OrdersModal from './components/OrdersModal';

const ManagerShipment: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [shipments, setShipments] = useState<ManagerShipment[] | []>([]);
  const [loading, setLoading] = useState(false)

  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);

  const [orders, setOrders] = useState<ManagerOrderShipment[] | []>([]);
  const [totalOrder, setTotalOrder] = useState(0)
  const [pageOrder, setPageOrder] = useState(1);
  const [searchTextOrder, setSearchTextOrder] = useState('');

  const [hover, setHover] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [shipment, setShipment] = useState<Partial<ManagerShipment>>({});
  const [detailModalVisible, setDetailModalVisible] = useState(false);

  const [searchText, setSearchText] = useState('');
  const [filterType, setFilterType] = useState("ALL");
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [filterSort, setFilterSort] = useState('NEWEST');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [modalConfirmOpen, setModalConfirmOpen] = useState(false);

  const [form] = Form.useForm();

  const [selectedShipment, setSelectedShipment] = useState<ManagerShipment | null>(null);
  const [selectedShipmentId, setSelectedShipmentId] = useState<number | null>(null);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterType !== "ALL") params.type = filterType.toLowerCase();
    params.sort = filterSort.toLowerCase();
    if (page) params.page = page;

    if (dateRange) {
      params.start = dateRange[0].format("YYYY-MM-DD");
      params.end = dateRange[1].format("YYYY-MM-DD");
    }

    setSearchParams(params, { replace: true });
  };

  useEffect(() => {
    const s = searchParams.get("search");
    const st = searchParams.get("status")?.toLocaleUpperCase();
    const type = searchParams.get("type")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    if (s) setSearchText(s);
    if (st) setFilterStatus(st);
    if (type) setFilterType(type);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetchShipments = async (currentPage = page) => {
    try {
      setLoading(true);
      const param: ManagerShipmentSearchRequest = {
        page: currentPage,
        limit: limit,
        search: searchText,
        type: filterType !== "ALL" ? filterType : undefined,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        sort: filterSort,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await shipmentApi.listManagerShipments(param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setShipments(list);
        setTotal(result.data.pagination?.total || 0);
        setPage(page);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách chuyến hàng");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách chuyến hàng");
      console.error("Error fetching shipments:", error);
    } finally {
      setLoading(false);
    }
  };

  const showConfirmCancel = (request: ManagerShipment) => {
    setSelectedShipment(request);
    setModalConfirmOpen(true);
  };

  const confirmCancelShipment = () => {
    if (!selectedShipment) return;

    handleCancelShipment(selectedShipment.id);
  }

  const handleCancelShipment = async (requestId: number) => {
    // setLoading(true);
    // try {
    //   const result = await shippingRequestApi.cancelUserShipment(requestId);

    //   if (result.success && result.data) {
    //     message.success(result.message || "Hủy yêu cầu thành công");
    //     fetchShipments(page);
    //     if (detailModalVisible && selectedShipment) {
    //       setSelectedShipment({
    //         ...selectedShipment,
    //         status: 'CANCELLED'
    //       });
    //     }
    //   } else {
    //     message.error(result.message || "Hủy yêu cầu thất bại");
    //   }
    // } catch (error: any) {
    //   console.error("Lỗi khi hủy yêu cầu:", error);
    //   message.error("Lỗi khi hủy yêu cầu:");
    // } finally {
    //   setModalConfirmOpen(false);
    //   setLoading(false);
    // }
  };

  // Handler mở edit từ detail modal
  const handleEditFromDetail = (request: ManagerShipment) => {
    // setModalMode('edit');
    // setShipment(request);
    // setIsModalOpen(true);

    // form.setFieldsValue({
    //   id: request.id,
    //   trackingNumber: request.orderTrackingNumber,
    //   requestType: request.requestType,
    //   requestContent: request.requestContent,
    // });
  };

  const handleFilterChange = (filter: string, value: string) => {
    switch (filter) {
      case 'search':
        setSearchText(value);
        break;
      case 'sort':
        setFilterSort(value);
        break;
      case 'status':
        setFilterStatus(value);
        break;
      case 'type':
        setFilterType(value);
        break;
    }
    setPage(1);
  };

  const handleClearFilters = () => {
    setSearchText('');
    setFilterSort('ALL');
    setFilterStatus('ALL');
    setFilterSort('NEWEST');
    setFilterType('ALL');
    setDateRange(null);
    setPage(1);
    setFilterType('ALL');
  };

  const handleEditShipment = async (requestId: number) => {
    // try {
    //   setLoading(true);

    //   const result = await shippingRequestApi.getUserShipmentByIdForEdit(requestId);
    //   if (result.success && result.data) {
    //     setShipment(result.data);
    //     setModalMode('edit');
    //     setIsModalOpen(true);
    //     form.setFieldsValue(result.data);
    //   } else {
    //     message.error(result.message || "Lỗi khi lấy yêu cầu để chỉnh sửa");
    //   }
    // } catch (error) {
    //   setSelectedShipment(null);
    //   console.error("Lỗi khi lấy yêu cầu để chỉnh sửa:", error);
    // } finally {
    //   setLoading(false);
    // }
  };

  const handleDeleteShipment = async (requestId: number) => {
  }

  const fetchOrdersShipment = async (currentPage = pageOrder) => {
    if (selectedShipmentId === null) return;
    try {
      setLoading(true);
      const param: ManagerOrderShipmentSearchRequest = {
        page: currentPage,
        limit: limit,
        search: searchTextOrder,
      };
      const result = await shipmentApi.getManagerOrdersByShipmentId(selectedShipmentId, param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setOrders(list);
        setTotalOrder(result.data.pagination?.total || 0);
        setPageOrder(currentPage);
      } else {
        setSelectedShipment(null);
        message.error(result.message || "Lỗi khi lấy danh sách đơn hàng của chuyến hàng");
      }
    } catch (error) {
      setSelectedShipment(null);
      console.error("Lỗi khi lấy danh sách đơn hàng của chuyến hàng:", error);
    } finally {
      setLoading(false);
    }
  }


  const handleViewDetailShipment = async (id: number) => {
    setSelectedShipmentId(id);
    setPageOrder(1);
    setSearchTextOrder("");
    setOrders([]);
    setDetailModalVisible(true);
    await fetchOrdersShipment(1);
  };

  const handleSearch = (value: string) => {
    setSearchTextOrder(value);
    setPageOrder(1);
    if (selectedShipmentId && detailModalVisible) {
      fetchOrdersShipment(1);
    }
  };

  const handlePageChange = (newPage: number) => {
    if (!selectedShipmentId) return;
    fetchOrdersShipment(newPage);
  };

  useEffect(() => {
    if (!selectedShipmentId || !detailModalVisible) return;
    const timeout = setTimeout(() => {
      fetchOrdersShipment(1);
    }, 300);

    return () => clearTimeout(timeout);
  }, [searchTextOrder, selectedShipmentId, detailModalVisible]);

  useEffect(() => {
    if (!selectedShipmentId) return;

    fetchOrdersShipment(1);
  }, [selectedShipmentId]);

  const handleEditSuccessShipment = async () => {
    // await fetchShipments(page);

    // if (detailModalVisible && selectedShipment) {
    //   try {
    //     const result = await shippingRequestApi.getUserShipmentById(selectedShipment.id);
    //     if (result.success && result.data) {
    //       setSelectedShipment(result.data);
    //     } else {
    //       message.error(result.message || "Cập nhật chi tiết thất bại");
    //     }
    //   } catch (error) {
    //     console.error("Lỗi khi load chi tiết:", error);
    //     message.error("Cập nhật chi tiết thất bại");
    //   }
    // }

    // setIsModalOpen(false);
  };

  useEffect(() => {
    updateURL();
    setPage(1);
    fetchShipments(1);
  }, [searchText, filterSort, filterStatus, dateRange, filterType]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          filterType={filterType}
          filterStatus={filterStatus}
          sort={filterSort}
          dateRange={dateRange}
          hover={hover}
          onSearchChange={setSearchText}
          onFilterChange={handleFilterChange}
          onSortChange={setFilterSort}
          onDateRangeChange={setDateRange}
          onClearFilters={handleClearFilters}
          onHoverChange={setHover}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <TruckOutlined className="title-icon" />
              Danh sách chuyến hàng
            </Title>
          </Col>

          <Col>
            <div className="list-page-actions">
              <Actions
                onAddRequest={() => {
                  setIsModalOpen(true);
                  setModalMode('create');
                  setShipment({});
                  form.resetFields();
                }}
              />
            </div>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} chuyến hàng</Tag>

        <RequestTable
          data={shipments}
          currentPage={page}
          pageSize={limit}
          total={total}
          onEdit={handleEditShipment}
          onDelete={handleDeleteShipment}
          onCancel={showConfirmCancel}
          onDetail={handleViewDetailShipment}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
            fetchShipments(page);
          }}
        />
      </div>

      {/* <AddEditModal
        open={isModalOpen}
        mode={modalMode}
        shipment={shipment}
        onSuccess={handleEditSuccessShipment}
        onCancel={() => setIsModalOpen(false)}
      /> */}


      <OrdersModal
        open={detailModalVisible}
        orders={orders}
        page={pageOrder}
        limit={limit}
        searchText={searchTextOrder} 
        total={totalOrder}
        loading={loading}
        onClose={() => {
          setDetailModalVisible(false);
          setSearchTextOrder("");
          setOrders([]);
        }}
        onSearch={handleSearch}
        onPageChange={handlePageChange}
      />

      <ConfirmModal
        title='Xác nhận hủy yêu cầu'
        message='Bạn có chắc chắn muốn hủy yêu cầu này không?'
        open={modalConfirmOpen}
        onOk={confirmCancelShipment}
        onCancel={() => setModalConfirmOpen(false)}
        loading={loading}
      />
    </div>
  );
};

export default ManagerShipment;