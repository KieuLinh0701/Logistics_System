import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import UserAuditLogsByUser from "../user/employee/audit-logs-user/UserAuditLogsByUser.tsx";
import ManagerAuditLogsByEmployee from "../manager/employee/audit-logs/ManagerAuditLogsByEmployee.tsx";

const AuditLogsDetailRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isUser = ["group_user", "user_audit_log_view_detail"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));

    if (isUser) {
        return <UserAuditLogsByUser />;
    }

    if (isManager) {
        return <ManagerAuditLogsByEmployee />;
    }

    return <Forbidden />;
};

export default AuditLogsDetailRouter;