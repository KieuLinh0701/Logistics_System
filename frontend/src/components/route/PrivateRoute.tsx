import { useParams, Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import Forbidden from "../../pages/Forbidden";
import authApi from "../../api/authApi";

interface PrivateRouteProps {
  children: ReactNode;
}

export const PrivateRoute = ({ children }: PrivateRouteProps) => {
  const user = authApi.getCurrentUser();
  const { role: paramRole } = useParams<{ role: string }>();

  // Chưa login hoặc chưa xác thực → chuyển về login
  if (!user || !user.isVerified) {
    return <Navigate to="/login" replace />;
  }

  // Role URL không trùng với user.role → hiển thị Forbidden
  if (paramRole && paramRole !== user.role) {
    return <Forbidden />;
  }

  return <>{children}</>;
};