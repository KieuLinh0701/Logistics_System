import React from "react";
import { Card, Col, Form, Input, Row } from "antd";
import type { FormInstance } from "antd/lib";

interface Props {
    form: FormInstance; 
    recipient: {
        name: string;
        phone: string;
        detailAddress: string;
        wardCode: number;
        cityCode: number;
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
    return (
        <div className="rowContainerEdit">
            <Form
                form={form}
                layout="vertical"
                initialValues={{
                    recipientName: recipient.name,
                    recipientPhone: recipient.phone,
                    recipient: {
                        province: recipient.cityCode !== 0 ? recipient.cityCode : undefined,
                        commune: recipient.wardCode !== 0 ? recipient.wardCode : undefined,
                        address: recipient.detailAddress,
                    }
                }}
                onValuesChange={(_, allValues) => {
                    onChange?.(allValues); 
                }}
            >
                <Card className="customCard">
                    <div className="cardTitle">Thông tin người nhận</div>

                    <Row gutter={16} >
                        <Col span={12}>
                            <Form.Item
                                name="recipientName"
                                label="Tên người nhận"
                                rules={[{ required: true, message: "Vui lòng nhập tên" }]}
                            >
                                <Input placeholder="Nhập tên người nhận" disabled={disabled}/>
                            </Form.Item>

                            <Form.Item
                                name="recipientPhone"
                                label="Số điện thoại"
                                rules={[
                                    { required: true, message: "Vui lòng nhập số điện thoại" },
                                    {
                                        pattern: /^\d{10}$/,
                                        message: "Số điện thoại phải đủ 10 số",
                                    },
                                ]}
                            >
                                <Input placeholder="Ví dụ: 0901234567" disabled={disabled}/>
                            </Form.Item>
                        </Col>

                        <Col span={12}>
                            {/* <AddressForm 
                                form={form}
                                prefix="recipient"
                                disableCity={disabled} 
                                disableDetailAddress={disabled} 
                            /> */}
                        </Col>
                    </Row>
                </Card>
            </Form>
        </div>
    );
};

export default RecipientInfo;