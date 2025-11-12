import { getUserRole } from "../utils/authUtils";
import ManagerDashboard from "./manager/ManagerDashboard";
import UserDashboard from "./user/UserDashboard";

const DashboardRouter = () => {
  const role = getUserRole();

  switch(role) {
    case "manager":
      return <ManagerDashboard />;
    case "user":
      return <UserDashboard />;
    default:
      return <div>Access Denied</div>;
  }
};

export default DashboardRouter;