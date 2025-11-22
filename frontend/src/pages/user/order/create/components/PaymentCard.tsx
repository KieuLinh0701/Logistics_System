import React from "react";
import { Card, Col, Form, Row, Select } from "antd";
import type { FormInstance } from "antd/lib";
import { translateOrderPayerType } from "../../../../../utils/orderUtils";

const { Option } = Select;

interface Props {
  form: FormInstance;
  payer?: string;
  payers: string[];
  paymentMethods: string[];
  paymentMethod?: string;
  disabled: boolean;
  onChangePayment?: (changedValues: any) => void;
}

const PaymentCard: React.FC<Props> = ({
  form,
  payer,
  payers,
  paymentMethod,
  paymentMethods,
  disabled,
  onChangePayment
}) => {
  return (
    <Card className="customCard">
      <div className="cardTitleEdit">Thanh toán</div>

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          payer: payer,
          paymentMethod: paymentMethod || "Cash",
        }}
        onValuesChange={(changedValues) => {

          // Nếu payer thay đổi thành Customer, reset paymentMethod về Cash
          if (changedValues.payer === "Customer") {
            form.setFieldsValue({ paymentMethod: "Cash" });
            
            // Gọi onChangePayment với cả 2 giá trị
            onChangePayment?.({
              payer: "Customer",
              paymentMethod: "Cash"
            });
            return;
          }

          // Nếu payer thay đổi thành Shop, reset paymentMethod về Cash
          if (changedValues.payer === "Shop") {
            form.setFieldsValue({ paymentMethod: "Cash" });
            
            // Gọi onChangePayment với cả 2 giá trị
            onChangePayment?.({
              payer: "Shop",
              paymentMethod: "Cash"
            });
            return;
          }

          // Các trường hợp khác
          onChangePayment?.(changedValues);
        }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="payer"
              label="Người trả phí"
              rules={[{ required: true, message: "Chọn người trả phí" }]}
            >
              <Select placeholder="Chọn người trả phí" disabled={disabled}>
                {payers.map((payer) => (
                  <Option key={payer} value={payer}>
                    {translateOrderPayerType(payer)}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              shouldUpdate={(prev, cur) => prev.payer !== cur.payer}
              noStyle
            >
              {({ getFieldValue }) => {
                const currentPayer = getFieldValue("payer");
                return (
                  <Form.Item
                    name="paymentMethod"
                    label="Phương thức thanh toán"
                    rules={[
                      { required: true, message: "Chọn phương thức thanh toán" },
                    ]}
                  >
                    <Select placeholder="Chọn phương thức thanh toán" disabled={disabled}>
                      {currentPayer === "Customer" ? (
                        <Option value="Cash">Tiền mặt</Option>
                      ) : (
                        paymentMethods.map((paymentMethod) => (
                          <Option key={paymentMethod} value={paymentMethod}>
                            {/* {translateOrderPaymentM(paymentMethod)} */} a
                          </Option>
                        ))
                      )}
                    </Select>
                  </Form.Item>
                );
              }}
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Card>
  );
};

export default PaymentCard;