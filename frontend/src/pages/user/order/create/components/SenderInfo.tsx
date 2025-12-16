import React, { useEffect, useState } from "react";
import { Card, Button, Alert } from "antd";
import { EditOutlined } from "@ant-design/icons";
import AddressPickerModal from "./AddressPickerModal";
import type { Address } from "../../../../../types/address";
import locationApi from "../../../../../api/locationApi";
import { useNavigate } from "react-router-dom";

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
    existBankAccount: Boolean;
    onSelectAddress: (addr: Address) => void;
    onAdd: () => void;
    onEdit: (a: Address) => void;
    onDelete: (id: number) => void;
    onSetDefault: (id: number) => void;
}

const SenderInfo: React.FC<Props> = ({
    sender,
    addresses,
    existBankAccount,
    initialSelected,
    onSelectAddress,
    onAdd,
    onEdit,
    onDelete,
    onSetDefault,
}) => {
    
    const navigate = useNavigate();
    const [showModal, setShowModal] = useState(false);
    const [cityName, setCityName] = useState<string>('Unknown');
    const [wardName, setWardName] = useState<string>('Unknown');
    const [hasAddress, setHasAddress] = useState(false);

    useEffect(() => {
        const fetchLocationNames = async () => {
            if (addresses.length !== 0 && sender.cityCode && sender.wardCode) {
                try {
                    const city = await locationApi.getCityNameByCode(sender.cityCode);
                    const ward = await locationApi.getWardNameByCode(sender.cityCode, sender.wardCode);

                    setCityName(city || 'Unknown');
                    setWardName(ward || 'Unknown');
                    setHasAddress(true);
                } catch (error) {
                    console.error("Error fetching location names:", error);
                    setCityName('Unknown');
                    setWardName('Unknown');
                    setHasAddress(false);
                }
            } else {
                setCityName('Unknown');
                setWardName('Unknown');
                setHasAddress(false);
            }
        };

        fetchLocationNames();
    }, [sender, addresses]);

    return (
        <>
            {!existBankAccount && (
                <Alert
                    message="Chưa có tài khoản ngân hàng"
                    description="Bạn cần thêm tài khoản ngân hàng trong hồ sơ cá nhân để nhận tiền COD hoặc thanh toán khi tạo đơn hàng. Vui lòng cập nhật trước khi tiếp tục."
                    type="warning"
                    showIcon
                    className="create-order-alert-bank-account"
                    action={
                        <Button
                            type="primary"
                            className="modal-ok-button"
                            onClick={() => {
                                navigate("/bank-accounts");
                            }}
                        >
                            Cập nhật ngay
                        </Button>
                    }
                />
            )}

            <div className="create-order-card-container">
                <Card className="create-order-custom-card">
                    <div className="create-order-custom-card-title">Thông tin người gửi</div>

                    {hasAddress ? (
                        <div className="create-order-sender-info">
                            <p><strong>Tên:</strong> {sender.name}</p>
                            <p><strong>SĐT:</strong> {sender.phoneNumber}</p>
                            <p>
                                <strong>Địa chỉ:</strong> {sender.detail}, {wardName}, {cityName}
                            </p>
                        </div>
                    ) : (
                        <Alert
                            message="Chưa có địa chỉ"
                            description="Bạn cần cập nhật địa chỉ trong hồ sơ cá nhân để tiếp tục tạo đơn hàng"
                            type="warning"
                            showIcon
                            className="create-order-alert"
                        />
                    )}

                    <Button
                        className="create-order-btn"
                        icon={<EditOutlined />}
                        onClick={() => setShowModal(true)}
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
        </>
    );
};

export default SenderInfo;