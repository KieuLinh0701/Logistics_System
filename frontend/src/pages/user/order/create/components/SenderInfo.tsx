import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Card, Col, Form, Input, Row, Button, Alert } from "antd";
import { EditOutlined, RollbackOutlined, UserOutlined } from "@ant-design/icons";
import type { FormInstance } from "antd/lib";
import type { City, Ward } from "../../../../../types/location";

interface Props {
    form: FormInstance; 
    sender: {
        name: string;
        phone: string;
        detailAddress: string;
        wardCode: number;
        cityCode: number;
    };
    user?: {
        firstName: string;
        lastName: string;
        phoneNumber: string;
        detailAddress: string;
        codeWard: number;
        codeCity: number;
        role: string;
    };
    cityList?: City[];
    wardList?: Ward[];
    onChange?: (newSender: {
        name: string;
        phone: string;
        detailAddress: string;
        wardCode: number;
        cityCode: number;
    }) => void;
}

const SenderInfo: React.FC<Props> = ({
    form, 
    sender,
    user,
    cityList = [],
    wardList = [],
    onChange,
}) => {
    const navigate = useNavigate();
    const [useUserView, setUseUserView] = useState(true); 
    
    // Thêm state để lưu giá trị khi chỉnh sửa
    const [editedValues, setEditedValues] = useState<{
        name: string;
        phone: string;
        detailAddress: string;
        wardCode: number;
        cityCode: number;
    } | null>(null);

    const senderFormValues = {
        senderName: sender.name,
        senderPhone: sender.phone,
        sender: {
            province: sender.cityCode,
            commune: sender.wardCode,
            address: sender.detailAddress,
        },
    };

    const userFormValues = user
        ? {
            senderName: `${user.lastName} ${user.firstName}`,
            senderPhone: user.phoneNumber,
            sender: {
                province: user.codeCity,
                commune: user.codeWard,
                address: user.detailAddress,
            },
        }
        : senderFormValues;

    // Kiểm tra user có địa chỉ đầy đủ không
    const hasCompleteAddress = user && 
        user.detailAddress && 
        user.codeWard && 
        user.codeCity;

    useEffect(() => {
        // Chỉ set giá trị user khi component mount và đang ở chế độ user view
        if (user && useUserView) {
            form.setFieldsValue(userFormValues);
            if (onChange) {
                onChange({
                    name: userFormValues.senderName,
                    phone: userFormValues.senderPhone,
                    detailAddress: userFormValues.sender.address,
                    wardCode: userFormValues.sender.commune,
                    cityCode: userFormValues.sender.province,
                });
            }
        }
    }, [user, form, useUserView]); // Thêm useUserView vào dependency

    const handleValuesChange = (_: any, allValues: any) => {
        if (!useUserView) { // Chỉ gửi onChange khi đang ở chế độ chỉnh sửa
            const newValues = {
                name: allValues.senderName || "",
                phone: allValues.senderPhone || "",
                detailAddress: allValues.sender?.address || "",
                wardCode: allValues.sender?.commune || 0,
                cityCode: allValues.sender?.province || 0,
            };
            
            // Lưu giá trị đã chỉnh sửa
            setEditedValues(newValues);
            
            if (onChange) {
                onChange(newValues);
            }
        }
    };

    const handleToggleUserInfo = () => {
        const newUseUserView = !useUserView;
        setUseUserView(newUseUserView);

        if (newUseUserView) {
            // Chuyển về dùng thông tin user
            form.setFieldsValue(userFormValues);
            if (onChange) {
                onChange({
                    name: userFormValues.senderName,
                    phone: userFormValues.senderPhone,
                    detailAddress: userFormValues.sender.address,
                    wardCode: userFormValues.sender.commune,
                    cityCode: userFormValues.sender.province,
                });
            }
        } else {
            // Chuyển sang chế độ chỉnh sửa
            // Sử dụng giá trị đã chỉnh sửa trước đó hoặc giá trị user
            const valuesToSet = editedValues 
                ? {
                    senderName: editedValues.name,
                    senderPhone: editedValues.phone,
                    sender: {
                        province: editedValues.cityCode,
                        commune: editedValues.wardCode,
                        address: editedValues.detailAddress,
                    },
                }
                : userFormValues;
                
            form.setFieldsValue(valuesToSet);
            
            if (onChange) {
                onChange({
                    name: valuesToSet.senderName,
                    phone: valuesToSet.senderPhone,
                    detailAddress: valuesToSet.sender.address,
                    wardCode: valuesToSet.sender.commune,
                    cityCode: valuesToSet.sender.province,
                });
            }
        }
    };

    const handleNavigateToProfile = () => {
        if (user) {
            navigate(`/${user.role}/orders/create/edit-profile`); 
        }
    };

    // Reset form khi user thay đổi
    useEffect(() => {
        if (useUserView && user) {
            form.setFieldsValue(userFormValues);
            setEditedValues(null); // Reset edited values khi quay lại dùng user info
        }
    }, [user]);

    return (
        <div className="rowContainerEdit">
            <Form
                form={form}
                layout="vertical"
                onValuesChange={handleValuesChange}
                initialValues={userFormValues} // Set initial values từ user
            >
                <Card className="customCard">
                    <div className="cardTitle">Thông tin người gửi</div>

                    {useUserView ? (
                        <>
                            <div
                                style={{
                                    padding: "0 16px",
                                    lineHeight: 1.6,
                                    marginTop: 16,
                                }}
                            >
                                <p><strong>Họ tên:</strong> {userFormValues.senderName}</p>
                                <p><strong>SĐT:</strong> {userFormValues.senderPhone}</p>
                                
                                {hasCompleteAddress ? (
                                    <p>
                                        <strong>Địa chỉ:</strong> {userFormValues.sender.address}
                                        {userFormValues.sender.commune && (
                                            <>, {wardList.find((w) => w.code === userFormValues.sender.commune)?.name || ""}</>
                                        )}
                                        {userFormValues.sender.province && (
                                            <>, {cityList.find((p) => p.code === userFormValues.sender.province)?.name || ""}</>
                                        )}
                                    </p>
                                ) : (
                                    <Alert
                                        message="Chưa có địa chỉ"
                                        description="Bạn cần cập nhật địa chỉ trong hồ sơ cá nhân để tiếp tục tạo đơn hàng"
                                        type="warning"
                                        showIcon
                                        style={{ margin: "16px 0" }}
                                    />
                                )}
                            </div>
                            
                            {!hasCompleteAddress ? (
                                <Button
                                    className="buttonEdit"
                                    icon={<UserOutlined />}
                                    onClick={handleNavigateToProfile}
                                    type="primary"
                                >
                                    Cập nhật địa chỉ trong hồ sơ
                                </Button>
                            ) : (
                                <Button
                                    className="buttonEdit"
                                    icon={<EditOutlined />}
                                    onClick={handleToggleUserInfo}
                                >
                                    Thay đổi địa chỉ cho đơn hàng
                                </Button>
                            )}
                        </>
                    ) : (
                        <>
                            {user && (
                                <Button
                                    className="buttonEdit"
                                    icon={<RollbackOutlined />}
                                    onClick={handleToggleUserInfo}
                                    type="primary"
                                >
                                    Sử dụng thông tin cá nhân
                                </Button>
                            )}
                            
                            <Row gutter={16} style={{ marginTop: 30 }}>
                                <Col span={12}>
                                    <Form.Item
                                        name="senderName"
                                        label="Tên người gửi"
                                        rules={[
                                            {
                                                required: true,
                                                message: "Vui lòng nhập tên người gửi",
                                            },
                                        ]}
                                        validateTrigger={['onChange', 'onBlur']}
                                    >
                                        <Input placeholder="Nhập tên người gửi" />
                                    </Form.Item>

                                    <Form.Item
                                        name="senderPhone"
                                        label="Số điện thoại"
                                        rules={[
                                            {
                                                required: true,
                                                message: "Vui lòng nhập số điện thoại",
                                            },
                                            {
                                                pattern: /^\d{10}$/,
                                                message: "Số điện thoại phải đủ 10 số",
                                            },
                                        ]}
                                        validateTrigger={['onChange', 'onBlur']}
                                    >
                                        <Input placeholder="Ví dụ: 0901234567" />
                                    </Form.Item>
                                </Col>

                                <Col span={12}>
                                    {/* <AddressForm
                                        form={form}
                                        prefix="sender"
                                        disableCity={false} 
                                    /> */}
                                </Col>
                            </Row>
                        </>
                    )}
                </Card>
            </Form>
        </div>
    );
};

export default SenderInfo;