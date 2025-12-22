import { Row, Col, message } from "antd";
import { useEffect, useState } from "react";
import dayjs, { Dayjs } from "dayjs";
import dashboardApi from "../../../api/dashboardApi";
import { OrderStatusOverview } from "./components/OrderStatusOverview";
import { ProductStatsOverview } from "./components/ProductStatsOverview";
import { RevenueOverview } from "./components/RevenueOverview";
import DateFilter from "./components/DateFilter";
import type { UserDashboardChartResponse, UserDashboardOverviewResponse } from "../../../types/dashboard";
import { OrderTimelineChart } from "./components/OrderTimelineChart";
import { ProductTypeChart } from "./components/ProductTypeChart";
import "./UserDashboard.css"
import type { SearchRequest } from "../../../types/request";
import TopProductTable from "./components/TopProductTable";

const UserDashboard: React.FC = () => {
  const [data, setData] = useState<UserDashboardOverviewResponse | null>(null);
  const [data2, setData2] = useState<UserDashboardChartResponse | null>(null);
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>([dayjs().subtract(7, "day"), dayjs()]);
  const [loading, setLoading] = useState(true);
  const [loadingChart, setLoadingChart] = useState(true);

  const fetchChart = async () => {
    setLoadingChart(true);

    const params: SearchRequest = {
    };
    if (dateRange) {
      params.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
      params.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
    }

    try {
      const result = await dashboardApi.getUserChart(params);
      if (result.success) setData2(result.data);
      else message.warning(result.message || "Không thể tải dữ liệu biểu đồ. Vui lòng thử lại sau.");
    } catch (error: any) {
      message.error(error.message || "Đã xảy ra lỗi hệ thống. Vui lòng thử lại.");
    } finally {
      setLoadingChart(false);
    }
  };

  useEffect(() => {
    const fetchOverView = async () => {
      setLoading(true);
      try {
        const result = await dashboardApi.getUserOverview();
        if (result.success) setData(result.data);
        else message.warning(result.message || "Không thể tải dữ liệu tổng quan. Vui lòng thử lại sau.");
      } catch (error: any) {
        message.error(error.message || "Đã xảy ra lỗi hệ thống. Vui lòng thử lại.");
      } finally {
        setLoading(false);
      }
    };

    fetchOverView();
  }, []);

  useEffect(() => {
    fetchChart();
  }, [dateRange]);


  if (loading) return <div>Loading dashboard...</div>;

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <DateFilter dateRange={dateRange} onDateRangeChange={setDateRange} />
        <div className="dashboard-note">
          * Bộ lọc thời gian áp dụng cho biểu đồ Lịch sử đơn hàng và top 5 sản phẩm.
          Các chỉ số còn lại hiển thị theo dữ liệu hiện tại.
        </div>


        {/* Revenue */}
        <Row gutter={[16, 16]}>
          <Col span={24}>
            <RevenueOverview data={data!.revenue} />
          </Col>
        </Row>

        {/* Order */}
        <Row gutter={[16, 16]} className="dashboard-divide">
          <Col xs={24} lg={8}>
            <OrderStatusOverview data={data!.orders} />
          </Col>

          <Col xs={24} lg={16}>
            <OrderTimelineChart data={data2!.orderTimelines} />
          </Col>
        </Row>

        {/* Product */}
        <Row gutter={[16, 16]} className="dashboard-divide">
          <Col xs={24} md={12} lg={6}>
            <ProductStatsOverview data={data!.products} />
          </Col>

          <Col xs={24} md={12} lg={8}>
            <ProductTypeChart data={data!.productCounts} />
          </Col>

          <Col xs={24} lg={10}>
            <TopProductTable
              topSelling={data2!.topSelling}
              topReturned={data2!.topReturned}
            />
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default UserDashboard;