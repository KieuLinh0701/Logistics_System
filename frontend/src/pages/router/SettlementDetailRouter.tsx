import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerPaymentSubmissions from "../manager/paymentSubmission/ManagerPaymentSubmissions";
import UserSettlementBatchDetail from "../user/settletmentBatchDetail/UserSettlementBatchDetail";

const SettlementDetailRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "manager":
      return <ManagerPaymentSubmissions />;
    case "user":
      return <UserSettlementBatchDetail />;
    default:
      return <Forbidden />;
  }
};

export default SettlementDetailRouter;