import {getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import UserAuditLogs from "../user/audit-logs/UserAuditLogs.tsx";
import ManagerAuditLogs from "../manager/audit-logs/ManagerAuditLogs.tsx";
import AdminAuditLogs from "../admin/audit-logs/AdminAuditLogs.tsx";

const AuditLogsRouter = () => {
    const userPermissions = getUserPermissionGroups();

    const isUser = ["group_user", "user_audit_log_view"].some(p => userPermissions.includes(p));
    const isManager = ["group_manager"].some(p => userPermissions.includes(p));
    const isAdmin = ["group_admin"].some(p => userPermissions.includes(p));

    if (isUser) {
        return <UserAuditLogs />;
    }

    if (isManager) {
        return <ManagerAuditLogs />;
    }

    if (isAdmin) {
        return <AdminAuditLogs />;
    }

    return <Forbidden />;
};

export default AuditLogsRouter;