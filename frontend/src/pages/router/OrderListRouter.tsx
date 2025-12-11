import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import UserOrderList from "../user/order/list/UserOrderList";

const OrderListRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "user":
      return <UserOrderList />;
    default:
      return <Forbidden />;
  }
};

export default OrderListRouter;