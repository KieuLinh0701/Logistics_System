import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderList from "../manager/order/list/ManagerOrderList";
import UserOrderList from "../user/order/list/UserOrderList";

const OrderListRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isUser = ["group_user", "user_order_view"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));

    if (isUser) {
        return <UserOrderList />;
    }

    if (isManager) {
        return <ManagerOrderList />;
    }

    return <Forbidden />;
};

export default OrderListRouter;