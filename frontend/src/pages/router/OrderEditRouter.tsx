import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerOrderEdit from "../manager/order/edit/ManagerOrderEdit";
import UserOrderEdit from "../user/order/edit/UserOrderEdit";

const OrderEditRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "user":
      return <UserOrderEdit />;
    case "manager":
      return <ManagerOrderEdit />;
    default:
      return <Forbidden />;
  }
};

export default OrderEditRouter;