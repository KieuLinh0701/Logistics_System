import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderDetail from "../manager/order/detail/ManagerOrderDetail";
import UserOrderDetail from "../user/order/detail/UserOrderDetail";

const OrderDetailRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "user":
      return <UserOrderDetail />;
    case "manager":
      return <ManagerOrderDetail />;
    default:
      return <Forbidden />;
  }
};

export default OrderDetailRouter;