import React, {useEffect, useRef} from "react";
import {Card, Col, Form, Input, Row} from "antd";
import type {FormInstance} from "antd/lib";
import AddressForm from "../../../../../components/common/AdressForm";
import {type OrderCreatorType, type OrderStatus} from "../../../../../utils/orderUtils";
import {canManagerEditOrderField} from "../../../../../utils/managerOrderEditRules";

interface Props {
    form: FormInstance;
    sender: {
        name: string;
        phoneNumber: string;
        detail: string;
        wardCode: number;
        wardName: string;
        cityCode: number;
        cityName: string;
        latitude: number;
        longitude: number;
    };
    status: OrderStatus;
    creator: OrderCreatorType;
    onChange?: (values: any) => void;
}

const SenderInfo: React.FC<Props> = ({form, sender, onChange, status, creator}) => {
    const prevRef = useRef<string>("");

    // Set lại form khi prop sender thay đổi — giống RecipientInfo
    useEffect(() => {
        if (!sender) return;

        const key = JSON.stringify(sender);
        if (prevRef.current === key) return;
        prevRef.current = key;

        form.setFieldsValue({
            name: sender.name,
            phoneNumber: sender.phoneNumber,
            sender: {
                cityCode: sender.cityCode !== 0 ? sender.cityCode : undefined,
                wardCode: sender.wardCode !== 0 ? sender.wardCode : undefined,
                detail: sender.detail,
                cityName: sender.cityName,
                wardName: sender.wardName,
                latitude: sender.latitude,
                longitude: sender.longitude,
            },
        });
    }, [sender, form]);

    return (
        <div className="create-order-card-container">
            <Form
                form={form}
                layout="vertical"
                // Giống RecipientInfo: init 1 lần, update qua useEffect
                initialValues={{
                    name: sender.name,
                    phoneNumber: sender.phoneNumber,
                    sender: {
                        cityCode: sender.cityCode !== 0 ? sender.cityCode : undefined,
                        cityName: sender.cityName,
                        wardCode: sender.wardCode !== 0 ? sender.wardCode : undefined,
                        wardName: sender.wardName,
                        latitude: sender.latitude,
                        longitude: sender.longitude,
                        detail: sender.detail,
                    },
                }}
                // Giống RecipientInfo: notify parent mỗi khi bất kỳ field nào đổi
                onValuesChange={() => onChange?.(form.getFieldsValue(true))}
            >
                <Card className="create-order-custom-card">
                    <div className="create-order-custom-card-title">Thông tin người gửi</div>
                    <div className="create-order-content">
                        <Row gutter={16}>
                            <Col span={12}>
                                <Form.Item
                                    name="name"
                                    label={<span className="modal-lable">Tên người gửi</span>}
                                    rules={[{required: true, message: "Vui lòng nhập tên"}]}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Nhập tên người gửi"
                                        disabled={!canManagerEditOrderField('senderName', status, creator)}
                                    />
                                </Form.Item>

                                <Form.Item
                                    name="phoneNumber"
                                    label={<span className="modal-lable">Số điện thoại</span>}
                                    rules={[
                                        {required: true, message: "Vui lòng nhập số điện thoại"},
                                        {
                                            pattern: /^\d{10,11}$/,
                                            message: "Số điện thoại phải gồm 10 hoặc 11 chữ số"
                                        },
                                    ]}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Ví dụ: 0901234567"
                                        disabled={!canManagerEditOrderField('senderPhoneNumber', status, creator)}
                                    />
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