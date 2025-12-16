import React, { useEffect } from "react";
import {
  Card,
  Row,
  Col,
  Form,
  InputNumber,
  Select,
  Button,
  Table,
  Tooltip,
} from "antd";
import {
  PlusOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons";
import type { FormInstance } from "antd/lib";
import type { OrderProduct } from "../../../../../types/orderProduct";
import type { ServiceType } from "../../../../../types/serviceType";

interface Props {
  form: FormInstance;
  weight: number | undefined;
  orderValue: number | undefined;
  orderProducts: OrderProduct[];
  orderColumns: any[];
  serviceTypes?: ServiceType[];
  loading: boolean;
  setSelectedServiceType: (val: any) => void;
  onOpenProductModal: () => void;
  onChangeOrderInfo?: (changedValues: any) => void;
  disabled: boolean;
}

const OrderInfo: React.FC<Props> = ({
  form,
  weight,
  orderValue,
  orderProducts,
  orderColumns,
  serviceTypes,
  loading,
  setSelectedServiceType,
  onOpenProductModal,
  onChangeOrderInfo,
  disabled,
}) => {

  const isOrderValueDisabled = !(orderProducts.length === 0);
  const isWeightDisabled = !(orderProducts.length === 0);

  useEffect(() => {
    if (orderProducts.length > 0) {
      form.setFieldsValue({
        orderValue: orderValue,
        weight: weight,
      });
      onChangeOrderInfo?.({
        orderValue: orderValue,
        weight: weight,
      });
    } else {
      form.setFieldsValue({
        orderValue: undefined,
        weight: undefined,
      });
      onChangeOrderInfo?.({
        orderValue: 0,
        weight: 0,
      });
    }
  }, [orderProducts, form]);

  const handleWeightChange = (value: number | null) => {
    if (value !== null) {
      onChangeOrderInfo?.({ weight: value });
    } else {
      form.setFieldValue('weight', undefined);
      onChangeOrderInfo?.({ weight: 0 });
    }
  };

  const handleOrderValueChange = (value: number | null) => {
    if (value !== null) {
      onChangeOrderInfo?.({ orderValue: value });
    } else {
      form.setFieldValue('orderValue', undefined);
      onChangeOrderInfo?.({ orderValue: 0 });
    }
  };

  const handleCodChange = (value: number | null) => {
    if (value !== null) {
      onChangeOrderInfo?.({ codAmount: value });
    } else {
      form.setFieldValue('codAmount', undefined);
      onChangeOrderInfo?.({ codAmount: 0 });
    }
  };

  return (
    <div className="create-order-card-container">
      <Form
        form={form}
        layout="vertical"
        onValuesChange={(changedValues) => {
          onChangeOrderInfo?.(changedValues);
        }}
        initialValues={{
          weight: undefined,
          orderValue: undefined,
          codAmount: undefined,
          serviceType: undefined
        }}
      >
        <Card className="create-order-custom-card">
          <div className="create-order-custom-card-title">Thông tin đơn hàng</div>
          <Button
            icon={<PlusOutlined />}
            disabled={disabled}
            className="create-order-btn"
            onClick={onOpenProductModal}
          >
            Chọn sản phẩm
          </Button>

          <div className="create-order-content">

            {orderProducts.length > 0 && (
              <Table<OrderProduct>
                dataSource={orderProducts}
                rowKey={(record) =>
                  String(record.productId)
                }
                scroll={{ x: "max-content" }}
                className="list-page-table"
                pagination={false}
                columns={orderColumns}
              />
            )}

            <Row gutter={16}
              className="create-order-order-info">
              <Col span={12}>
                <Form.Item
                  label={
                    <span className="modal-label">
                      Khối lượng quy đổi (kg){" "}
                      <Tooltip
                        title={
                          "Khối lượng quy đổi = (Dài × Rộng × Cao) / 5000. So sánh với khối lượng thực tế và lấy giá trị lớn hơn để tính phí vận chuyển."
                        }
                      >
                        <InfoCircleOutlined />
                      </Tooltip>
                    </span>
                  }
                  name="weight"
                  rules={[
                    {
                      required: true,
                      message: "Vui lòng nhập khối lượng",
                    },
                    {
                      validator: (_, value) => {
                        // Chỉ kiểm tra khi có giá trị
                        if (value !== undefined && value !== null && value !== '') {
                          if (isNaN(value) || value <= 0) {
                            return Promise.reject(new Error("Khối lượng phải là số lớn hơn 0"));
                          }
                        }
                        return Promise.resolve();
                      },
                    },
                  ]}
                  validateTrigger={['onChange', 'onBlur']}
                >
                  <InputNumber
                    className="modal-custom-input-number"
                    placeholder="Ví dụ: 1.5"
                    disabled={isWeightDisabled || disabled}
                    onChange={handleWeightChange}
                    min={0.01}
                    step={0.01}
                  />
                </Form.Item>
              </Col>

              <Col span={12}>
                <Form.Item
                  name="serviceType"
                  label={<span className="modal-lable">Loại dịch vụ giao hàng</span>}
                  rules={[{ required: true, message: "Chọn loại dịch vụ" }]}
                >
                  <Select
                    className="modal-custom-select"
                    placeholder="Chọn dịch vụ..."
                    disabled={disabled}
                    showSearch
                    optionLabelProp="label"
                    filterOption={(input, option) =>
                      (option?.label as string)
                        ?.toLowerCase()
                        .includes(input.toLowerCase())
                    }
                    loading={loading}
                    allowClear
                    onChange={(value) => {
                      const selected = serviceTypes?.find((s) => s.id === value);
                      setSelectedServiceType(selected || null);
                      form.setFieldValue("serviceType", value);
                    }}
                  >
                    {serviceTypes?.map((s) => (
                      <Select.Option key={s.id} value={s.id} label={s.name}>
                        <div className="create-order-pickup-type office-contain">
                          <span className="create-order-pickup-type office-name">
                            {s.name}
                          </span>
                          <span className="create-order-pickup-type office-address">
                            ( {s.deliveryTime} )
                          </span>
                        </div>
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label={<span className="modal-lable">
                    Giá trị thu hộ{" "}
                    <Tooltip title="Là tiền bên gửi nhờ thu hộ của bên nhận và chuyển về tài khoản ngân hàng của bên gửi">
                      <InfoCircleOutlined />
                    </Tooltip>
                  </span>}
                  name="codAmount"
                  rules={[
                    { required: true, message: "Vui lòng nhập tổng tiền thu hộ" },
                    {
                      type: "number",
                      min: 0,
                      message: "Nhập số tiền thu hộ hợp lệ"
                    },
                  ]}
                >
                  <InputNumber
                    className="modal-custom-input-number"
                    placeholder="Ví dụ: 200,000"
                    disabled={disabled}
                    min={0}
                    step={1000}
                    onChange={handleCodChange}
                    formatter={value => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                    parser={value => value?.replace(/\$\s?|(,*)/g, '') as any}
                  />
                </Form.Item>
              </Col>

              <Col span={12}>
                <Form.Item
                  name="orderValue"
                  label={<span className="modal-lable">
                    Tổng giá trị hàng hóa{" "}
                    <Tooltip title="Giá trị đơn hàng dùng để giúp bạn được đền bù 100% nếu hàng gặp sự cố">
                      <InfoCircleOutlined />
                    </Tooltip></span>}
                  rules={[
                    {
                      required: true,
                      message: "Vui lòng nhập tổng giá trị hàng hóa",
                    },
                    {
                      type: "number",
                      min: 0,
                      message: "Nhập tổng giá trị hàng hóa hợp lệ",
                    },
                  ]}
                >
                  <InputNumber
                    className="modal-custom-input-number"
                    placeholder="Ví dụ: 150,000"
                    min={0}
                    step={1000}
                    disabled={isOrderValueDisabled || disabled}
                    onChange={handleOrderValueChange}
                    formatter={value => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                    parser={value => value?.replace(/\$\s?|(,*)/g, '') as any}
                  />
                </Form.Item>
              </Col>
            </Row>
          </div>
        </Card>
      </Form>
    </div >
  );
};

export default OrderInfo;