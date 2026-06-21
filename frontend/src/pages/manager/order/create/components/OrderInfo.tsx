import React from "react";
import {Card, Col, Form, InputNumber, Row, Select, Tooltip,} from "antd";
import {InfoCircleOutlined} from "@ant-design/icons";
import type {FormInstance} from "antd/lib";
import type {ServiceType} from "../../../../../types/serviceType";

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

  const handleOrderValueChange = (value: number | null) => {
    onChangeOrderInfo?.({ orderValue: value ?? 0 });
  };

  return (
    <div className="create-order-card-container">
      <Form
        form={form}
        layout="vertical"
        onValuesChange={onChangeOrderInfo}
      >
        <Card className="create-order-custom-card">
          <div className="create-order-custom-card-title">Thông tin đơn hàng</div>
            <Row gutter={16} className="create-order-order-info">
                <Col span={6}>
                    <Form.Item
                        label={<span className="modal-label">Dài (cm)</span>}
                        name="length"
                        rules={[
                            {required: true, message: "Vui lòng nhập chiều dài"},
                            {
                                validator: (_, value) => {
                                    if (value !== undefined && value !== null && value !== '') {
                                        if (isNaN(value) || value <= 0) return Promise.reject(new Error("Phải lớn hơn 0"));
                                    }
                                    return Promise.resolve();
                                }
                            },
                        ]}
                    >
                        <InputNumber
                            className="modal-custom-input-number"
                            placeholder="Ví dụ: 30"
                            min={0.1}
                            step={0.1}
                        />
                    </Form.Item>
                </Col>

                <Col span={6}>
                    <Form.Item
                        label={<span className="modal-label">Rộng (cm)</span>}
                        name="width"
                        rules={[
                            {required: true, message: "Vui lòng nhập chiều rộng"},
                            {
                                validator: (_, value) => {
                                    if (value !== undefined && value !== null && value !== '') {
                                        if (isNaN(value) || value <= 0) return Promise.reject(new Error("Phải lớn hơn 0"));
                                    }
                                    return Promise.resolve();
                                }
                            },
                        ]}
                    >
                        <InputNumber
                            className="modal-custom-input-number"
                            placeholder="Ví dụ: 20"
                            min={0.1}
                            step={0.1}
                        />
                    </Form.Item>
                </Col>

                <Col span={6}>
                    <Form.Item
                        label={<span className="modal-label">Cao (cm)</span>}
                        name="height"
                        rules={[
                            {required: true, message: "Vui lòng nhập chiều cao"},
                            {
                                validator: (_, value) => {
                                    if (value !== undefined && value !== null && value !== '') {
                                        if (isNaN(value) || value <= 0) return Promise.reject(new Error("Phải lớn hơn 0"));
                                    }
                                    return Promise.resolve();
                                }
                            },
                        ]}
                    >
                        <InputNumber
                            className="modal-custom-input-number"
                            placeholder="Ví dụ: 15"
                            min={0.1}
                            step={0.1}
                        />
                    </Form.Item>
                </Col>

                <Col span={6}>
                    <Form.Item
                        label={<span className="modal-label">Khối lượng (kg)</span>}
                        name="originalWeight"
                        rules={[
                            {required: true, message: "Vui lòng nhập khối lượng"},
                            {
                                validator: (_, value) => {
                                    if (value !== undefined && value !== null && value !== '') {
                                        if (isNaN(value) || value <= 0) return Promise.reject(new Error("Khối lượng phải là số lớn hơn 0"));
                                    }
                                    return Promise.resolve();
                                }
                            },
                        ]}
                    >
                        <InputNumber
                            className="modal-custom-input-number"
                            placeholder="Ví dụ: 1.5"
                            min={0.01}
                            step={0.01}
                        />
                    </Form.Item>
                </Col>
            </Row>

            <Row gutter={16}>
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
              >
                <InputNumber
                  className="modal-custom-input-number"
                  placeholder={"Tự động tính..."}
                  disabled={true}
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