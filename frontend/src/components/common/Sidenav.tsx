import React, { useState, useEffect } from "react";
import { Menu } from "antd";
import { NavLink, useLocation } from "react-router-dom";
import {
  DashboardOutlined,
  GlobalOutlined,
  ShoppingOutlined,
  CarOutlined,
  HomeOutlined,
  TeamOutlined,
  DollarOutlined,
  DropboxOutlined,
  SearchOutlined,
  UserOutlined,
  ShopOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
  GiftOutlined,
  BarChartOutlined,
  ProfileOutlined,
  InboxOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  TruckOutlined,
  EnvironmentOutlined,
  BankOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import "./Sidenav.css";
import { useMemo } from "react";
import { getUserRole } from "../../utils/authUtils";

type MenuItemType = {
  key: string;
  label: string;
  path?: string;
  icon?: React.ReactNode;
  children?: MenuItemType[];
};

const Sidenav: React.FC = () => {
  const { pathname } = useLocation();
  const role = getUserRole();

  const menuConfig: Record<string, MenuItemType[]> = {
    admin: [
      {
        key: "/dashboard",
        label: "Tổng quan",
        path: "/dashboard",
        icon: <DashboardOutlined />,
      },
      {
        key: "/users",
        label: "Quản lý người dùng",
        path: "/users",
        icon: <UserOutlined />,
      },
      {
        key: "/postoffices",
        label: "Quản lý bưu cục",
        path: "/postoffices",
        icon: <ShopOutlined />,
      },
      {
        key: "/service-types",
        label: "Loại dịch vụ",
        path: "/service-types",
        icon: <SettingOutlined />,
      },
      {
        key: "/orders",
        label: "Đơn hàng",
        path: "/orders",
        icon: <ShoppingCartOutlined />,
      },
      {
        key: "/shipping-requests",
        label: "Yêu cầu hỗ trợ",
        path: "/shipping-requests",
        icon: <ExclamationCircleOutlined />,
      },
      {
        key: "/vehicles",
        label: "Phương tiện",
        path: "/vehicles",
        icon: <CarOutlined />,
      },
      {
        key: "/promotions",
        label: "Khuyến mãi",
        path: "/promotions",
        icon: <GiftOutlined />,
      },
      {
        key: "/fee-configurations",
        label: "Cấu hình phí",
        path: "/fee-configurations",
        icon: <DollarOutlined />,
      },
      {
        key: "/reports",
        label: "Báo cáo",
        path: "/reports",
        icon: <BarChartOutlined />,
      },
      {
        key: "/financial",
        label: "Quản lý dòng tiền",
        path: "/financial",
        icon: <DollarOutlined />,
      },
      {
        key: "/account/settings",
        label: "Cài đặt tài khoản",
        path: "/account/settings?tab=profile",
        icon: <ProfileOutlined />,
      },
    ],
    manager: [
      {
        key: "/dashboard",
        label: "Báo cáo & Thống kê",
        path: "/dashboard",
        icon: <DashboardOutlined />,
      },
      {
        key: "orders",
        label: "Quản lý đơn hàng",
        icon: <ShoppingOutlined />,
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
        icon: <TruckOutlined />,
      },
      {
        key: "/supports",
        label: "Hỗ trợ & Khiếu nại",
        path: "/supports",
        icon: <GlobalOutlined />,
      },
      {
        key: "staff",
        label: "Quản lý nhân sự",
        icon: <TeamOutlined />,
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
        key: "/office",
        label: "Thông tin bưu cục",
        path: "/office",
        icon: <HomeOutlined />,
      },
      {
        key: "/settlements",
        label: "Đối soát",
        icon: <CheckCircleOutlined />, 
        path: "/settlements",
      },
      {
        key: "/vehicles",
        label: "Quản lý phương tiện",
        path: "/vehicles",
        icon: <CarOutlined />,
      },
      {
        key: "/account/settings",
        label: "Cài đặt tài khoản",
        path: "/account/settings?tab=profile",
        icon: <ProfileOutlined />,
      },
    ],
    user: [
      {
        key: "/dashboard",
        label: "Báo cáo & Thống kê",
        path: "/dashboard",
        icon: <DashboardOutlined />,
      },
      {
        key: "orders",
        label: "Quản lý đơn hàng",
        icon: <ShoppingOutlined />,
        children: [
          {
            key: "/orders/list",
            label: "Danh sách đơn hàng",
            path: "/orders/list",
          },
          {
            key: "/orders/request",
            label: "Hỗ trợ & Khiếu nại",
            path: "/orders/requests",
          },
        ],
      },
      {
        key: "/products",
        label: "Quản lý sản phẩm",
        path: "/products",
        icon: <DropboxOutlined />,
      },
      {
        key: "/settlements",
        label: "COD & Đối soát",
        icon: <DollarOutlined />,
        path: "/settlements",
      },
      {
        key: "/bank-accounts",
        label: "Tài khoản ngân hàng",
        icon: <BankOutlined />,
        path: "/bank-accounts",
      },
      {
        key: "tracking",
        label: "Tra cứu thông tin",
        icon: <SearchOutlined />,
        children: [
          {
            key: "/tracking/shipping-fee",
            label: "Tra cứu cước vận chuyển",
            path: "shipping-fee",
          },
          {
            key: "/tracking/office-search",
            label: "Tra cứu bưu cục",
            path: "office-search",
          },
          {
            key: "/tracking/shipping-rates",
            label: "Bảng giá",
            path: "shipping-rates",
          },
        ],
      },
      {
        key: "/account/settings",
        label: "Cài đặt tài khoản",
        path: "/account/settings?tab=profile",
        icon: <ProfileOutlined />,
      },
    ],
    shipper: [
      {
        key: "/dashboard",
        label: "Tổng quan",
        path: "/dashboard",
        icon: <DashboardOutlined />,
      },
      {
        key: "shipper-orders",
        label: "Quản lý đơn hàng",
        icon: <ShoppingOutlined />,
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
        icon: <CarOutlined />,
      },
      {
        key: "/shipper/shipping-requests",
        label: "Yêu cầu lấy hàng",
        path: "/shipper/shipping-requests",
        icon: <ExclamationCircleOutlined />,
      },
      {
        key: "/cod",
        label: "Quản lý COD",
        path: "/cod",
        icon: <DollarOutlined />,
      },
      {
        key: "/history",
        label: "Lịch sử giao hàng",
        path: "/history",
        icon: <ClockCircleOutlined />,
      },
      {
        key: "/report",
        label: "Báo cáo sự cố",
        path: "/report",
        icon: <ExclamationCircleOutlined />,
      },
      {
        key: "/account/settings",
        label: "Cài đặt tài khoản",
        path: "/account/settings?tab=profile",
        icon: <ProfileOutlined />,
      },
    ],
    driver: [
      {
        key: "/dashboard",
        label: "Tổng quan",
        path: "/dashboard",
        icon: <DashboardOutlined />,
      },
      {
        key: "/driver/orders",
        label: "Đơn hàng cần nhận",
        path: "/driver/orders",
        icon: <ShoppingOutlined />,
      },
      {
        key: "/driver/shipments",
        label: "Quản lý chuyến vận chuyển",
        path: "/driver/shipments",
        icon: <TruckOutlined />,
      },
      {
        key: "/driver/route",
        label: "Lộ trình vận chuyển",
        path: "/driver/route",
        icon: <EnvironmentOutlined />,
      },
      {
        key: "/driver/history",
        label: "Lịch sử vận chuyển",
        path: "/driver/history",
        icon: <ClockCircleOutlined />,
      },
      {
        key: "/account/settings",
        label: "Cài đặt tài khoản",
        path: "/account/settings?tab=profile",
        icon: <ProfileOutlined />,
      },
    ],
  };

  const menuItems = useMemo(() => menuConfig[role!] || menuConfig.user, [role]);

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