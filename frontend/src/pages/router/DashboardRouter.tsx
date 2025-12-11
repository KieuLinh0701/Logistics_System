import { getUserRole } from "../../utils/authUtils";
import ManagerDashboard from "../manager/ManagerDashboard";
import UserDashboard from "../user/UserDashboard";
import AdminDashboard from "../admin/Dashboard";
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
    default:
      return <Forbidden />;
  }
};

export default DashboardRouter;