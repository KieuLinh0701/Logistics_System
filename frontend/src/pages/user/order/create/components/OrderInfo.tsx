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

const { Option } = Select;

interface Props {
  form: FormInstance;
  weight: number;
  orderValue: number;
  cod: number
  orderProducts: OrderProduct[];
  orderColumns: any[];
  serviceTypes?: ServiceType[];
  serviceLoading: boolean;
  selectedServiceType: ServiceType | null;
  setSelectedServiceType: (val: any) => void;
  onOpenProductModal: () => void;
  onChangeOrderInfo?: (changedValues: any) => void;
  disabled: boolean;
}

const OrderInfo: React.FC<Props> = ({
  form,
  weight,
  orderValue,
  cod,
  orderProducts,
  orderColumns,
  serviceTypes,
  serviceLoading,
  selectedServiceType,
  setSelectedServiceType,
  onOpenProductModal,
  onChangeOrderInfo,
  disabled,
}) => {

  const isOrderValueDisabled = !(orderProducts.length === 0);
  const isWeightDisabled = !(orderProducts.length === 0);

  // CHỈ set giá trị khi có sản phẩm hoặc giá trị > 0
  useEffect(() => {
    if (orderProducts.length > 0) {
      // const totalValue = orderProducts.reduce(
      //   (sum, p) => sum + p.product.price * p.quantity,
      //   0
      // );
      // const totalWeight = orderProducts.reduce(
      //   (sum, p) => sum + (p.product.weight || 0) * p.quantity,
      //   0
      // );

      form.setFieldsValue({
        // orderValue: totalValue,
        // weight: totalWeight,
      });
      onChangeOrderInfo?.({
        // orderValue: totalValue,
        // weight: totalWeight,
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

  // Thêm hàm xử lý change trực tiếp
  const handleWeightChange = (value: number | null) => {
    if (value !== null) {
      onChangeOrderInfo?.({ weight: value });
    } else {
      // Khi xóa giá trị, set về undefined để hiển thị placeholder
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
    <div className="rowContainerEdit">
      <Form
        form={form}
        layout="vertical"
        onValuesChange={(changedValues) => {
          console.log("🔄 onValuesChange được gọi với:", changedValues);
          onChangeOrderInfo?.(changedValues);
        }}
        initialValues={{
          weight: undefined,
          orderValue: undefined,
          codAmount: undefined,
          serviceType: undefined
        }}
      >
        <Card className="customCard">
          <div className="cardTitle">Thông tin đơn hàng</div>
          <Button
            icon={<PlusOutlined />}
            type="primary"
            disabled={disabled}
            style={{
              position: "absolute",
              top: 24,
              right: 24,
              zIndex: 5,
              color: "#ffffff",
              background: "#1C3D90",
            }}
            onClick={onOpenProductModal}
          >
            Chọn sản phẩm
          </Button>

          {orderProducts.length > 0 && (
            <Table<OrderProduct>
              dataSource={orderProducts}
              rowKey={(record) =>  "a"
                // String(record.product.id)
              }
              pagination={false}
              columns={orderColumns}
              style={{ marginBottom: 12, marginTop: 24 }}
            />
          )}

          <Row gutter={16} style={{ marginTop: 24 }}>
            <Col span={12}>
              <Form.Item
                name="weight"
                label="Khối lượng (kg)"
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
                  style={{ width: "100%" }}
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
                label="Loại dịch vụ giao hàng"
                rules={[{ required: true, message: "Chọn loại dịch vụ" }]}
              >
                <Select
                  placeholder="Chọn dịch vụ..."
                  disabled={disabled}
                  options={
                    serviceTypes?.map((s) => ({
                      label: s.name,
                      value: s.id,
                    })) || []
                  }
                  onChange={(value) => {
                    const selected = serviceTypes?.find((s) => s.id === value);
                    setSelectedServiceType(selected || null);
                    form.setFieldValue("serviceType", value);
                  }}
                  loading={serviceLoading}
                  allowClear
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="codAmount"
                label={
                  <span>
                    Tổng tiền thu hộ (COD){" "}
                    <Tooltip title="Số tiền khách hàng thanh toán khi nhận hàng (chưa bao gồm phí vận chuyển)">
                      <InfoCircleOutlined />
                    </Tooltip>
                  </span>
                }
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
                  style={{ width: "100%" }}
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
                label={
                  <span>
                    Tổng giá trị hàng hóa{" "}
                    <Tooltip title="Tổng giá trị sản phẩm trong đơn hàng (chưa bao gồm phí vận chuyển)">
                      <InfoCircleOutlined />
                    </Tooltip>
                  </span>
                }
                rules={[
                  {
                    required: true,
                    message: "Vui lòng nhập tổng giá trị hàng hóa",
                  },
                  {
                    type: "number",
                    min: 1, 
                    message: "Tổng giá trị hàng hóa phải lớn hơn 0",
                  },
                ]}
              >
                <InputNumber
                  style={{ width: "100%" }}
                  placeholder="Ví dụ: 150,000"
                  min={1} 
                  step={1000}
                  disabled={isOrderValueDisabled || disabled}
                  onChange={handleOrderValueChange}
                  formatter={value => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                  parser={value => value?.replace(/\$\s?|(,*)/g, '') as any}
                />
              </Form.Item>
            </Col>
          </Row>
        </Card>
      </Form>
    </div >
  );
};

export default OrderInfo;