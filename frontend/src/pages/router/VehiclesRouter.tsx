import {getUserPermissionGroups} from "../../utils/authUtils";
import AdminVehicles from "../admin/vehicles/VehiclesPage";
import Forbidden from "../common/Forbidden";
import ManagerVehicles from "../manager/vehicle/ManagerVehicles";

const VehiclesRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isAdmin = ["group_admin"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));

    if (isAdmin) {
        return <AdminVehicles />;
    }

    if (isManager) {
        return <ManagerVehicles />;
    }

    return <Forbidden />;
};

export default VehiclesRouter;