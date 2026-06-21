import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderEdit from "../manager/order/edit/ManagerOrderEdit";
import UserOrderEdit from "../user/order/edit/UserOrderEdit";

const OrderEditRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isUser = ["group_user", "user_order_edit"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));

    if (isUser) {
        return <UserOrderEdit />;
    }

    if (isManager) {
        return <ManagerOrderEdit />;
    }

    return <Forbidden />;
};

export default OrderEditRouter;