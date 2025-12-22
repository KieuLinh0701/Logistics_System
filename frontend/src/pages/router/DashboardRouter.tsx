import { getUserRole } from "../../utils/authUtils";
import ManagerDashboard from "../manager/ManagerDashboard";
import UserDashboard from "../user/dashboard/UserDashboard";
import AdminDashboard from "../admin/Dashboard";
import ShipperDashboard from "../shipper/Dashboard";
import DriverDashboard from "../driver/Dashboard";
import Forbidden from "../common/Forbidden";

const DashboardRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "admin":
      return <AdminDashboard />;
    case "manager":
      return <ManagerDashboard />;
    case "user":
      return <UserDashboard />;
    case "shipper":
      return <ShipperDashboard />;
    case "driver":
      return <DriverDashboard />;
    default:
      return <Forbidden />;
  }
};

export default DashboardRouter;