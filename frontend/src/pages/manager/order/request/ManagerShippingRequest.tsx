import React, { useEffect, useRef, useState } from 'react';
import { Col, Form, message, Row, Tag } from 'antd';
import dayjs from 'dayjs';
import SearchFilters from './components/SearchFilters';
import ProcessingModal from './components/ProcessingModal';
import RequestTable from './components/Table';
import DetailModal from './components/DetailModal';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Title from 'antd/es/typography/Title';
import type { ManagerShippingRequestSearchRequest, ShippingRequest } from '../../../../types/shippingRequest';
import { HistoryOutlined } from '@ant-design/icons';
import shippingRequestApi from '../../../../api/shippingRequestApi';
import "./ManagerShippingRequest.css"
import ConfirmModal from '../../../common/ConfirmModal';

const ManagerShippingRequests: React.FC = () => {
  const navigate = useNavigate();
  const latestRequestRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();

  const [requests, setRequests] = useState<ShippingRequest[] | []>([]);
  const [loading, setLoading] = useState(false)

  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);

  const [hover, setHover] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [newRequest, setNewRequest] = useState<Partial<ShippingRequest>>({});

  const [searchText, setSearchText] = useState('');
  const [filterRrequestType, setFilterRequestType] = useState("ALL");
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [filterSort, setFilterSort] = useState('NEWEST');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [modalConfirmOpen, setModalConfirmOpen] = useState(false);


  const [form] = Form.useForm();

  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<ShippingRequest | null>(null);

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
    if (filterRrequestType !== "ALL") params.type = filterRrequestType.toLowerCase();
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
    const st = searchParams.get("status")?.toLocaleUpperCase();
    const type = searchParams.get("type")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    setPage(pageParam);
    if (s) setSearchText(s);
    if (st) setFilterStatus(st);
    if (type) setFilterRequestType(type);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const fetchRequests = async (currentPage = page) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;
      const param: ManagerShippingRequestSearchRequest = {
        page: currentPage,
        limit: limit,
        search: searchText,
        type: filterRrequestType !== "ALL" ? filterRrequestType : undefined,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        sort: filterSort,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await shippingRequestApi.listManagerShippingRequests(param);
      if (requestId !== latestRequestRef.current) return;
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setRequests(list);
        setTotal(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách yêu cầu");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách yêu cầu");
    } finally {
      setLoading(false);
    }
  };

  const confirmCancelShippingRequest = () => {
    if (!selectedRequest) return;

    handleCancelShippingRequest(selectedRequest.id);
  }

  const handleCancelShippingRequest = async (requestId: number) => {
    setLoading(true);
    try {
      const result = await shippingRequestApi.cancelUserShippingRequest(requestId);

      if (result.success && result.data) {
        message.success(result.message || "Hủy yêu cầu thành công");
        fetchRequests(page);
        if (detailModalVisible && selectedRequest) {
          setSelectedRequest({
            ...selectedRequest,
            status: 'CANCELLED'
          });
        }
      } else {
        message.error(result.message || "Hủy yêu cầu thất bại");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi hủy yêu cầu:");
    } finally {
      setModalConfirmOpen(false);
      setLoading(false);
    }
  };

  // Handler mở edit từ detail modal
  const handleEditFromDetail = (request: ShippingRequest) => {
    setModalMode('edit');
    setNewRequest(request);
    setIsModalOpen(true);

    form.setFieldsValue({
      id: request.id,
      trackingNumber: request.orderTrackingNumber,
      requestType: request.requestType,
      requestContent: request.requestContent,
    });
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
      case 'requestType':
        setFilterRequestType(value);
        break;
    }
    setPage(1);
  };

  const handleClearFilters = () => {
    setSearchText('');
    setFilterSort('ALL');
    setFilterStatus('ALL');
    setFilterSort('NEWEST');
    setDateRange(null);
    setPage(1);
    setFilterRequestType('ALL');
  };

  const handleProcessShippingRequest = async (requestId: number) => {
    try {
      setLoading(true);

      const result = await shippingRequestApi.getManagerShippingRequestById(requestId);
      if (result.success && result.data) {
        setNewRequest(result.data);
        setModalMode('edit');
        setIsModalOpen(true);
        form.setFieldsValue(result.data);
      } else {
        message.error(result.message || "Lỗi khi lấy yêu cầu để chỉnh sửa");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy yêu cầu để chỉnh sửa");
      setSelectedRequest(null);
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetailShippingRequest = async (requestId: number) => {
    try {
      setLoading(true);

      const result = await shippingRequestApi.getManagerShippingRequestById(requestId);
      if (result.success && result.data) {
        setSelectedRequest(result.data);
        setDetailModalVisible(true);
      } else {
        setSelectedRequest(null);
        message.error(result.message || "Lỗi khi lấy chi tiết yêu cầu");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy chi tiết yêu cầu");
      setSelectedRequest(null);
    } finally {
      setLoading(false);
    }
  };

  const handleViewOrderDetail = (trackingNumber: string) => {
    navigate(`/orders/tracking/${trackingNumber}`);
  };

  const handleEditSuccessShippingRequest = async () => {
    await fetchRequests(page);

    if (detailModalVisible && selectedRequest) {
      try {
        const result = await shippingRequestApi.getManagerShippingRequestById(selectedRequest.id);
        if (result.success && result.data) {
          setSelectedRequest(result.data);
        } else {
          message.error(result.message || "Cập nhật chi tiết thất bại");
        }
      } catch (error: any) {
        message.error(error.message || "Cập nhật chi tiết thất bại");
      }
    }

    setIsModalOpen(false);
  };

  const handleCloseDetail = () => {
    setDetailModalVisible(false);
    setSelectedRequest(null);
  };

  useEffect(() => {
    updateURL();
    fetchRequests(page);
  }, [page, limit, searchText, filterSort, filterStatus, dateRange, filterRrequestType]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          filterRrequestType={filterRrequestType}
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
              <HistoryOutlined className="title-icon" />
              Danh sách yêu cầu
            </Title>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} yêu cầu</Tag>

        <RequestTable
          data={requests}
          currentPage={page}
          pageSize={limit}
          total={total}
          onProcess={handleProcessShippingRequest}
          onDetail={handleViewDetailShippingRequest}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
          }}
        />
      </div>

      <ProcessingModal
        open={isModalOpen}
        request={newRequest}
        onSuccess={handleEditSuccessShippingRequest}
        onCancel={() => setIsModalOpen(false)}
      />


      <DetailModal
        open={detailModalVisible}
        request={selectedRequest}
        loading={loading}
        onClose={handleCloseDetail}
        onEdit={handleEditFromDetail}
        onViewOrderDetail={handleViewOrderDetail}
      />

      <ConfirmModal
        title='Xác nhận hủy yêu cầu'
        message='Bạn có chắc chắn muốn hủy yêu cầu này không?'
        open={modalConfirmOpen}
        onOk={confirmCancelShippingRequest}
        onCancel={() => setModalConfirmOpen(false)}
        loading={loading}
      />
    </div>
  );
};

export default ManagerShippingRequests;