import React from "react";
import { CheckOutlined } from "@ant-design/icons";
import { Button } from "antd";

interface Props {
  onCreate: (status: "DRAFT" | "PENDING") => void;
  loading?: boolean;
}

const Actions: React.FC<Props> = ({
  onCreate,
  loading = false,
}) => {
  return (
    <div className="create-order-action-buttons-container">

      <Button
        type="primary"
        block
        className="modal-ok-button"
        icon={<CheckOutlined />}
        onClick={() => onCreate("PENDING")}
        loading={loading}
        disabled={loading}
      >
        Tạo đơn
      </Button>
    </div>
  );
};

export default Actions;