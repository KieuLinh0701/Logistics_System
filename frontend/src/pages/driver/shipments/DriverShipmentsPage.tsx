import React, {useEffect, useState} from "react";
import {message, Row, Typography} from "antd";
import shipmentApi from "../../../api/shipmentApi";
import type {DriverShipment} from "../../../types/shipment";
import ShipmentsTable from "./components/ShipmentsTable";
import ShipmentsToolbar from "./components/ShipmentsToolbar";

const { Title } = Typography;

const DriverShipmentsPage: React.FC = () => {
  const [shipments, setShipments] = useState<DriverShipment[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0 });

  useEffect(() => {
    loadShipments();
  }, [pagination.page, pagination.limit]);

  const loadShipments = async () => {
    try {
      setLoading(true);
      const res = await shipmentApi.getDriverShipments({
        page: pagination.page,
        limit: pagination.limit,
      });
      setShipments(Array.isArray(res.shipments) ? res.shipments : []);
      setPagination(res.pagination || pagination);
    } catch {
      message.error("Không tải được danh sách chuyến hàng");
    } finally {
      setLoading(false);
    }
  };

  const handleStartShipment = async (shipmentId: number) => {
    try {
      const res = await shipmentApi.startShipment(shipmentId);
      if (res && !res.success) {
        message.error(res.message || "Không thể bắt đầu chuyến");
        return;
      }
      message.success("Đã bắt đầu vận chuyển");
      loadShipments();
    } catch (e: any) {
      message.error(e?.message || "Lỗi khi bắt đầu vận chuyển");
    }
  };

  const handleFinishShipment = async (shipmentId: number, status: "COMPLETED" | "CANCELLED") => {
    try {
      await shipmentApi.finishShipment({ shipmentId, status });
      message.success(status === "COMPLETED" ? "Đã hoàn tất chuyến hàng" : "Đã hủy chuyến hàng");
      loadShipments();
    } catch (e: any) {
      message.error(e?.message || "Lỗi khi hoàn tất chuyến hàng");
    }
  };

  const handlePaginationChange = (page: number, limit: number) => {
    setPagination((prev) => ({ ...prev, page, limit }));
  };

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <Row className="list-page-header" justify="space-between" align="middle">
          <Title level={3} className="list-page-title-main">
            Quản lý chuyến vận chuyển
          </Title>
          <ShipmentsToolbar onReload={loadShipments} />
        </Row>

        <div className="list-page-table">
          <ShipmentsTable
            shipments={shipments}
            loading={loading}
            pagination={pagination}
            onPaginationChange={handlePaginationChange}
            onStart={handleStartShipment}
            onFinish={handleFinishShipment}
          />
        </div>
      </div>
    </div>
  );
};

export default DriverShipmentsPage;
