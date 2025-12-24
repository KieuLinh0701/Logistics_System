import React, { useEffect, useState } from "react";
import { CloseOutlined, SaveOutlined } from "@ant-design/icons";
import { Button, Row, Switch, Col } from "antd";
import { canEditUserOrder, type OrderStatus } from "../../../../../utils/orderUtils";

interface Props {
  onStatusChange: (status: "DRAFT" | "PENDING") => void;
  onEdit: () => void;
  onCancel: () => void;
  loading?: boolean;
  status: OrderStatus;
  userLocked: boolean;
}

const Actions: React.FC<Props> = ({
  onStatusChange,
  onEdit,
  onCancel,
  loading = false,
  status,
  userLocked
}) => {
  const [isOn, setIsOn] = useState(status === "PENDING");
  const [tempStatus, setTempStatus] = useState<"DRAFT" | "PENDING">(
    status === "PENDING" ? "PENDING" : "DRAFT"
  );

  useEffect(() => {
    setIsOn(status === "PENDING");
    setTempStatus(status === "PENDING" ? "PENDING" : "DRAFT");
  }, [status]);

  const handleToggle = (checked: boolean) => {
    if (userLocked) {
      setIsOn(false);
      setTempStatus("DRAFT");
      onStatusChange("DRAFT");
      return;
    }

    setIsOn(checked);
    const newStatus = checked ? "PENDING" : "DRAFT";
    setTempStatus(newStatus);
    onStatusChange(newStatus);
  };

  const handleEditClick = () => {
    onEdit();
  };

  return (
    <div className="create-order-actions-container">
      {status === "DRAFT" && (
        <Row className="create-order-switch-container" justify="space-between" align="middle">
          <Col>
            <span>Chuyển sang công khai</span>
          </Col>
          <Col>
            <Switch
              checked={isOn}
              onChange={handleToggle}
              checkedChildren=""
              unCheckedChildren=""
              className="custom-switch"
            />
          </Col>
        </Row>
      )}

      <Row className="create-order-buttons-container" gutter={8}>
        <Col span={12}>
          <Button
            block
            className="modal-cancel-button"
            icon={<CloseOutlined />}
            onClick={onCancel}
            loading={loading}
            disabled={loading  || !canEditUserOrder(status as string)}
          >
            Hủy
          </Button>
        </Col>
        <Col span={12}>
          <Button
            type="primary"
            block
            className="modal-ok-button"
            icon={<SaveOutlined />}
            onClick={handleEditClick}
            loading={loading}
            disabled={loading || !canEditUserOrder(status as string)}
          >
            Lưu thay đổi
          </Button>
        </Col>
      </Row>
    </div>
  );
};

export default Actions;