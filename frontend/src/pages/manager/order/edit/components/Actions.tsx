import React from "react";
import { CloseOutlined, SaveOutlined } from "@ant-design/icons";
import { Button, Row, Col } from "antd";
import { canEditUserOrder, type OrderStatus } from "../../../../../utils/orderUtils";

interface Props {
  onEdit: () => void;
  onCancel: () => void;
  loading?: boolean;
  status: OrderStatus;
}

const Actions: React.FC<Props> = ({
  onEdit,
  onCancel,
  loading = false,
  status,
}) => {

  const handleEditClick = () => {
    onEdit(); 
  };

  return (
    <div className="create-order-actions-container">

      <Row className="create-order-buttons-container" gutter={8}>
        <Col span={12}>
          <Button
            block
            className="modal-cancel-button"
            icon={<CloseOutlined />}
            onClick={onCancel}
            loading={loading}
            disabled={loading || !canEditUserOrder(status as string)}
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