import React, { useCallback, useEffect, useRef, useState } from "react";
import { Tabs } from "antd";
import { useSearchParams } from "react-router-dom";
import DeliveryOrdersTab from "./components/DeliveryOrdersTab";
import ReturnOrdersTab from "./components/ReturnOrdersTab";
import PickupRequestsTab from "./components/PickupRequestsTab";
import MyOrdersToolbar, { type TabKey } from "./components/MyOrdersToolbar";
import "./MyOrdersPage.css";

export interface TabRefreshHandle {
  reload: () => void;
}

const MyOrdersPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const getTabFromUrl = (): TabKey => {
    const tab = searchParams.get("tab");
    if (tab === "return" || tab === "pickup") return tab;
    return "delivery";
  };

  const [activeTab, setActiveTab] = useState<TabKey>(getTabFromUrl());
  const [toolbarSearch, setToolbarSearch] = useState("");
  const [toolbarStatus, setToolbarStatus] = useState<string | undefined>(undefined);

  // Refs to call reload from each tab
  const deliveryTabRef = useRef<TabRefreshHandle | null>(null);
  const returnTabRef = useRef<TabRefreshHandle | null>(null);
  const pickupTabRef = useRef<TabRefreshHandle | null>(null);

  const handleTabChange = (key: string) => {
    const newTab = key as TabKey;
    setActiveTab(newTab);
    setToolbarStatus(undefined); // Reset status when switching tabs
    setSearchParams({ tab: newTab }, { replace: true });
  };

  const handleToolbarSearch = (value: string) => {
    setToolbarSearch(value);
  };

  const handleToolbarStatusChange = (value: string | undefined) => {
    setToolbarStatus(value);
  };

  const handleRefresh = () => {
    const currentRef = activeTab === "delivery" ? deliveryTabRef
      : activeTab === "return" ? returnTabRef
      : pickupTabRef;
    currentRef.current?.reload();
  };

  const tabItems = [
    {
      key: "delivery",
      label: <span className="tab-label">Đơn hàng cần giao</span>,
      children: (
        <DeliveryOrdersTab
          ref={deliveryTabRef}
          search={toolbarSearch}
          status={toolbarStatus}
        />
      ),
    },
    {
      key: "return",
      label: <span className="tab-label">Đơn hàng hoàn trả</span>,
      children: (
        <ReturnOrdersTab
          ref={returnTabRef}
          search={toolbarSearch}
          status={toolbarStatus}
        />
      ),
    },
    {
      key: "pickup",
      label: <span className="tab-label">Yêu cầu lấy hàng tại nhà</span>,
      children: (
        <PickupRequestsTab
          ref={pickupTabRef}
          search={toolbarSearch}
        />
      ),
    },
  ];

  return (
    <div className="list-page-layout my-orders-page-root">
      <div className="list-page-content">
        {/* Toolbar */}
        <MyOrdersToolbar
          activeTab={activeTab}
          search={toolbarSearch}
          status={toolbarStatus}
          onSearchChange={handleToolbarSearch}
          onStatusChange={handleToolbarStatusChange}
          onRefresh={handleRefresh}
        />

        {/* Tabs */}
        <Tabs
          activeKey={activeTab}
          onChange={handleTabChange}
          className="my-orders-tabs"
          items={tabItems}
        />
      </div>
    </div>
  );
};

export default MyOrdersPage;
