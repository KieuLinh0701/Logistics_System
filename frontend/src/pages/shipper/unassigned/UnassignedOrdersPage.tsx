import React, {useEffect, useState} from "react";
import {Alert, message} from "antd";
import type {ShipperOrder} from "../../../api/orderApi";
import orderApi from "../../../api/orderApi";
import {dispatchShipperRouteRefresh} from "../delivery-route/deliveryRouteEvents";
import {getCurrentPositionOnce, type CurrentPosition} from "../../../utils/geolocation";
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
  const [recommendationError, setRecommendationError] = useState<string | null>(null);
  const [gps, setGps] = useState<CurrentPosition | null>(null);

  const fetchUnassigned = async (
    p = page,
    l = limit,
    overrideGps: CurrentPosition | null | "auto" = "auto"
  ) => {
    try {
      setLoading(true);
      // Resolve GPS: dùng giá trị override nếu có, ngược lại lấy mới 1 lần.
      let activeGps: CurrentPosition | null = null;
      if (overrideGps === "auto") {
        // Khi user bấm "Làm mới" (overrideGps === "auto") hoặc lần đầu tải,
        // ta luôn thử lấy GPS mới để có vị trí hiện tại.
        activeGps = await getCurrentPositionOnce(4000);
        setGps(activeGps);
      } else {
        activeGps = overrideGps;
        setGps(overrideGps);
      }

      const resp = await orderApi.getShipperUnassignedOrders({
        page: p,
        limit: l,
        latitude: activeGps?.latitude ?? null,
        longitude: activeGps?.longitude ?? null,
      });

      setData(resp.orders || []);
      setTotal(resp.pagination?.total || 0);
      setPage(resp.pagination?.page || p);
      setLimit(resp.pagination?.limit || l);
      setRecommendationError(null);
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
      // Re-fetch: backend sẽ tự động evict cache recommendation của shipper.
      await fetchUnassigned(page, limit, "auto");
    } catch (err: any) {
      const serverMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message;

      if (
        typeof serverMessage === "string" &&
        serverMessage.includes("đang thực hiện một chuyến giao hàng")
      ) {
        message.warning(
          "Bạn đang thực hiện một chuyến giao hàng. Vui lòng kết thúc chuyến hiện tại trước khi nhận thêm đơn tại bưu cục."
        );
        return;
      }

      message.error(serverMessage || "Lỗi khi nhận đơn");
    }
  };

  const handlePageChange = (p: number, l: number) => {
    setPage(p);
    setLimit(l);
    // Phân trang dùng lại GPS đã có (không gọi lại navigator).
    fetchUnassigned(p, l, gps);
  };

  const handleRefresh = () => {
    fetchUnassigned(page, limit, "auto");
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <UnassignedOrdersToolbar onRefresh={handleRefresh} />

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Đơn chưa gán</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {total} đơn</div>
              {gps ? (
                <div className="list-page-tag" style={{marginLeft: 8}}>
                  Vị trí: GPS ({gps.latitude.toFixed(4)}, {gps.longitude.toFixed(4)})
                </div>
              ) : null}
            </div>
          </div>
        </div>

        {recommendationError ? (
          <Alert
            type="warning"
            showIcon
            style={{marginBottom: 12}}
            message="Hệ thống đánh giá tạm thời không khả dụng"
            description="Danh sách đơn vẫn hiển thị bình thường. Vui lòng bấm 'Làm mới' để thử lại sau."
          />
        ) : null}

        <div className="table-container shipper-page-table">
          <UnassignedOrdersTable
            orders={data}
            loading={loading}
            pagination={{current: page, pageSize: limit, total}}
            onPageChange={handlePageChange}
            onClaim={handleClaim}
          />
        </div>
      </div>
    </div>
  );
};

export default UnassignedOrdersPage;