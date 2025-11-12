import React, { useState, useEffect } from "react";
import { Menu } from "antd";
import { NavLink, useLocation } from "react-router-dom";
import {
  DashboardOutlined,
  GlobalOutlined,
  ShoppingOutlined,
  CarOutlined,
  DatabaseOutlined,
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
} from "@ant-design/icons";
import "./Sidenav.css";
import { getUserRole } from "../../utils/authUtils";

const { SubMenu } = Menu;

type MenuItem = {
  key: string;
  label: string;
  path?: string;
  icon?: React.ReactNode;
  children?: MenuItem[];
};

type Props = {
  color: string;
};

const Sidenav: React.FC<Props> = () => {
  const { pathname } = useLocation();

  const role = getUserRole();

  const menuConfig: Record<string, MenuItem[]> = {
    admin: [
      {
        key: "/dashboard",
        label: "Báo cáo & Thống kê",
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
        key: "/servicetypes",
        label: "Loại dịch vụ",
        path: "/servicetypes",
        icon: <SettingOutlined />,
      },
      {
        key: "/orders",
        label: "Đơn hàng",
        path: "/orders",
        icon: <ShoppingCartOutlined />,
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
        path: "/account/settings",
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
        ],
      },
      {
        key: "/office",
        label: "Thông tin bưu cục",
        path: "/office",
        icon: <HomeOutlined />,
      },
      {
        key: "/warehouse",
        label: "Đơn nhập/xuất kho",
        path: "/warehouse",
        icon: <DatabaseOutlined />,
      },
      {
        key: "/finance",
        label: "Quản lý dòng tiền",
        icon: <DollarOutlined />,
        children: [
          {
            key: "/finance/transactions",
            label: "Theo dõi thu - chi",
            path: "/finance/transactions",
          },
          {
            key: "/finance/settlements",
            label: "Đối soát",
            path: "/finance/settlements",
          },
        ],
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
        path: "/account/settings",
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
        key: "/transactions",
        label: "Quản lý giao dịch",
        icon: <DollarOutlined />,
        path: "/transactions",
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
            path: "/tracking/shipping-fee",
          },
          {
            key: "/tracking/office-search",
            label: "Tra cứu bưu cục",
            path: "/tracking/office-search",
          },
          {
            key: "/info/shipping-rates",
            label: "Bảng giá",
            path: "/info/shipping-rates",
          },
        ],
      },
      {
        key: "/account/settings",
        label: "Cài đặt tài khoản",
        path: "/account/settings",
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
            key: "/orders-unassigned",
            label: "Danh sách đơn hàng",
            path: "/orders-unassigned",
          },
          {
            key: "/orders",
            label: "Đơn hàng cần giao",
            path: "/orders",
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
        key: "/pickup",
        label: "Nhận/Trả hàng",
        path: "/pickup",
        icon: <InboxOutlined />,
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
        path: "/account/settings",
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
        key: "/orders",
        label: "Đơn hàng cần nhận",
        path: "/orders",
        icon: <ShoppingOutlined />,
      },
      {
        key: "/shipments",
        label: "Quản lý chuyến vận chuyển",
        path: "/shipments",
        icon: <TruckOutlined />,
      },
      {
        key: "/route",
        label: "Lộ trình vận chuyển",
        path: "/route",
        icon: <EnvironmentOutlined />,
      },
      {
        key: "/history",
        label: "Lịch sử vận chuyển",
        path: "/history",
        icon: <ClockCircleOutlined />,
      },
      {
        key: "/account/settings",
        label: "Cài đặt tài khoản",
        path: "/account/settings",
        icon: <ProfileOutlined />,
      },
    ],
  };

  const menuItems = menuConfig[role!] || menuConfig.user;

  const [openKeys, setOpenKeys] = useState<string[]>([]);
  useEffect(() => {
    const keys: string[] = [];
    menuItems.forEach((item) => {
      if (item.children?.some((child) => child.key === pathname)) {
        keys.push(item.key);
      }
    });
    setOpenKeys(keys);
  }, [pathname]);

  return (
    <div className="sidenav">
      <Menu
        theme="light"
        mode="inline"
        selectedKeys={[pathname]}
        openKeys={openKeys}
        onOpenChange={(keys) => setOpenKeys(keys as string[])}
        className="sidenav-menu"
      >
        {menuItems.map((item) =>
          item.children ? (
            <SubMenu key={item.key} icon={item.icon} title={item.label}>
              {item.children.map((child: MenuItem) => (
                <Menu.Item key={child.key} icon={child.icon}>
                  <NavLink to={child.path || "#"}>{child.label}</NavLink>
                </Menu.Item>
              ))}
            </SubMenu>
          ) : (
            <Menu.Item key={item.key} icon={item.icon}>
              <NavLink to={item.path || "#"}>{item.label}</NavLink>
            </Menu.Item>
          )
        )}
      </Menu>
    </div>
  );
};

export default Sidenav;