import React, {useEffect, useState} from "react";
import {message} from "antd";
import type {ShipperOrder} from "../../../api/orderApi";
import orderApi from "../../../api/orderApi";
import {dispatchShipperRouteRefresh} from "../delivery-route/deliveryRouteEvents";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";
import UnassignedOrdersTable from "./components/UnassignedOrdersTable";
import UnassignedOrdersToolbar from "./components/UnassignedOrdersToolbar";

const UnassignedOrdersPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<ShipperOrder[]>([]);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchUnassigned = async (p = page, l = limit) => {
    try {
      setLoading(true);
      const resp = await orderApi.getShipperUnassignedOrders({ page: p, limit: l });
      setData(resp.orders || []);
      setTotal(resp.pagination?.total || 0);
      setPage(resp.pagination?.page || p);
      setLimit(resp.pagination?.limit || l);
    } catch (err) {
      message.error("Lỗi khi tải đơn chưa gán");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnassigned(1, limit);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleClaim = async (orderId: number) => {
    try {
      await orderApi.claimShipperOrder(orderId);
      message.success("Đã nhận đơn");
      dispatchShipperRouteRefresh();
      fetchUnassigned(page, limit);
    } catch (err: any) {
      message.error(err?.message || "Lỗi khi nhận đơn");
    }
  };

  const handlePageChange = (p: number, l: number) => {
    setPage(p);
    setLimit(l);
    fetchUnassigned(p, l);
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <UnassignedOrdersToolbar onRefresh={() => fetchUnassigned(page, limit)} />

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Đơn chưa gán</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {total} đơn</div>
            </div>
          </div>
        </div>

        <div className="list-page-table shipper-page-table">
          <UnassignedOrdersTable
            orders={data}
            loading={loading}
            pagination={{ current: page, pageSize: limit, total }}
            onPageChange={handlePageChange}
            onClaim={handleClaim}
          />
        </div>
      </div>
    </div>
  );
};

export default UnassignedOrdersPage;
