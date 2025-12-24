import React, { useEffect } from "react";
import { Card, Form, Radio } from "antd";
import type { FormInstance } from "antd/lib";
import { ORDER_PAYER_TYPES, translateOrderPayerType, type OrderStatus } from "../../../../../utils/orderUtils";
import { canEditUserOrderField } from "../../../../../utils/userOrderEditRules";

interface Props {
  form: FormInstance;
  payer: string;
  onChangePayment?: (changedValues: any) => void;
  status: OrderStatus;
}

const PaymentCard: React.FC<Props> = ({
  form,
  payer,
  onChangePayment,
  status,
}) => {

  useEffect(() => {
    form.setFieldsValue({ payer: payer || "CUSTOMER" });
  }, [payer, form]);

  return (
    <div className="create-order-card-container">
      <Card className="create-order-custom-card">
        <div className="create-order-custom-card-title">Người trả phí</div>

        <Form
          className="create-order-form"
          form={form}
          layout="vertical"
          onValuesChange={(changedValues) => {
            onChangePayment?.(changedValues);
          }}
        >
          <Form.Item
            name="payer"
            rules={[{ required: true, message: "Chọn người trả phí" }]}
          >
            <Radio.Group
              disabled={!canEditUserOrderField('payer', status)}
              className="custom-radio-group"
            >
              {ORDER_PAYER_TYPES.map((payerType) => (
                <Radio
                  key={payerType}
                  value={payerType}
                  className="custom-radio"
                >
                  <span className="custom-radio-label">
                    {translateOrderPayerType(payerType)}
                  </span>
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default PaymentCard;