import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { getUserRole, getCurrentUser } from "../../utils/authUtils";
import Forbidden from "../../pages/Forbidden";

type PrivateRouteProps = {
  children: React.ReactNode;
  allowedRoles?: string[];
};

export const PrivateRoute: React.FC<PrivateRouteProps> = ({ children, allowedRoles }) => {
  const user = getCurrentUser();
  const role = getUserRole();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Nếu route có giới hạn role
  if (allowedRoles && !allowedRoles.includes(role!)) {
    return <Forbidden />
  }

  return <>{children}</>;
};