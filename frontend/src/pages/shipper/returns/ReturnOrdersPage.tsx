import React, { useEffect, useState } from "react";
import { message } from "antd";
import type { ShipperOrder } from "../../../api/orderApi";
import orderApi from "../../../api/orderApi";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";
import ReturnOrdersTable from "./components/ReturnOrdersTable";
import ReturnOrdersToolbar from "./components/ReturnOrdersToolbar";

interface FilterParams {
  status?: string;
  search?: string;
}

const ReturnOrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<ShipperOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<FilterParams>({});
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagination.current, pagination.pageSize, filters]);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const params = {
        page: pagination.current,
        limit: pagination.pageSize,
        status: filters.status,
        search: filters.search,
      };
      const res = await orderApi.getShipperReturnOrders(params);
      setOrders((res.orders || []) as ShipperOrder[]);
      setPagination((prev) => ({ ...prev, total: res.pagination?.total || 0 }));
    } catch (error) {
      console.error("Error fetching return orders:", error);
      message.error("Lỗi khi tải danh sách đơn hoàn trả");
    } finally {
      setLoading(false);
    }
  };

  const handleSearchChange = (value: string | undefined) => {
    setFilters((f) => ({ ...f, search: value }));
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handleStatusChange = (value: string | undefined) => {
    setFilters((f) => ({ ...f, status: value }));
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handleRefresh = () => {
    setFilters({});
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handlePageChange = (page: number, pageSize: number) => {
    setPagination((prev) => ({ ...prev, current: page, pageSize: pageSize || 10 }));
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <ReturnOrdersToolbar
          search={filters.search}
          status={filters.status}
          onSearchChange={handleSearchChange}
          onStatusChange={handleStatusChange}
          onRefresh={handleRefresh}
        />

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Đơn hàng hoàn trả</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {pagination.total} đơn</div>
            </div>
          </div>
        </div>

        <div className="list-page-table shipper-page-table">
          <ReturnOrdersTable
            orders={orders}
            loading={loading}
            pagination={pagination}
            onPageChange={handlePageChange}
            from="/shipper/my-orders?tab=return"
          />
        </div>
      </div>
    </div>
  );
};

export default ReturnOrdersPage;
