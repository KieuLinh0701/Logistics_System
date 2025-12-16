import React, { useEffect, useState } from "react";
import { Card, Button } from "antd";
import { EditOutlined } from "@ant-design/icons";
import AddressPickerModal from "./AddressPickerModal";
import type { Address } from "../../../../../types/address";
import locationApi from "../../../../../api/locationApi";

interface Props {
    sender: {
        name: string;
        phoneNumber: string;
        detail: string;
        wardCode: number;
        cityCode: number;
    };
    initialSelected: Address | null;
    addresses: Address[];
    onSelectAddress: (addr: Address) => void;
    onAdd: () => void;
    onEdit: (a: Address) => void;
    onDelete: (id: number) => void;
    onSetDefault: (id: number) => void;
    onOpenAddressModal: () => void;
}

const SenderInfo: React.FC<Props> = ({
    sender,
    addresses,
    initialSelected,
    onSelectAddress,
    onAdd,
    onEdit,
    onDelete,
    onSetDefault,
    onOpenAddressModal,
}) => {

    const [showModal, setShowModal] = useState(false);
    const [cityName, setCityName] = useState<string>('Unknown');
    const [wardName, setWardName] = useState<string>('Unknown');

    useEffect(() => {
        const fetchLocationNames = async () => {
            if (sender.cityCode && sender.wardCode) {
                try {
                    const city = await locationApi.getCityNameByCode(sender.cityCode);
                    const ward = await locationApi.getWardNameByCode(sender.cityCode, sender.wardCode);

                    setCityName(city || 'Unknown');
                    setWardName(ward || 'Unknown');
                } catch (error) {
                    console.error("Error fetching location names:", error);
                    setCityName('Unknown');
                    setWardName('Unknown');
                }
            } else {
                setCityName('Unknown');
                setWardName('Unknown');
            }
        };

        fetchLocationNames();
    }, [sender]);

    return (
        <div className="create-order-card-container">
            <Card className="create-order-custom-card">
                <div className="create-order-custom-card-title">Thông tin người gửi</div>
                <div className="create-order-sender-info">
                    <p><strong>Tên:</strong> {sender.name}</p>
                    <p><strong>SĐT:</strong> {sender.phoneNumber}</p>
                    <p>
                        <strong>Địa chỉ:</strong> {sender.detail}, {wardName}, {cityName}
                    </p>
                </div>

                <Button
                    className="create-order-btn"
                    icon={<EditOutlined />}
                    onClick={() => {
                        setShowModal(true);
                        if (onOpenAddressModal) onOpenAddressModal();
                    }}
                >
                    Chọn địa chỉ
                </Button>
            </Card>

            <AddressPickerModal
                open={showModal}
                addresses={addresses}
                initialSelected={initialSelected}
                onCancel={() => setShowModal(false)}
                onSelect={(addr) => {
                    onSelectAddress(addr);
                    setShowModal(false);
                }}
                onAdd={onAdd}
                onEdit={onEdit}
                onDelete={onDelete}
                onSetDefault={onSetDefault}
            />
        </div>
    );
};

export default SenderInfo;