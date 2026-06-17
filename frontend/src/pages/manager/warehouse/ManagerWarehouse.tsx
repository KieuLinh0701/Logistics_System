import React, { useEffect, useState } from "react";
import { message, Row, Typography } from "antd";
import WarehouseTable from "./components/WarehouseTable";
import SearchFilters from "./components/SearchFilters";
import type { ServiceType } from "../../../types/serviceType";
import serviceTypeApi from "../../../api/serviceTypeApi";
import { DatabaseOutlined } from "@ant-design/icons";
import type { OrderHistory } from "../../../types/orderHistory";
import "./ManagerWarehouse.css"

const { Title } = Typography;

const ManagerWarehouse: React.FC = () => {
  const [serviceTypes, setServiceTypes] = useState<ServiceType[] | []>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState("");
  const [filterServiceType, setFilterServiceType] = useState<number | string>('ALL');
  const [filterSort, setFilterSort] = useState('NEWEST');
  const [hover, setHover] = useState(false);
  const [activeTab, setActiveTab] = useState<string>('1');
  const [page, setPage] = useState(1);
  const limit = 10;
  const [total] = useState(0);

  const [orders] = useState<OrderHistory[]>([]);

  // Fetch orders based on tab and page
  const fetchOrdersByTab = async (_tabKey: string, _pageNumber = 1) => {
    setLoading(true);
    try {
      // Logic for future API integration
    } catch (error: any) {
      message.error(error.message || "Lỗi khi lấy danh sách đơn hàng");
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    setPage(1);
    fetchOrdersByTab(key, 1);
  };

  const handlePageChange = (newPage: number) => {
    fetchOrdersByTab(activeTab, newPage);
  };

  const handleFilterChange = (filter: string, value: string | number) => {
    if (filter === 'serviceType') setFilterServiceType(value);
     if (filter === 'sort') setFilterSort(value.toLocaleString);
  };

  const handleClearFilters = () => {
    setSearchText('');
    setFilterServiceType('ALL');
    setFilterSort('NEWEST');
  };

  useEffect(() => {
    const fetchServiceTypes = async () => {
      try {
        setLoading(true);
        const results = await serviceTypeApi.getActiveServiceTypes();
        if (results.success && results.data) {
          setServiceTypes(results.data)
        } else {
          message.error(results.message || "Lỗi khi lấy dịch vụ")
        }
      } catch (error: any) {
        message.error(error.message || "Lỗi khi lấy dịch vụ");
      } finally {
        setLoading(false);
      }
    };
    fetchServiceTypes();
  }, []);

  useEffect(() => {
    fetchOrdersByTab(activeTab, 1);
  }, [searchText, filterServiceType, filterSort]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <SearchFilters
          searchText={searchText}
          filterServiceType={filterServiceType}
          sort={filterSort}
          hover={hover}
          serviceTypes={serviceTypes || []}
          onSearchChange={setSearchText}
          onFilterChange={handleFilterChange}
          onSortChange={setFilterSort}
          onClearFilters={handleClearFilters}
          onHoverChange={setHover}
        />

        <Row className="list-page-header" justify="space-between" align="middle">
          <Title level={3} className="list-page-title-main">
            <DatabaseOutlined className="title-icon" />
            Đơn nhập - xuất kho
          </Title>
        </Row>

        <WarehouseTable
          orders={orders}
          loading={loading}
          activeTab={activeTab}
          onTabChange={handleTabChange}
          page={page}
          limit={limit}
          total={total}
          onPageChange={handlePageChange}
        />

      </div>
    </div>
  );
};

export default ManagerWarehouse;