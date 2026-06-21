import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderDetail from "../manager/order/detail/ManagerOrderDetail";
import UserOrderDetail from "../user/order/detail/UserOrderDetail";

const OrderDetailRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isUser = ["group_user", "user_order_detail"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));

    if (isUser) {
        return <UserOrderDetail />;
    }

    if (isManager) {
        return <ManagerOrderDetail />;
    }

    return <Forbidden />;
};

export default OrderDetailRouter;