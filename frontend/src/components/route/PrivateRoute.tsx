import React from "react";
import {Navigate, useLocation} from "react-router-dom";
import {getCurrentUser, getUserPermissionGroups} from "../../utils/authUtils";
import Forbidden from "../../pages/common/Forbidden";

type PrivateRouteProps = {
    children: React.ReactNode;
    allowedPermissionGroups?: string[];
};

export const PrivateRoute: React.FC<PrivateRouteProps> = ({children, allowedPermissionGroups}) => {
    const user = getCurrentUser();
    const permissionGroups = getUserPermissionGroups();
    const location = useLocation();

    if (!user) {
        return <Navigate to="/login" state={{from: location}} replace/>;
    }

    if (allowedPermissionGroups && allowedPermissionGroups.length > 0) {

        const hasPermission = permissionGroups.some(permissionGroup => allowedPermissionGroups.includes(permissionGroup));
        if (!hasPermission) {
            return <Forbidden/>
        }
    }

    return <>{children}</>;
};