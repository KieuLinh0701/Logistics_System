import { getUserRole } from "../../utils/authUtils";
import Forbidden from "../common/Forbidden";
import ManagerPaymentSubmissionBatchs from "../manager/paymentSubmissionBatch/ManagerPaymentSubmissionBatchs";
import UserSettlementBatchs from "../user/settletmentBatch/UserSettlementBatchs";

const SettlementRouter = () => {
  const role = getUserRole();

  switch (role) {
    case "manager":
      return <ManagerPaymentSubmissionBatchs />;
    case "user":
      return <UserSettlementBatchs />;
    default:
      return <Forbidden />;
  }
};

export default SettlementRouter;