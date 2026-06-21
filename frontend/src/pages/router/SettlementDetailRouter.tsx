import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerPaymentSubmissions from "../manager/paymentSubmission/ManagerPaymentSubmissions";
import UserSettlementBatchDetail from "../user/settletmentBatchDetail/UserSettlementBatchDetail";

const SettlementDetailRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isManager = ["group_manager"].some(p => userPermissions.includes(p));
    const isUser = ["group_user", "user_cod_detail"].some(p => userPermissions.includes(p));

    if (isManager) {
        return <ManagerPaymentSubmissions />;
    }

    if (isUser) {
        return <UserSettlementBatchDetail />;
    }

    return <Forbidden />;
};

export default SettlementDetailRouter;