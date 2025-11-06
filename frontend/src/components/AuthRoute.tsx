import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import authApi from "../api/authApi";

interface AuthRouteProps {
  children: ReactNode;
  type: 'public' | 'protected';
  fallbackPath?: string;
}

export const AuthRoute = ({ 
  children, 
  type, 
  fallbackPath = "/" 
}: AuthRouteProps) => {
  // const { user, isAuthenticated } = useAppSelector((state) => state.auth);
  const user = authApi.getCurrentUser();

  const roleRoutes: Record<string, string> = {
    admin: "/admin/dashboard",
    manager: "/manager/dashboard",
    staff: "/staff/dashboard",
    shipper: "/shipper/dashboard",
    user: "/user/dashboard",
    driver: "/shipper/dashboard",
  };

  // Public route: chỉ cho phép truy cập khi CHƯA login
  if (type === 'public') {
    if (user && user.isVerified) {
      return <Navigate to={roleRoutes[user.role] || fallbackPath} replace />;
    }
    return <>{children}</>;
  }

  // Protected route: chỉ cho phép truy cập khi ĐÃ login
  if (type === 'protected') {
    if (!user || !user.isVerified) {
      return <Navigate to={fallbackPath} replace />;
    }
    return <>{children}</>;
  }

  return <>{children}</>;
};