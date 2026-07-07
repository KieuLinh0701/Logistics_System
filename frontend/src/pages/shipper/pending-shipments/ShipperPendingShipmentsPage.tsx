import React, {useEffect, useState} from "react";
import {message} from "antd";
import shipmentApi from "../../../api/shipmentApi";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";
import PendingShipmentsTable from "./components/PendingShipmentsTable";
import PendingShipmentsToolbar from "./components/PendingShipmentsToolbar";

const ShipperPendingShipmentsPage: React.FC = () => {
  const [shipments, setShipments] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [fetchKey, setFetchKey] = useState(0);

  const normalizeShipments = (raw: any): any[] => {
    let data = raw;
    if (typeof raw === "string") {
      try {
        data = JSON.parse(raw);
      } catch {
        console.error("[NORMALIZE] Failed to parse JSON string:", raw);
        return [];
      }
    }

    if (Array.isArray(data)) {
      return data;
    }

    if (data && typeof data === 'object' && 'success' in data) {
      if (Array.isArray(data.data)) {
        return data.data;
      }
      if (data.data && typeof data.data === 'object' && 'shipments' in data.data) {
        return data.data.shipments || [];
      }
    }

    const candidates = ['shipments', 'data', 'items', 'content'];
    for (const key of candidates) {
      if (Array.isArray(data?.[key])) {
        return data[key];
      }
    }

    return [];
  };

  const fetchShipments = async () => {
    try {
      setLoading(true);
      const response = await shipmentApi.listShipperActiveShipments();
      const normalized = normalizeShipments(response);
      setShipments(normalized);
    } catch (error) {
      console.error("Error fetching shipper shipments:", error);
      message.error("Lỗi khi tải danh sách chuyến hàng");
      setShipments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShipments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchKey]);

  const handleStartShipment = async (shipmentId: number) => {
    if (!shipmentId) return;
    try {
      setActionLoading(true);
      await shipmentApi.startShipperDeliveryShipment(shipmentId);
      message.success("Đã bắt đầu chuyến giao hàng");
      setFetchKey((k) => k + 1);
    } catch (e: any) {
      const errorMsg = e?.response?.data?.message || e?.message || "Không thể bắt đầu chuyến";
      message.error(errorMsg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleFinishShipment = async (shipmentId: number) => {
    if (!shipmentId) return;
    try {
      setActionLoading(true);
      await shipmentApi.finishShipperDeliveryShipment(shipmentId);
      message.success("Đã kết thúc chuyến giao hàng");
      setFetchKey((k) => k + 1);
    } catch (e: any) {
      const errorMsg = e?.response?.data?.message || e?.message || "Không thể kết thúc chuyến";
      message.error(errorMsg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRefresh = () => {
    setFetchKey((k) => k + 1);
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <PendingShipmentsToolbar onRefresh={handleRefresh} />

        <div className="list-page-header shipper-page-header">
          <h3 className="list-page-title-main">Chuyến hàng cần giao</h3>
          <div className="list-page-tag">Kết quả: {shipments.length} chuyến</div>
        </div>

        <div className="list-page-table shipper-page-table">
          <PendingShipmentsTable
            shipments={shipments}
            loading={loading}
            actionLoading={actionLoading}
            onStartShipment={handleStartShipment}
            onFinishShipment={handleFinishShipment}
          />
        </div>
      </div>
    </div>
  );
};

export default ShipperPendingShipmentsPage;
