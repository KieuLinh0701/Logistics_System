import React from "react";
import { CheckOutlined, SaveOutlined } from "@ant-design/icons";
import { Button } from "antd";

interface Props {
  onCreate: (status: "DRAFT" | "PENDING") => void;
  loading?: boolean;
  disabled: boolean;
}

const Actions: React.FC<Props> = ({
  onCreate,
  loading = false,
  disabled,
}) => {
  return (
    <div className="create-order-action-buttons-container">
      <Button
        block
        className="modal-cancel-button"
        icon={<SaveOutlined />}
        onClick={() => onCreate("DRAFT")}
        loading={loading}
        disabled={loading || disabled}
      >
        Lưu nháp
      </Button>

      <Button
        type="primary"
        block
        className="modal-ok-button"
        icon={<CheckOutlined />}
        onClick={() => onCreate("PENDING")}
        loading={loading}
        disabled={loading || disabled}
      >
        Tạo đơn
      </Button>
    </div>
  );
};

export default Actions;