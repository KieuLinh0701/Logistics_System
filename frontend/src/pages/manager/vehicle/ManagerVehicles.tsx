import React, { useEffect, useRef, useState } from 'react';
import { Col, Form, message, Row, Tag } from 'antd';
import dayjs from 'dayjs';
import SearchFilters from './components/SearchFilters';
import VehicleTable from './components/VehicleTable';
import EditModal from './components/EditModal';
import type { ManagerVehicleEditRequest, ManagerVehicleSearchRequest, Vehicle } from '../../../types/vehicle';
import Title from 'antd/es/typography/Title';
import { CarOutlined } from '@ant-design/icons';
import vehicleApi from '../../../api/vehicleApi';
import { useSearchParams } from 'react-router-dom';

const ManagerVehicles: React.FC = () => {
  const latestRequestRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();
  const [vehicles, setVehicles] = useState<Vehicle[] | []>([]);
  const [loading, setLoading] = useState(false);

  const [total, setTotal] = useState(0);
  const [limit, setLimit] = useState(10);
  const [page, setPage] = useState(1);

  const [isModalOpen, setIsModalOpen] = useState(false);

  const [selectedVehicle, setSelectedVehicle] = useState<Partial<any>>({});
  const [searchText, setSearchText] = useState('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [filterSort, setFilterSort] = useState('NEWEST');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [hover, setHover] = useState(false);
  const [form] = Form.useForm();

  const updateURL = () => {
    const params: any = {};

    if (searchText) params.search = searchText;
    if (filterType !== "ALL") params.type = filterType.toLowerCase();
    if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
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
    const t = searchParams.get("type")?.toLocaleUpperCase();
    const status = searchParams.get("status")?.toLocaleUpperCase();
    const sort = searchParams.get("sort")?.toLocaleUpperCase();
    const startDate = searchParams.get("start");
    const endDate = searchParams.get("end");

    setPage(pageParam);
    if (s) setSearchText(s);
    if (t) setFilterType(t);
    if (status) setFilterStatus(status);
    if (sort) setFilterSort(sort);

    if (startDate && endDate) {
      setDateRange([
        dayjs(startDate, "YYYY-MM-DD"),
        dayjs(endDate, "YYYY-MM-DD")
      ]);
    }
  }, [searchParams]);

  const handleEditVehicle = async () => {
    try {
      const values = await form.validateFields();

      const param: ManagerVehicleEditRequest = {
        status: values.status,
        description: values.description,
        nextMaintenanceDue: values.nextMaintenanceDue
          ? values.nextMaintenanceDue.format("YYYY-MM-DDTHH:mm:ss")
          : null,
        gpsDeviceId: values.gpsDeviceId,
      };

      const result = await vehicleApi.updateManagerVehicle(selectedVehicle.id, param);

      if (result.success) {
        message.success(result.message || "Cập nhật phương tiện thành công!");
        setIsModalOpen(false);
        fetchVehicles(page);
      } else {
        message.error(result.message || "Có lỗi khi cập nhật phương tiện!");
      }
    } catch (error: any) {
      message.error(error?.message || "Có lỗi khi cập nhật phương tiện!");
    }
  };

  const fetchVehicles = async (currentPage = page) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;
      const param: ManagerVehicleSearchRequest = {
        page: currentPage,
        limit,
        search: searchText,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        type: filterType !== "ALL" ? filterType : undefined,
        sort: filterSort,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
        param.endDate = dateRange[0].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
      }

      const result = await vehicleApi.listManagerVehicles(param);
      if (requestId !== latestRequestRef.current) return;
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setVehicles(list);
        setTotal(result.data.pagination?.total || 0);
      } else {
        message.error(result.message || "Lỗi khi lấy danh sách phương tiện");
      }
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách phương tiện");
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (filter: string, value: string) => {
    switch (filter) {
      case 'sort':
        setFilterSort(value);
        break;
        break;
      case 'type':
        setFilterType(value);
        break;
      case 'status':
        setFilterStatus(value);
        break;
    }
    setPage(1);
  };

  const handleClearFilters = () => {
    setSearchText('');
    setFilterType('ALL');
    setFilterStatus('ALL');
    setFilterSort('NEWEST');
    setDateRange(null);
    setPage(1);
  };

  useEffect(() => {
    updateURL();
    fetchVehicles(page);
  }, [page, limit, searchText, filterType, filterStatus, filterSort, dateRange]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          filterType={filterType}
          filterStatus={filterStatus}
          filterSort={filterSort}
          dateRange={dateRange}
          hover={hover}
          onSearchChange={setSearchText}
          onFilterChange={handleFilterChange}
          onDateRangeChange={setDateRange}
          onClearFilters={handleClearFilters}
          onHoverChange={setHover}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <CarOutlined className="title-icon" />
              Danh sách phương tiện
            </Title>
          </Col>
        </Row>

        <Tag className="list-page-tag">Kết quả trả về: {total} phương tiện</Tag>

        <VehicleTable
          data={vehicles}
          currentPage={page}
          pageSize={limit}
          total={total}
          onEdit={(vehicle) => {
            setSelectedVehicle(vehicle);
            setIsModalOpen(true);
            form.setFieldsValue({
              ...vehicle,
              nextMaintenanceDue: vehicle.nextMaintenanceDue ? dayjs(vehicle.nextMaintenanceDue) : null
            });
          }}
          onPageChange={(page, size) => {
            setPage(page);
            if (size) setLimit(size);
          }}
        />

        <EditModal
          open={isModalOpen}
          vehicle={selectedVehicle}
          onOk={handleEditVehicle}
          onCancel={() => setIsModalOpen(false)}
          loading={loading}
          onVehicleChange={setSelectedVehicle}
          form={form}
        />
      </div>
    </div>
  );
};

export default ManagerVehicles;