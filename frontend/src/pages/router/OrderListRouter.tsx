import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderList from "../manager/order/list/ManagerOrderList";
import UserOrderList from "../user/order/list/UserOrderList";

const OrderListRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "user":
      return <UserOrderList />;
    case "manager":
      return <ManagerOrderList />;
    default:
      return <Forbidden />;
  }
};

export default OrderListRouter;