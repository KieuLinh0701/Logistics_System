import React, { useEffect } from "react";
import { Card, Col, Form, Input, Row } from "antd";
import type { FormInstance } from "antd/lib";
import AddressForm from "../../../../../components/common/AdressForm";
import { type OrderStatus } from "../../../../../utils/orderUtils";
import { canEditUserOrderField } from "../../../../../utils/userOrderEditRules";

interface Props {
    form: FormInstance;
    recipient: {
        name: string;
        phoneNumber: string;
        detail: string;
        wardCode: number;
        cityCode: number;
    };
    status: OrderStatus;
    onChange?: (values: any) => void;
}

const RecipientInfo: React.FC<Props> = ({
    form,
    recipient,
    onChange,
    status,
}) => {

    useEffect(() => {
        if (recipient) {
            form.setFieldsValue({
                name: recipient.name,
                phoneNumber: recipient.phoneNumber,
                recipient: {
                    cityCode: recipient.cityCode !== 0 ? recipient.cityCode : undefined,
                    wardCode: recipient.wardCode !== 0 ? recipient.wardCode : undefined,
                    detail: recipient.detail,
                }
            });
        }
    }, [recipient, form]);

    return (
        <div className="create-order-card-container">
            <Form
                form={form}
                layout="vertical"
                onValuesChange={(_, allValues) => {
                    onChange?.(allValues);
                }}
            >
                <Card className="create-order-custom-card">
                    <div className="create-order-custom-card-title">Thông tin người nhận</div>
                    <div className="create-order-content">
                        <Row gutter={16} >
                            <Col span={12}>
                                <Form.Item
                                    name="name"
                                    label={<span className="modal-lable">Tên người nhận</span>}
                                    rules={[{ required: true, message: "Vui lòng nhập tên" }]}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Nhập tên người nhận"
                                        disabled={!canEditUserOrderField('recipientName', status)} />
                                </Form.Item>

                                <Form.Item
                                    name="phoneNumber"
                                    label={<span className="modal-lable">Số điện thoại</span>}
                                    rules={[
                                        { required: true, message: "Vui lòng nhập số điện thoại" },
                                        {
                                            pattern: /^\d{10,11}$/,
                                            message: "Số điện thoại phải gồm 10 hoặc 11 chữ số",
                                        },
                                    ]}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Ví dụ: 0901234567"
                                        disabled={!canEditUserOrderField('recipientPhoneNumber', status)} />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <AddressForm
                                    form={form}
                                    prefix="recipient"
                                    disableCity={!canEditUserOrderField('recipientCityCode', status)}
                                    disableWard={!canEditUserOrderField('recipientWardCode', status)}
                                    disableDetailAddress={!canEditUserOrderField('recipientDetailAddress', status)}
                                />
                            </Col>
                        </Row>
                    </div>
                </Card>
            </Form>
        </div>
    );
};

export default RecipientInfo;