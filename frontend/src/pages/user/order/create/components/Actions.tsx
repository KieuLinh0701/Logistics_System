import React from "react";
import { CheckOutlined, SaveOutlined } from "@ant-design/icons";
import { Button } from "antd";

interface Props {
  onCreate: (status: "draft" | "pending") => void;
  loading?: boolean;
  disabled: boolean;
}

const Actions: React.FC<Props> = ({
  onCreate,
  loading = false,
  disabled,
}) => {
  return (
    <div style={{ display: "flex", gap: 8, justifyContent: "space-between" }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          width: "100%",
          gap: "8px",
        }}
      >
        <Button
          block
          style={{ borderColor: "#1C3D90", color: "#1C3D90" }}
          icon={<SaveOutlined />}
          onClick={() => onCreate("draft")}
          loading={loading}
          disabled={loading || disabled}
        >
          Lưu nháp
        </Button>

        <Button
          type="primary"
          block
          style={{ background: "#1C3D90", color: "#ffffff" }}
          icon={<CheckOutlined />}
          onClick={() => onCreate("pending")}
          loading={loading}
          disabled={loading || disabled}
        >
          Tạo đơn
        </Button>
      </div>
    </div>
  );
};

export default Actions;