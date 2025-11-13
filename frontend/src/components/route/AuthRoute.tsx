import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import { getUserRole } from "../../utils/authUtils";

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
  const role = getUserRole();

  // Public route: chỉ cho phép truy cập khi chưa login
  if (type === "public") {
    if (role) {
      return <Navigate to={`/dashboard`} replace />;
    }
    return <>{children}</>;
  }

  // Protected route: chỉ cho phép truy cập khi đã login
  if (type === "protected") {
    if (!role) {
      return <Navigate to={fallbackPath} replace />;
    }
    return <>{children}</>;
  }

  return <>{children}</>;
};