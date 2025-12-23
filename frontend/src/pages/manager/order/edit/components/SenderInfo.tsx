import React, { useEffect } from "react";
import { Card, Col, Form, Input, Row } from "antd";
import type { FormInstance } from "antd/lib";
import AddressForm from "../../../../../components/common/AdressForm";
import { type OrderCreatorType, type OrderStatus } from "../../../../../utils/orderUtils";
import { canManagerEditOrderField } from "../../../../../utils/managerOrderEditRules";

interface Props {
    form: FormInstance;
    sender: {
        name: string;
        phoneNumber: string;
        detail: string;
        wardCode: number;
        cityCode: number;
    };
    status: OrderStatus;
    creator: OrderCreatorType;
    onChange?: (values: any) => void;
}

const SenderInfo: React.FC<Props> = ({
    form,
    sender,
    onChange,
    status,
    creator,
}) => {

    useEffect(() => {
        if (sender) {
            form.setFieldsValue({
                name: sender.name,
                phoneNumber: sender.phoneNumber,
                sender: {
                    cityCode: sender.cityCode !== 0 ? sender.cityCode : undefined,
                    wardCode: sender.wardCode !== 0 ? sender.wardCode : undefined,
                    detail: sender.detail,
                }
            });
        }
    }, [sender, form]);

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
                    <div className="create-order-custom-card-title">Thông tin người gửi</div>
                    <div className="create-order-content">
                        <Row gutter={16} >
                            <Col span={12}>
                                <Form.Item
                                    name="name"
                                    label={<span className="modal-lable">Tên người gửi</span>}
                                    rules={[{ required: true, message: "Vui lòng nhập tên" }]}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Nhập tên người gửi"
                                        disabled={!canManagerEditOrderField('senderName', status, creator)} />
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
                                        disabled={!canManagerEditOrderField('senderPhoneNumber', status, creator)} />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <AddressForm
                                    form={form}
                                    prefix="sender"
                                    disableCity={!canManagerEditOrderField('senderCityCode', status, creator)}
                                    disableWard={!canManagerEditOrderField('senderWardCode', status, creator)}
                                    disableDetailAddress={!canManagerEditOrderField('senderDetailAddress', status, creator)}
                                />
                            </Col>
                        </Row>
                    </div>
                </Card>
            </Form>
        </div>
    );
};

export default SenderInfo;