import React from "react";
import { Modal, List, Button, Tag, Empty } from "antd";
import { EnvironmentOutlined, StarFilled } from "@ant-design/icons";
import type { Address } from "../../../../../types/address";

interface Props {
    open: boolean;
    addresses: Address[];
    onCancel: () => void;
    onSelect: (addr: Address) => void;
}

const RecipientAddressPickerModal: React.FC<Props> = ({
                                                          open,
                                                          addresses,
                                                          onCancel,
                                                          onSelect,
                                                      }) => {
    return (
        <Modal
            title="Chọn địa chỉ người nhận"
            open={open}
            onCancel={onCancel}
            footer={null}
            width={560}
        >
            {addresses?.length === 0 ? (
                <Empty
                    description="Chưa có địa chỉ người nhận đã lưu"
                    style={{ padding: "24px 0" }}
                />
            ) : (
                <List
                    dataSource={addresses}
                    renderItem={(addr) => (
                        <List.Item
                            key={addr.id}
                            style={{ cursor: "pointer", padding: "12px 0" }}
                            actions={[
                                <Button
                                    type="primary"
                                    size="small"
                                    onClick={() => onSelect(addr)}
                                >
                                    Chọn
                                </Button>,
                            ]}
                        >
                            <List.Item.Meta
                                avatar={
                                    <div
                                        style={{
                                            width: 36,
                                            height: 36,
                                            borderRadius: "50%",
                                            background: "#f0f0ff",
                                            display: "flex",
                                            alignItems: "center",
                                            justifyContent: "center",
                                            color: "#722ed1",
                                            fontSize: 16,
                                        }}
                                    >
                                        <EnvironmentOutlined />
                                    </div>
                                }
                                title={
                                    <span>
                                        {addr.name}{" "}
                                        <span style={{ color: "#8c8c8c", fontWeight: 400 }}>
                                            · {addr.phoneNumber}
                                        </span>
                                        {addr.isDefault && (
                                            <Tag
                                                icon={<StarFilled />}
                                                color="gold"
                                                style={{ marginLeft: 8, fontSize: 11 }}
                                            >
                                                Mặc định
                                            </Tag>
                                        )}
                                    </span>
                                }
                                description={
                                    <span style={{ fontSize: 12, color: "#8c8c8c" }}>
                                        {addr.detail}, {addr.wardName}, {addr.cityName}
                                    </span>
                                }
                            />
                        </List.Item>
                    )}
                />
            )}
        </Modal>
    );
};

export default RecipientAddressPickerModal;