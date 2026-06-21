import React, {useEffect} from "react";
import {Card, Col, Form, Input, Row} from "antd";
import type {FormInstance} from "antd/lib";
import AddressForm from "../../../../../components/common/AdressForm";

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
    resetTrigger?: number;
    onChange?: (values: any) => void;
}

const SenderInfo: React.FC<Props> = ({ form, sender, onChange, resetTrigger }) => {
    const watchedName   = Form.useWatch("name", form);
    const watchedPhone  = Form.useWatch("phoneNumber", form);
    const watchedSender = Form.useWatch("sender", form);

    useEffect(() => {
        onChange?.(form.getFieldsValue(true));
    }, [watchedName, watchedPhone, watchedSender]);

    return (
        <div className="create-order-card-container">
            <Form
                form={form}
                layout="vertical"
                initialValues={{
                    name: sender.name,
                    phoneNumber: sender.phoneNumber,
                    sender: {
                        cityCode: sender.cityCode !== 0 ? sender.cityCode : undefined,
                        cityName: sender.cityName !== "" ? sender.cityName : undefined,
                        wardCode: sender.wardCode !== 0 ? sender.wardCode : undefined,
                        wardName: sender.wardName !== "" ? sender.wardName : undefined,
                        latitude: sender.latitude !== 0 ? sender.latitude : undefined,
                        longitude: sender.longitude !== 0 ? sender.longitude : undefined,
                        detail: sender.detail,
                    },
                }}
            >
                <Card className="create-order-custom-card">
                    <div className="create-order-custom-card-title">Thông tin người gửi</div>
                    <div className="create-order-content">
                        <Row gutter={16}>
                            <Col span={12}>
                                <Form.Item
                                    name="name"
                                    label={<span className="modal-lable">Tên người gửi</span>}
                                    rules={[{ required: true, message: "Vui lòng nhập tên" }]}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Nhập tên người gửi"
                                    />
                                </Form.Item>

                                <Form.Item
                                    name="phoneNumber"
                                    label={<span className="modal-lable">Số điện thoại</span>}
                                    rules={[
                                        { required: true, message: "Vui lòng nhập số điện thoại" },
                                        {
                                            pattern: /^\d{10}$/,
                                            message: "Số điện thoại phải đủ 10 số",
                                        },
                                    ]}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Ví dụ: 0901234567"
                                    />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <AddressForm
                                    form={form}
                                    prefix="sender"
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