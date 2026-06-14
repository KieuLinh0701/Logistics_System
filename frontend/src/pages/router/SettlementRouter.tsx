import { getUserPermissionGroups } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerPaymentSubmissionBatchs from "../manager/paymentSubmissionBatch/ManagerPaymentSubmissionBatchs";
import UserSettlementBatchs from "../user/settletmentBatch/UserSettlementBatchs";

const SettlementRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isManager = ["group_manager"].some(p => userPermissions.includes(p));
    const isUser = ["group_user", "user_cod_session_view"].some(p => userPermissions.includes(p));

    if (isManager) {
        return <ManagerPaymentSubmissionBatchs />;
    }

    if (isUser) {
        return <UserSettlementBatchs />;
    }

    return <Forbidden />;
};

export default SettlementRouter;