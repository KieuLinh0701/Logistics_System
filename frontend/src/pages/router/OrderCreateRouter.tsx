import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderCreate from "../manager/order/create/ManagerOrderCreate";
import UserOrderCreate from "../user/order/create/UserOrderCreate";

const OrderCreateRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "user":
      return <UserOrderCreate />;
    case "manager":
      return <ManagerOrderCreate />;
    default:
      return <Forbidden />;
  }
};

export default OrderCreateRouter;