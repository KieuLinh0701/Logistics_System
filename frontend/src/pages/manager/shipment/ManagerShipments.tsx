import React, { useEffect, useRef, useState } from 'react';
import { Col, Form, message, Row, Tag } from 'antd';
import dayjs from 'dayjs';
import SearchFilters from './components/SearchFilters';
import Actions from './components/Actions';
import RequestTable from './components/Table';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Title from 'antd/es/typography/Title';
import { TruckOutlined } from '@ant-design/icons';
import "./ManagerShipments.css"
import ConfirmModal from '../../common/ConfirmModal';
import type { ManagerShipment, ManagerShipmentSearchRequest } from '../../../types/shipment';
import shipmentApi from '../../../api/shipmentApi';
import AddEditShipmentModal from './components/AddEditShipmentModal';

const ManagerShipments: React.FC = () => {
  const latestRequestRef = useRef(0);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [shipments, setShipments] = useState<ManagerShipment[] | []>([]);
  const [loading, setLoading] = useState(false)

  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);

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
      const requestId = ++latestRequestRef.current;
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
      if (requestId !== latestRequestRef.current) return;
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setShipments(list);
        setTotal(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách chuyến hàng");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách chuyến hàng");
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

  const handleCancelShipment = async (id: number) => {
    setLoading(true);
    try {
      const result = await shipmentApi.cancelManagerShipment(id);

      if (result.success && result.data) {
        message.success(result.message || "Hủy chuyến hàng thành công");
        fetchShipments(page);
        if (detailModalVisible && selectedShipment) {
          setSelectedShipment({
            ...selectedShipment,
            status: 'CANCELLED'
          });
        }
      } else {
        message.error(result.message || "Hủy chuyến hàng thất bại");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi hủy chuyến hàng:");
    } finally {
      setModalConfirmOpen(false);
      setLoading(false);
    }
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

  const handleEditShipment = async (shipment: ManagerShipment) => {
    setIsModalOpen(true);
    setModalMode('edit');
    setShipment(shipment);
  };

  const handleViewDetailShipment = async (shipment: ManagerShipment) => {
    navigate(`/shipments/${shipment.id}/orders`)
  };

  const handleEditSuccessShipment = async () => {
    if (modalMode == "create") {
      await fetchShipments(1);
      setPage(1);
    } else {
      await fetchShipments(page);
    }
  };

  useEffect(() => {
    updateURL();
    fetchShipments(page);
  }, [page, limit, searchText, filterSort, filterStatus, dateRange, filterType]);

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
          onCancel={showConfirmCancel}
          onDetail={handleViewDetailShipment}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
          }}
        />
      </div>

      <AddEditShipmentModal
        open={isModalOpen}
        mode={modalMode}
        shipment={shipment}
        onSuccess={handleEditSuccessShipment}
        onCancel={() => setIsModalOpen(false)}
      />

      <ConfirmModal
        title='Xác nhận hủy chuyến hàng'
        message='Bạn có chắc chắn muốn hủy chuyến hàng này không?'
        open={modalConfirmOpen}
        onOk={confirmCancelShipment}
        onCancel={() => setModalConfirmOpen(false)}
        loading={loading}
      />
    </div>
  );
};

export default ManagerShipments;