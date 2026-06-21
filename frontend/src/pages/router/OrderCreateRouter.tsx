import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderCreate from "../manager/order/create/ManagerOrderCreate";
import UserOrderCreate from "../user/order/create/UserOrderCreate";

const OrderCreateRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isUser = ["group_user", "user_order_create"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));

    if (isUser) {
        return <UserOrderCreate />;
    }

    if (isManager) {
        return <ManagerOrderCreate />;
    }

    return <Forbidden />;
};

export default OrderCreateRouter;