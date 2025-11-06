import { Navigate } from "react-router-dom";
import Forbidden from "../pages/Forbidden";
import type { ReactNode } from "react";
import authApi from "../api/authApi";

interface PrivateRouteProps {
  children: ReactNode;
  roles?: string[];
}

export const PrivateRoute = ({ children, roles }: PrivateRouteProps) => {
  const user = authApi.getCurrentUser();

  if (!user || !user.isVerified) {
    return <Navigate to="/" replace />;
  }

  if (roles && !roles.includes(user.role)) {
    return <Forbidden />;
  }

  return <>{children}</>;
};