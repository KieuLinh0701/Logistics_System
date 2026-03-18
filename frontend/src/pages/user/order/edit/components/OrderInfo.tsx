import React, { useEffect, useMemo } from "react";
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
import { PlusOutlined, InfoCircleOutlined } from "@ant-design/icons";
import type { FormInstance } from "antd/lib";
import type { OrderProduct } from "../../../../../types/orderProduct";
import type { ServiceType } from "../../../../../types/serviceType";
import { type OrderStatus } from "../../../../../utils/orderUtils";
import { canEditUserOrderField } from "../../../../../utils/userOrderEditRules";

interface Props {
  form: FormInstance;
  codAmount?: number;
  weight?: number;
  adjustedWeight?: number;
  orderValue?: number;
  orderProducts: OrderProduct[];
  orderColumns: any[];
  serviceTypes?: ServiceType[];
  loading: boolean;
  setSelectedServiceType: (val: any) => void;
  onOpenProductModal: () => void;
  onChangeOrderInfo?: (changedValues: any) => void;
  selectedServiceType: ServiceType | null;
  status: OrderStatus;
}

const OrderInfo: React.FC<Props> = ({
  form,
  codAmount,
  weight,
  adjustedWeight,
  orderValue,
  orderProducts,
  orderColumns,
  serviceTypes,
  loading,
  setSelectedServiceType,
  onOpenProductModal,
  onChangeOrderInfo,
  selectedServiceType,
  status
}) => {
  const isOrderValueDisabled = !(orderProducts.length === 0);
  const isWeightDisabled = !(orderProducts.length === 0);

  const isAdjusted = useMemo(() => {
    return adjustedWeight != null;
  }, [adjustedWeight]);

  useEffect(() => {
    form.setFieldsValue({
      weight: weight ?? undefined,
      orderValue: orderValue ?? undefined,
      codAmount: codAmount ?? undefined,
      serviceType: selectedServiceType?.id ?? undefined,
    });
  }, [weight, orderValue, codAmount, selectedServiceType]);

  useEffect(() => {
    if (orderProducts.length === 0) return;

    const totalWeight = orderProducts.reduce(
      (sum, p) => sum + (p.productWeight * p.quantity),
      0
    );

    const totalValue = orderProducts.reduce(
      (sum, p) => sum + (p.productPrice * p.quantity),
      0
    );

    form.setFieldsValue({
      weight: totalWeight,
      orderValue: totalValue,
    });

    onChangeOrderInfo?.({
      weight: totalWeight,
      orderValue: totalValue,
    });
  }, [orderProducts]);

  const handleWeightChange = (value: number | null) => {
    const newValue = value ?? 0;
    onChangeOrderInfo?.({ weight: newValue });
  };

  const handleOrderValueChange = (value: number | null) => {
    const newValue = value ?? 0;
    onChangeOrderInfo?.({ orderValue: newValue });
  };

  const handleCodChange = (value: number | null) => {
    const newValue = value ?? 0;
    onChangeOrderInfo?.({ codAmount: newValue });
  };

  return (
    <div className="create-order-card-container">
      <Form
        form={form}
        layout="vertical"
        onValuesChange={(changedValues) => {
          onChangeOrderInfo?.(changedValues);
        }}
      >
        <Card className="create-order-custom-card">
          <div className="create-order-custom-card-title">Thông tin đơn hàng</div>

          <Button
            icon={<PlusOutlined />}
            disabled={!canEditUserOrderField('products', status)}
            className="create-order-btn"
            onClick={onOpenProductModal}
          >
            Chọn sản phẩm
          </Button>

          <div className="create-order-content">
            {orderProducts.length > 0 && (
              <Table<OrderProduct>
                dataSource={orderProducts}
                rowKey={(record) => String(record.productId)}
                scroll={{ x: "max-content" }}
                className="list-page-table"
                pagination={false}
                columns={orderColumns}
              />
            )}

            <Row gutter={16} className="create-order-order-info">
              <Col span={12}>
                <Form.Item
                  label={
                    <span className="modal-label">
                      Khối lượng quy đổi (kg){" "}
                      <Tooltip title="Khối lượng quy đổi = (Dài × Rộng × Cao) / 5000. So sánh với khối lượng thực tế và lấy giá trị lớn hơn để tính phí vận chuyển.">
                        <InfoCircleOutlined />
                      </Tooltip>
                    </span>
                  }
                  name="weight"
                  rules={[
                    { required: true, message: "Vui lòng nhập khối lượng" },
                    {
                      validator: (_, value) => {
                        if (value !== undefined && value <= 0) {
                          return Promise.reject(
                            new Error("Khối lượng phải lớn hơn 0")
                          );
                        }
                        return Promise.resolve();
                      },
                    },
                  ]}
                  extra={
                    isAdjusted && (
                      <div className="text-muted text-extra-time">
                        Đã khai báo:{" "}
                        <span className="custom-text-removed">
                          {weight?.toFixed(2)} kg
                        </span>
                        {"  "}–{"  "}
                        Đã điều chỉnh:{" "}
                        <b className="custom-table-content-error">
                          {adjustedWeight?.toFixed(2)} kg
                        </b>
                      </div>
                    )
                  }
                >
                  <InputNumber
                    className="modal-custom-input-number"
                    placeholder="Ví dụ: 1.5"
                    disabled={isWeightDisabled || !canEditUserOrderField('weight', status)}
                    onChange={handleWeightChange}
                    min={0.01}
                    step={0.01}
                    precision={2}
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
                    disabled={!canEditUserOrderField('serviceType', status)}
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
                      <Select.Option
                        key={s.id}
                        value={s.id}
                        label={s.name}
                      >
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
                  label={
                    <span className="modal-lable">
                      Giá trị thu hộ{" "}
                      <Tooltip title="Số tiền khách hàng thanh toán khi nhận hàng (chưa bao gồm phí vận chuyển)">
                        <InfoCircleOutlined />
                      </Tooltip>
                    </span>
                  }
                  name="codAmount"
                  rules={[
                    { required: true, message: "Vui lòng nhập tổng tiền thu hộ" },
                  ]}
                >
                  <InputNumber
                    className="modal-custom-input-number"
                    placeholder="Ví dụ: 200,000"
                    disabled={!canEditUserOrderField('cod', status)}
                    min={0}
                    step={1000}
                    onChange={handleCodChange}
                    formatter={(value) =>
                      value
                        ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                        : ""
                    }
                    parser={(value) => value?.replace(/\$\s?|(,*)/g, "") as any}
                  />
                </Form.Item>
              </Col>

              <Col span={12}>
                <Form.Item
                  name="orderValue"
                  label={
                    <span className="modal-lable">
                      Tổng giá trị hàng hóa{" "}
                      <Tooltip title="Giá trị đơn hàng dùng để tính phí bảo hiểm và bồi thường">
                        <InfoCircleOutlined />
                      </Tooltip>
                    </span>
                  }
                  rules={[
                    { required: true, message: "Nhập tổng giá trị hàng hóa" },
                  ]}
                >
                  <InputNumber
                    className="modal-custom-input-number"
                    placeholder="Ví dụ: 150,000"
                    min={0}
                    step={1000}
                    disabled={isOrderValueDisabled || !canEditUserOrderField('orderValue', status)}
                    onChange={handleOrderValueChange}
                    formatter={(value) =>
                      value
                        ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                        : ""
                    }
                    parser={(value) => value?.replace(/\$\s?|(,*)/g, "") as any}
                  />
                </Form.Item>
              </Col>
            </Row>
          </div>
        </Card>
      </Form>
    </div>
  );
};

export default OrderInfo;