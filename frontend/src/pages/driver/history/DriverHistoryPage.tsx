import React, {useEffect, useState} from "react";
import {message, Row, Typography} from "antd";
import shipmentApi from "../../../api/shipmentApi";
import type {DriverShipment} from "../../../types/shipment";
import HistoryTable from "./components/HistoryTable";
import HistoryToolbar from "./components/HistoryToolbar";

const { Title } = Typography;

const DriverHistoryPage: React.FC = () => {
  const [shipments, setShipments] = useState<DriverShipment[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0 });

  useEffect(() => {
    loadHistory();
  }, [pagination.page, pagination.limit]);

  const loadHistory = async () => {
    try {
      setLoading(true);
      const res = await shipmentApi.getDriverHistory({
        page: pagination.page,
        limit: pagination.limit,
      });
      setShipments(Array.isArray(res.shipments) ? res.shipments : []);
      setPagination(res.pagination || pagination);
    } catch {
      message.error("Không tải được lịch sử vận chuyển");
    } finally {
      setLoading(false);
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
            Lịch sử vận chuyển
          </Title>
          <HistoryToolbar onReload={loadHistory} />
        </Row>

        <div className="list-page-table">
          <HistoryTable
            shipments={shipments}
            loading={loading}
            pagination={pagination}
            onPaginationChange={handlePaginationChange}
          />
        </div>
      </div>
    </div>
  );
};

export default DriverHistoryPage;
