import { useParams, Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import Forbidden from "../../pages/Forbidden";
import { getUserRole } from "../../utils/authUtils";

interface PrivateRouteProps {
  children: ReactNode;
}

export const PrivateRoute = ({ children }: PrivateRouteProps) => {
  const roleFromUrl = useParams<{ role: string }>().role?.toLowerCase();
  const userRole = getUserRole(); 

  // Chưa login hoặc token hết hạn
  if (!userRole) {
    return <Navigate to="/login" replace />;
  }

  // Role URL không trùng với user.role → hiển thị Forbidden
  if (roleFromUrl && roleFromUrl !== userRole) {
    return <Forbidden />;
  }

  return <>{children}</>;
};