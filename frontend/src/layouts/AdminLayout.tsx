import React from "react";
import BaseLayout from "./BaseLayout";
import Header from "../components/common/Header";
import Sidenav from "../components/common/Sidenav";

const AdminLayout: React.FC = () => {
  return <BaseLayout header={<Header />} sidenav={<Sidenav color="#fff" />} />;
};

export default AdminLayout;