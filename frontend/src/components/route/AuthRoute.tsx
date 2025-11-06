import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import authApi from "../../api/authApi";

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
  const user = authApi.getCurrentUser();

  // Public route: chỉ cho phép truy cập khi chưa login
  if (type === "public") {
    if (user && user.isVerified) {
      return <Navigate to={`/${user.role}`} replace />; 
    }
    return <>{children}</>;
  }

  // Protected route: chỉ cho phép truy cập khi đã login
  if (type === "protected") {
    if (!user || !user.isVerified) {
      return <Navigate to={fallbackPath} replace />;
    }
    return <>{children}</>;
  }

  return <>{children}</>;
};