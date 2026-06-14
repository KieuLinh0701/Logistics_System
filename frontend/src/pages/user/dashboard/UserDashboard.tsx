import {Row, Col, message} from "antd";
import {useEffect, useState} from "react";
import dayjs, {Dayjs} from "dayjs";
import dashboardApi from "../../../api/dashboardApi";
import {OrderStatusOverview} from "./components/OrderStatusOverview";
import {ProductStatsOverview} from "./components/ProductStatsOverview";
import {RevenueOverview} from "./components/RevenueOverview";
import DateFilter from "./components/DateFilter";
import {OrderTimelineChart} from "./components/OrderTimelineChart";
import {ProductTypeChart} from "./components/ProductTypeChart";
import "./UserDashboard.css"
import type {SearchRequest} from "../../../types/request";
import TopProductTable from "./components/TopProductTable";
import {hasPermissionGroup} from "../../../utils/authUtils.ts";
import type {
    UserDashboardChartProductsResponse,
    UserDashboardOverviewProductsResponse,
    UserOrderStats, UserOrderTimeLineItem,
    UserRevenueStats
} from "../../../types/dashboard.ts";

const UserDashboard: React.FC = () => {
    const [productsOverview, setProductsOverview] = useState<UserDashboardOverviewProductsResponse | null>(null);
    const [ordersOverview, setOrdersOverview] = useState<UserOrderStats | null>(null);
    const [revenueOverview, setRevenueOverview] = useState<UserRevenueStats | null>(null);
    const [productsChart, setProductsChart] = useState<UserDashboardChartProductsResponse | null>(null);
    const [ordersChart, setOrdersChart] = useState<UserOrderTimeLineItem[] | null>(null);
    const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>([dayjs().subtract(7, "day"), dayjs()]);
    const [loading, setLoading] = useState(true);
    const [loadingChart, setLoadingChart] = useState(true);

    const fetchChart = async () => {
        setLoadingChart(true);

        const params: SearchRequest = {};
        if (dateRange) {
            params.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
            params.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
        }

        try {
            const [productsRes, ordersRes] = await Promise.all([
                dashboardApi.getUserChartProducts(params),
                dashboardApi.getUserChartOrders(params)
            ]);

            if (productsRes.success) setProductsChart(productsRes.data);
            if (ordersRes.success) setOrdersChart(ordersRes.data);

        } catch (error: any) {
            message.error(error.message || "Không thể tải dữ liệu biểu đồ");
        } finally {
            setLoadingChart(false);
        }
    };

    const fetchOverview = async () => {
        setLoading(true);
        try {
            const [productsRes, ordersRes, revenueRes] = await Promise.all([
                dashboardApi.getUserOverviewProducts(),
                dashboardApi.getUserOverviewOrders(),
                dashboardApi.getUserOverviewRevenue()
            ]);

            if (productsRes.success) setProductsOverview(productsRes.data);
            if (ordersRes.success) setOrdersOverview(ordersRes.data);
            if (revenueRes.success) setRevenueOverview(revenueRes.data);

        } catch (error: any) {
            message.error("Không thể tải dữ liệu tổng quan.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchOverview();
    }, []);

    useEffect(() => {
        fetchChart();
    }, [dateRange]);


    if (loading) return <div>Loading dashboard...</div>;

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_VIEW', 'USER_PRODUCT_VIEW']) && (
                    <>
                        <DateFilter dateRange={dateRange} onDateRangeChange={setDateRange}/>
                        <div className="dashboard-note">
                            * Bộ lọc thời gian áp dụng cho biểu đồ Lịch sử đơn hàng và top 5 sản phẩm.
                            Các chỉ số còn lại hiển thị theo dữ liệu hiện tại.
                        </div>
                    </>
                )}


                {/* Revenue */}
                {hasPermissionGroup(['GROUP_USER', 'USER_COD_STATISTICS']) && (
                    <Row gutter={[16, 16]}>
                        <Col span={24}>
                            <RevenueOverview data={revenueOverview!}/>
                        </Col>
                    </Row>
                )}

                {/* Order */}
                {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_VIEW']) && (
                    <Row gutter={[16, 16]} className="dashboard-divide">
                        <Col xs={24} lg={8}>
                            <OrderStatusOverview data={ordersOverview!}/>
                        </Col>

                        <Col xs={24} lg={16}>
                            <OrderTimelineChart data={ordersChart!}/>
                        </Col>
                    </Row>
                )}

                {/* Product */}
                {hasPermissionGroup(['GROUP_USER', 'USER_PRODUCT_VIEW']) && (
                    <Row gutter={[16, 16]} className="dashboard-divide">
                        <Col xs={24} md={12} lg={6}>
                            <ProductStatsOverview data={productsOverview!.products}/>
                        </Col>

                        <Col xs={24} md={12} lg={8}>
                            <ProductTypeChart data={productsOverview!.productCounts}/>
                        </Col>

                        <Col xs={24} lg={10}>
                            <TopProductTable
                                topSelling={productsChart!.topSelling}
                                topReturned={productsChart!.topReturned}
                            />
                        </Col>
                    </Row>
                )}
            </div>
        </div>
    );
};

export default UserDashboard;