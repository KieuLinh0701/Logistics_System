import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerWaybillPrint from "../manager/order/ManagerWaybillPrint";
import UserWaybillPrint from "../user/order/UserWaybillPrint";

const WaybillPrintRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "user":
      return <UserWaybillPrint />;
    case "manager":
      return <ManagerWaybillPrint />;
    default:
      return <Forbidden />;
  }
};

export default WaybillPrintRouter;