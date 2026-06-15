import {Col, message, Row} from "antd";
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
    UserOrderStats,
    UserOrderTimeLineItem,
    UserRevenueStats
} from "../../../types/dashboard.ts";

const UserDashboard: React.FC = () => {
    const [productsOverview, setProductsOverview] = useState<UserDashboardOverviewProductsResponse | undefined>(undefined);
    const [ordersOverview, setOrdersOverview] = useState<UserOrderStats | undefined>(undefined);
    const [revenueOverview, setRevenueOverview] = useState<UserRevenueStats | undefined>(undefined);
    const [productsChart, setProductsChart] = useState<UserDashboardChartProductsResponse | undefined>(undefined);
    const [ordersChart, setOrdersChart] = useState<UserOrderTimeLineItem[] | undefined>(undefined);
    const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>([dayjs().subtract(7, "day"), dayjs()]);
    const [loading, setLoading] = useState(true);
    const [loadingChart, setLoadingChart] = useState(true);

    const hasRevenue = hasPermissionGroup(['USER_COD_STATISTICS', 'GROUP_USER']);
    const hasOrder = hasPermissionGroup(['USER_ORDER_VIEW', 'GROUP_USER']);
    const hasProduct = hasPermissionGroup(['USER_PRODUCT_VIEW', 'GROUP_USER']);
    const hasAnyPermission = hasRevenue || hasOrder || hasProduct;

    const fetchChart = async () => {
        if (!hasOrder && !hasProduct) return;
        setLoadingChart(true);

        const params: SearchRequest = {};
        if (dateRange) {
            params.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
            params.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
        }

        try {
            const [productsRes, ordersRes] = await Promise.all([
                hasProduct ? dashboardApi.getUserChartProducts(params) : Promise.resolve({success: false, data: undefined}),
                hasOrder ? dashboardApi.getUserChartOrders(params) : Promise.resolve({success: false, data: undefined}),
            ]);

            if (productsRes.success) {
                setProductsChart(productsRes.data as any);
            }
            if (ordersRes.success) {
                setOrdersChart(ordersRes.data as any);
            }

        } catch (error: any) {
            message.error(error.message || "Không thể tải dữ liệu biểu đồ");
        } finally {
            setLoadingChart(false);
        }
    };

    const fetchOverview = async () => {
        if (!hasAnyPermission) {
            setLoading(false);
            return;
        }
        setLoading(true);
        try {
            const [productsRes, ordersRes, revenueRes] = await Promise.all([
                hasProduct ? dashboardApi.getUserOverviewProducts() : Promise.resolve({success: false, data: undefined}),
                hasOrder ? dashboardApi.getUserOverviewOrders() : Promise.resolve({success: false, data: undefined}),
                hasRevenue ? dashboardApi.getUserOverviewRevenue() : Promise.resolve({success: false, data: undefined}),
            ]);

            if (productsRes.success) setProductsOverview(productsRes.data as any);
            if (ordersRes.success) setOrdersOverview(ordersRes.data as any);
            if (revenueRes.success) setRevenueOverview(revenueRes.data as any);

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
                {!hasAnyPermission ? (
                    <div className="dashboard-welcome">
                        <div className="dashboard-welcome-icon">👋</div>
                        <h2 className="dashboard-welcome-title">Chào mừng bạn đến với hệ thống!</h2>
                        <p className="dashboard-welcome-desc">
                            Trang tổng quan sẽ hiển thị khi bạn được cấp quyền theo dõi dữ liệu.
                            <br/>
                            Vui lòng liên hệ quản lý cửa hàng để được phân quyền.
                        </p>
                    </div>
                ) : (
                    <>
                        {(hasOrder || hasProduct) && (
                            <>
                                <DateFilter dateRange={dateRange} onDateRangeChange={setDateRange}/>
                                <div className="dashboard-note">
                                    * Bộ lọc thời gian áp dụng cho biểu đồ Lịch sử đơn hàng và top 5 sản phẩm.
                                    Các chỉ số còn lại hiển thị theo dữ liệu hiện tại.
                                </div>
                            </>
                        )}

                        {hasRevenue && (
                            <Row gutter={[16, 16]}>
                                <Col span={24}>
                                    <RevenueOverview data={revenueOverview!}/>
                                </Col>
                            </Row>
                        )}

                        {hasOrder && (
                            <Row gutter={[16, 16]} className="dashboard-divide">
                                <Col xs={24} lg={8}>
                                    <OrderStatusOverview data={ordersOverview!}/>
                                </Col>
                                <Col xs={24} lg={16}>
                                    <OrderTimelineChart data={ordersChart}/>
                                </Col>
                            </Row>
                        )}

                        {hasProduct && productsOverview && productsChart && (
                            <Row gutter={[16, 16]} className="dashboard-divide">
                                <Col xs={24} md={12} lg={6}>
                                    <ProductStatsOverview data={productsOverview?.products}/>
                                </Col>
                                <Col xs={24} md={12} lg={8}>
                                    <ProductTypeChart data={productsOverview?.productCounts}/>
                                </Col>
                                <Col xs={24} lg={10}>
                                    <TopProductTable
                                        topSelling={productsChart?.topSelling}
                                        topReturned={productsChart?.topReturned}
                                    />
                                </Col>
                            </Row>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default UserDashboard;