import React, {useState, useEffect} from "react";
import {Menu} from "antd";
import {NavLink, useLocation} from "react-router-dom";
import {
    DashboardOutlined,
    GlobalOutlined,
    CustomerServiceOutlined,
    ShoppingOutlined,
    CarOutlined,
    HomeOutlined,
    TeamOutlined,
    DollarOutlined,
    DropboxOutlined,
    UserOutlined,
    ShopOutlined,
    SettingOutlined,
    ShoppingCartOutlined,
    GiftOutlined,
    BarChartOutlined,
    ProfileOutlined,
    ClockCircleOutlined,
    ExclamationCircleOutlined,
    TruckOutlined,
    EnvironmentOutlined,
    BankOutlined,
    CheckCircleOutlined,
    ScanOutlined, UserSwitchOutlined, ContactsOutlined,
} from "@ant-design/icons";
import "./Sidenav.css";
import {useMemo} from "react";
import {getUserRole, hasPermissionGroup} from "../../utils/authUtils";

type MenuItemType = {
    key: string;
    label: string;
    path?: string;
    icon?: React.ReactNode;
    children?: MenuItemType[];
    permissionGroups?: string[];
};

const canViewMenuItem = (item: MenuItemType): boolean => {
    if (!item.permissionGroups || item.permissionGroups.length === 0) return true;
    return hasPermissionGroup(item.permissionGroups);
};

const filterMenuByPermissionGroup = (items: MenuItemType[]): MenuItemType[] => {
    return items
        .filter(canViewMenuItem)
        .map((item) => {
            if (item.children) {
                return { ...item, children: filterMenuByPermissionGroup(item.children) };
            }
            return item;
        })
        .filter((item) => !item.children || item.children.length > 0);
};

const Sidenav: React.FC = () => {
    const {pathname} = useLocation();
    const role = getUserRole();

    const menuConfig: Record<string, MenuItemType[]> = {
        admin: [
            {
                key: "/dashboard",
                label: "Tổng quan",
                path: "/dashboard",
                icon: <DashboardOutlined/>,
                // permissionGroups: ["ORDER_VIEW", "ORDER_MANAGE"],
            },
            {
                key: "/users",
                label: "Quản lý người dùng",
                path: "/users",
                icon: <UserOutlined/>,
            },
            {
                key: "/postoffices",
                label: "Quản lý bưu cục",
                path: "/postoffices",
                icon: <ShopOutlined/>,
            },
            {
                key: "/service-types",
                label: "Loại dịch vụ",
                path: "/service-types",
                icon: <SettingOutlined/>,
            },
            {
                key: "/orders",
                label: "Đơn hàng",
                path: "/orders",
                icon: <ShoppingCartOutlined/>,
            },
            {
                key: "/shipping-requests",
                label: "Yêu cầu hỗ trợ",
                path: "/shipping-requests",
                icon: <ExclamationCircleOutlined/>,
            },
            {
                key: "/vehicles",
                label: "Phương tiện",
                path: "/vehicles",
                icon: <CarOutlined/>,
            },
            {
                key: "/promotions",
                label: "Khuyến mãi",
                path: "/promotions",
                icon: <GiftOutlined/>,
            },
            {
                key: "/fee-configurations",
                label: "Cấu hình phí",
                path: "/fee-configurations",
                icon: <DollarOutlined/>,
            },
            {
                key: "/reports",
                label: "Báo cáo",
                path: "/reports",
                icon: <BarChartOutlined/>,
            },
            {
                key: "/support/tickets",
                label: "Chăm sóc khách hàng",
                path: "/support/tickets",
                icon: <CustomerServiceOutlined/>,
            },
            {
                key: "recruitment-admin",
                label: "Tuyển dụng",
                icon: <TeamOutlined/>,
                children: [
                    {
                        key: "/recruitment/hr/jobs",
                        label: "Tin tuyển dụng",
                        path: "/recruitment/hr/jobs",
                    },
                    {
                        key: "/recruitment/hr/applications",
                        label: "Duyệt hồ sơ",
                        path: "/recruitment/hr/applications",
                    },
                ],
            },
            {
                key: "/financial",
                label: "Quản lý dòng tiền",
                path: "/financial",
                icon: <DollarOutlined/>,
            },
            {
                key: "/account/settings",
                label: "Cài đặt tài khoản",
                path: "/account/settings?tab=profile",
                icon: <ProfileOutlined/>,
            },
        ],
        manager: [
            {
                key: "/dashboard",
                label: "Báo cáo & Thống kê",
                path: "/dashboard",
                icon: <DashboardOutlined/>,
            },
            {
                key: "orders",
                label: "Quản lý đơn hàng",
                icon: <ShoppingOutlined/>,
                children: [
                    {
                        key: "/orders/list",
                        label: "Danh sách đơn hàng",
                        path: "/orders/list",
                    },
                    {
                        key: "/orders/incidents",
                        label: "Báo cáo sự cố",
                        path: "/orders/incidents",
                    },
                ],
            },
            {
                key: "/shipments",
                label: "Quản lý chuyến hàng",
                path: "/shipments",
                icon: <TruckOutlined/>,
            },
            {
                key: "/supports",
                label: "Hỗ trợ & Khiếu nại",
                path: "/supports",
                icon: <GlobalOutlined/>,
            },
            {
                key: "/support/tickets",
                label: "Chăm sóc khách hàng",
                path: "/support/tickets",
                icon: <CustomerServiceOutlined/>,
            },
            {
                key: "staff",
                label: "Quản lý nhân sự",
                icon: <TeamOutlined/>,
                children: [
                    {
                        key: "/employees/list",
                        label: "Danh sách nhân viên",
                        path: "/employees/list",
                    },
                    {
                        key: "/employees/performance",
                        label: "Hiệu suất nhân viên",
                        path: "/employees/performance",
                    },
                    {
                        key: "/employees/assign-area",
                        label: "Phân công Shipper",
                        path: "/employees/assign-area",
                    },
                    {
                        key: "/employees/assign-history",
                        label: "Lịch sử phân công",
                        path: "/employees/assign-history",
                    },
                ],
            },
            {
                key: "recruitment",
                label: "Tuyển dụng",
                icon: <TeamOutlined/>,
                children: [
                    {
                        key: "/recruitment/hr/jobs",
                        label: "Tin tuyển dụng",
                        path: "/recruitment/hr/jobs",
                    },
                    {
                        key: "/recruitment/hr/applications",
                        label: "Duyệt hồ sơ",
                        path: "/recruitment/hr/applications",
                    },
                ],
            },
            {
                key: "/office",
                label: "Thông tin bưu cục",
                path: "/office",
                icon: <HomeOutlined/>,
            },
            {
                key: "/manager/leaves",
                label: "Duyệt nghỉ phép",
                path: "/manager/leaves",
                icon: <ClockCircleOutlined/>,
            },
            {
                key: "/settlements",
                label: "Đối soát",
                icon: <CheckCircleOutlined/>,
                path: "/settlements",
            },
            {
                key: "/vehicles",
                label: "Quản lý phương tiện",
                path: "/vehicles",
                icon: <CarOutlined/>,
            },
            {
                key: "/account/settings",
                label: "Cài đặt tài khoản",
                path: "/account/settings?tab=profile",
                icon: <ProfileOutlined/>,
            },
        ],
        user: [
            {
                key: "/dashboard",
                label: "Báo cáo & Thống kê",
                path: "/dashboard",
                icon: <DashboardOutlined/>,
            },
            {
                key: "orders",
                label: "Quản lý đơn hàng",
                icon: <ShoppingOutlined/>,
                permissionGroups: ['GROUP_USER', 'USER_ORDER_VIEW', 'USER_SUPPORT_VIEW'],
                children: [
                    {
                        key: "/orders/list",
                        label: "Danh sách đơn hàng",
                        path: "/orders/list",
                        permissionGroups: ['GROUP_USER', 'USER_ORDER_VIEW'],
                    },
                    {
                        key: "/orders/request",
                        label: "Hỗ trợ & Khiếu nại",
                        path: "/orders/requests",
                        permissionGroups: ['GROUP_USER', 'USER_SUPPORT_VIEW'],
                    },
                ],
            },
            {
                key: "/products",
                label: "Quản lý sản phẩm",
                path: "/products",
                icon: <DropboxOutlined/>,
                permissionGroups: ['GROUP_USER', 'USER_PRODUCT_VIEW'],
            },
            {
                key: "/roles",
                label: "Phân quyền",
                path: "/roles",
                icon: <UserSwitchOutlined />,
                permissionGroups: ['GROUP_USER', 'USER_PERMISSION_GROUP_VIEW'],
            },
            {
                key: "/employees",
                label: "Quản lý nhân viên",
                path: "/employees",
                icon: <TeamOutlined />,
                permissionGroups: ['GROUP_USER', 'USER_EMPLOYEE_VIEW'],
            },
            {
                key: "/customers",
                label: "Quản lý khách hàng",
                path: "/customers",
                icon: <ContactsOutlined />,
                permissionGroups: ['GROUP_USER', 'USER_CUSTOMER_VIEW'],
            },
            {
                key: "/settlements",
                label: "COD & Đối soát",
                icon: <DollarOutlined/>,
                path: "/settlements",
                permissionGroups: ['GROUP_USER', 'USER_COD_SESSION_VIEW', 'USER_COD_SCHEDULE_VIEW', 'USER_COD_STATISTICS', 'USER_COD_PAYMENT'],
            },
            {
                key: "/bank-accounts",
                label: "Tài khoản ngân hàng",
                icon: <BankOutlined/>,
                path: "/bank-accounts",
                permissionGroups: ['GROUP_USER', 'USER_BANK_VIEW'],
            },
            {
                key: "/account/settings",
                label: "Cài đặt tài khoản",
                path: "/account/settings?tab=profile",
                icon: <ProfileOutlined/>,
            },
        ],
        shipper: [
            {
                key: "/dashboard",
                label: "Tổng quan",
                path: "/dashboard",
                icon: <DashboardOutlined/>,
            },
            {
                key: "/shipper/scan-barcode",
                label: "Quét mã vận đơn",
                path: "/shipper/scan-barcode",
                icon: <ScanOutlined/>,
            },
            {
                key: "shipper-orders",
                label: "Quản lý đơn hàng",
                icon: <ShoppingOutlined/>,
                children: [
                    {
                        key: "/shipper/orders-unassigned",
                        label: "Danh sách đơn hàng",
                        path: "/shipper/orders-unassigned",
                    },
                    {
                        key: "/shipper/orders",
                        label: "Đơn hàng cần giao",
                        path: "/shipper/orders",
                    },
                ],
            },
            {
                key: "/route",
                label: "Lộ trình giao hàng",
                path: "/route",
                icon: <CarOutlined/>,
            },
            {
                key: "/shipper/shipping-requests",
                label: "Yêu cầu lấy hàng",
                path: "/shipper/shipping-requests",
                icon: <ExclamationCircleOutlined/>,
            },
            {
                key: "/cod",
                label: "Quản lý COD",
                path: "/cod",
                icon: <DollarOutlined/>,
            },
            {
                key: "/history",
                label: "Lịch sử giao hàng",
                path: "/history",
                icon: <ClockCircleOutlined/>,
            },
            {
                key: "/report",
                label: "Báo cáo sự cố",
                path: "/report",
                icon: <ExclamationCircleOutlined/>,
            },
            {
                key: "/employee/leaves",
                label: "Xin nghỉ phép",
                path: "/employee/leaves",
                icon: <ClockCircleOutlined/>,
            },
            {
                key: "/account/settings",
                label: "Cài đặt tài khoản",
                path: "/account/settings?tab=profile",
                icon: <ProfileOutlined/>,
            },
        ],
        driver: [
            {
                key: "/dashboard",
                label: "Tổng quan",
                path: "/dashboard",
                icon: <DashboardOutlined/>,
            },
            {
                key: "/driver/shipments",
                label: "Quản lý chuyến vận chuyển",
                path: "/driver/shipments",
                icon: <TruckOutlined/>,
            },
            {
                key: "/driver/route",
                label: "Lộ trình vận chuyển",
                path: "/driver/route",
                icon: <EnvironmentOutlined/>,
            },
            {
                key: "/driver/history",
                label: "Lịch sử vận chuyển",
                path: "/driver/history",
                icon: <ClockCircleOutlined/>,
            },
            {
                key: "/employee/leaves",
                label: "Xin nghỉ phép",
                path: "/employee/leaves",
                icon: <ClockCircleOutlined/>,
            },
            {
                key: "/account/settings",
                label: "Cài đặt tài khoản",
                path: "/account/settings?tab=profile",
                icon: <ProfileOutlined/>,
            },
        ],
    };

    const menuItems = useMemo(() => {
        const baseItems = menuConfig[role!] || menuConfig.user;
        return filterMenuByPermissionGroup(baseItems);
    }, [role]);

    const generateMenuItems = (items: MenuItemType[]): any[] => {
        return items.map((item) => {
            if (item.children) {
                return {
                    key: item.key,
                    icon: item.icon,
                    label: item.label,
                    children: generateMenuItems(item.children),
                };
            }
            return {
                key: item.key,
                icon: item.icon,
                label: <NavLink to={item.path || "#"}>{item.label}</NavLink>,
            };
        });
    };

    const [openKeys, setOpenKeys] = useState<string[]>([]);

    useEffect(() => {
        const keys: string[] = [];
        menuItems.forEach((item) => {
            if (item.children?.some((child) => child.key === pathname)) {
                keys.push(item.key);
            }
        });
        setOpenKeys(keys);
    }, [pathname, menuItems]);

    return (
        <div className="sidenav">
            <Menu
                theme="light"
                mode="inline"
                selectedKeys={[pathname]}
                openKeys={openKeys}
                onOpenChange={(keys) => setOpenKeys(keys as string[])}
                className="sidenav-menu"
                items={generateMenuItems(menuItems)}
            />
        </div>
    );
};

export default Sidenav;