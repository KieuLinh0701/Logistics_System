import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerWaybillPrint from "../manager/order/ManagerWaybillPrint";
import UserWaybillPrint from "../user/order/UserWaybillPrint";

const WaybillPrintRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isUser = ["group_user", "user_order_print_bulk", "user_order_print_single"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));

    if (isUser) {
        return <UserWaybillPrint />;
    }

    if (isManager) {
        return <ManagerWaybillPrint />;
    }

    return <Forbidden />;
};

export default WaybillPrintRouter;