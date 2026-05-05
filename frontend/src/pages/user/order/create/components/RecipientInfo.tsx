import React, {useEffect} from "react";
import { Card, Col, Form, Input, Row } from "antd";
import type { FormInstance } from "antd/lib";
import AddressForm from "../../../../../components/common/AdressForm";

interface Props {
    form: FormInstance;
    recipient: {
        name: string;
        phoneNumber: string;
        detail: string;
        wardCode: number;
        wardName: string,
        cityCode: number,
        cityName: string,
        latitude: number,
        longitude: number,
        fullAddress: string
    };
    disabled: boolean;
    onChange?: (values: any) => void;
}

const RecipientInfo: React.FC<Props> = ({
    form,
    recipient,
    disabled,
    onChange
}) => {

    const recipientValues = Form.useWatch('recipient', form);

    useEffect(() => {
        onChange?.(form.getFieldsValue(true));
    }, [recipientValues]);

    return (
        <div className="create-order-card-container">
            <Form
                form={form}
                layout="vertical"
                initialValues={{
                    recipientName: recipient.name,
                    recipientPhone: recipient.phoneNumber,
                    recipient: {
                        cityCode: recipient.cityCode !== 0 ? recipient.cityCode : undefined,
                        cityName: recipient.cityName,
                        wardCode: recipient.wardCode !== 0 ? recipient.wardCode : undefined,
                        wardName: recipient.wardName,
                        latitude: recipient.latitude,
                        longitude: recipient.longitude,
                        detail: recipient.detail,
                    }
                }}
                onValuesChange={(_, allValues) => {
                    onChange?.(form.getFieldsValue(true));
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
                                        disabled={disabled} />
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
                                        disabled={disabled} />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <AddressForm
                                    form={form}
                                    prefix="recipient"
                                    disableCity={disabled}
                                    disableDetailAddress={disabled}
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