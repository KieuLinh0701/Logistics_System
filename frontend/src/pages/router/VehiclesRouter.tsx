import { getUserRole } from "../../utils/authUtils";
import AdminVehicles from "../admin/Vehicles";
import Forbidden from "../common/Forbidden";
import ManagerVehicles from "../manager/vehicle/ManagerVehicles";

const VehiclesRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "admin":
      return <AdminVehicles />;
    case "manager":
      return <ManagerVehicles />;
    default:
      return <Forbidden />;
  }
};

export default VehiclesRouter;