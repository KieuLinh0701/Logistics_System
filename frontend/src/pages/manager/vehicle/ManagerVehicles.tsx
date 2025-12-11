import React, { useEffect, useState } from 'react';
import { Col, Form, message, Row, Tag } from 'antd';
import dayjs from 'dayjs';
import SearchFilters from './components/SearchFilters';
import VehicleTable from './components/VehicleTable';
import EditModal from './components/EditModal';
import type { ManagerVehicleEditRequest, ManagerVehicleSearchRequest, Vehicle } from '../../../types/vehicle';
import Title from 'antd/es/typography/Title';
import { CarOutlined } from '@ant-design/icons';
import vehicleApi from '../../../api/vehicleApi';

const ManagerVehicles: React.FC = () => {
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
  const [filterOffice, setFilterOffice] = useState<string>('ALL');
  const [filterSort, setFilterSort] = useState('NEWEST');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [hover, setHover] = useState(false);
  const [form] = Form.useForm();

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
      const param: ManagerVehicleSearchRequest = {
        page: currentPage,
        limit,
        search: searchText,
        status: filterStatus !== "ALL" ? filterStatus : undefined,
        type: filterType !== "ALL" ? filterType : undefined,
        sort: filterSort,
      };
      if (dateRange) {
        param.startDate = dateRange[0].startOf("day").toISOString();
        param.endDate = dateRange[1].endOf("day").toISOString();
      }

      const result = await vehicleApi.listManagerVehicles(param);
      if (result.success && result.data) {
        const list = result.data?.list || [];
        setVehicles(list);
        setTotal(result.data.pagination?.total || 0);
        setPage(page);
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
    setFilterOffice('ALL');
    setFilterSort('NEWEST');
    setDateRange(null);
    setPage(1);
  };

  useEffect(() => {
    setPage(1);
    fetchVehicles(page);
  }, []);

  useEffect(() => {
    setPage(1);
    fetchVehicles(1);
  }, [searchText, filterType, filterStatus, filterOffice, filterSort, dateRange]);

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
            fetchVehicles(page);
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