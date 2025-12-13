import React from "react";
import {
  Card,
  Row,
  Col,
  Form,
  InputNumber,
  Select,
  Tooltip,
} from "antd";
import { InfoCircleOutlined } from "@ant-design/icons";
import type { FormInstance } from "antd/lib";
import type { ServiceType } from "../../../../../types/serviceType";

interface Props {
  form: FormInstance;
  serviceTypes?: ServiceType[];
  loading: boolean;
  setSelectedServiceType: (val: any) => void;
  onChangeOrderInfo?: (changedValues: any) => void;
}

const OrderInfo: React.FC<Props> = ({
  form,
  serviceTypes,
  loading,
  setSelectedServiceType,
  onChangeOrderInfo,
}) => {

  const handleWeightChange = (value: number | null) => {
    onChangeOrderInfo?.({ weight: value ?? 0 });
  };

  const handleOrderValueChange = (value: number | null) => {
    onChangeOrderInfo?.({ orderValue: value ?? 0 });
  };

  const handleCodChange = (value: number | null) => {
    onChangeOrderInfo?.({ codAmount: value ?? 0 });
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

          <Row gutter={16} className="create-order-order-info">
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
                  { required: true, message: "Vui lòng nhập khối lượng" },
                  {
                    validator: (_, value) => {
                      if (value !== undefined && value !== null && value <= 0) {
                        return Promise.reject(new Error("Khối lượng phải lớn hơn 0"));
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
                name="orderValue"
                label={<span className="modal-lable">
                  Tổng giá trị hàng hóa{" "}
                  <Tooltip title="Giá trị đơn hàng dùng để tính phí bảo hiểm và hỗ trợ bồi thường khi xảy ra sự cố">
                    <InfoCircleOutlined />
                  </Tooltip>
                </span>}
                rules={[
                  { required: true, message: "Vui lòng nhập tổng giá trị hàng hóa" },
                  { type: "number", min: 0, message: "Nhập giá trị hợp lệ" },
                ]}
              >
                <InputNumber
                  className="modal-custom-input-number"
                  placeholder="Ví dụ: 150,000"
                  min={0}
                  step={1000}
                  onChange={handleOrderValueChange}
                  formatter={value => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                  parser={value => value?.replace(/\$\s?|(,*)/g, '') as any}
                />
              </Form.Item>
            </Col>
          </Row>
        </Card>
      </Form>
    </div>
  );
};

export default OrderInfo;