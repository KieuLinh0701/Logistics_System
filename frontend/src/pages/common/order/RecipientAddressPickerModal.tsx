import React, {useEffect, useState} from "react";
import {Modal, List, Empty, message, Radio} from "antd";
import recipientAddressApi from "../../../api/recipientAddressApi.ts";
import type {RecipientAddressWithStats} from "../../../types/recipientAddress.ts";

interface Props {
    open: boolean;
    onCancel: () => void;
    onSelect: (addr: RecipientAddressWithStats) => void;
    selectedAddress: RecipientAddressWithStats | null;
}

const RecipientAddressPickerModal: React.FC<Props> = ({
                                                          open,
                                                          onCancel,
                                                          onSelect,
                                                          selectedAddress
                                                      }) => {

    const [currentSelected, setCurrentSelected] = useState<RecipientAddressWithStats | null>(selectedAddress);
    const [recipientAddresses, setRecipientAddresses] = useState<RecipientAddressWithStats[]>([]);
    const [loading, setLoading] = useState<boolean>(false);

    const keyword = "";
    const [page, setPage] = useState<number>(1);
    const [total, setTotal] = useState<number>(0);
    const [limit, setLimit] = useState<number>(10);

    const fetchData = async () => {
        setLoading(true);
        try {
            const param = {
                page: page,
                limit: limit,
                search: keyword,
            }
            const result = await recipientAddressApi.getUserAddresses(param);
            if (result.success) {
                let response = result.data?.list || [];

                /**
                 * Nếu có selectedAddress:
                 * - page 1: đưa lên đầu
                 * - mọi page: remove duplicate
                 */
                if (selectedAddress?.address?.id) {

                    // remove duplicate khỏi data API
                    response = response.filter(
                        item => item.address.id !== selectedAddress.address.id
                    );

                    // chỉ ghim ở page 1
                    if (page === 1) {
                        response = [selectedAddress, ...response];
                    }
                }

                setRecipientAddresses(response);
                setTotal(result.data?.pagination?.total || 0);
            } else {
                message.error(result.message || "Không thể tải địa chỉ người nhận")
            }
        } catch (error: any) {
            message.error(error.message || "Không thể tải địa chỉ người nhận");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchData();
    }, [page, limit, keyword]);

    useEffect(() => {
        setCurrentSelected(selectedAddress);
    }, [selectedAddress, open]);

    const handleConfirm = () => {
        if (currentSelected) {
            onSelect(currentSelected);
        }
    };

    return (
        <Modal
            title={<span className="modal-title">Chọn địa chỉ người nhận</span>}
            open={open}
            onCancel={onCancel}
            onOk={handleConfirm}
            okText="Xác nhận"
            cancelText="Hủy"
            okButtonProps={{ className: "modal-ok-button" }}
            cancelButtonProps={{ className: "modal-cancel-button" }}
            className="create-order-address-picker-modal modal-hide-scrollbar"
        >
            <div className="create-order-addresses-container">
            {recipientAddresses?.length === 0 ? (
                <Empty
                    description="Chưa có địa chỉ người nhận đã lưu"
                    style={{padding: "12px 0"}}
                />
            ) : (
                <List
                    loading={loading}
                    dataSource={recipientAddresses}
                    pagination={{
                        current: page,
                        pageSize: limit,
                        total: total,
                        onChange: (newPage, newPageSize) => {
                            setPage(newPage);
                            setLimit(newPageSize);
                        },
                    }}
                    style={{
                        padding: "12px",
                    }}
                    renderItem={(addr) => (
                        <List.Item
                            key={addr.address.id}
                            style={{
                                cursor: "pointer",
                                padding: "12px 0",
                            }}
                            onClick={() => setCurrentSelected(addr)}
                        >
                            <Radio
                                checked={
                                    currentSelected?.address.id === addr.address.id
                                }
                                style={{ marginRight: 16 }}
                            />

                            <List.Item.Meta
                                title={
                                    <span>
                                        {addr.address.name}{" "}
                                        <span className="custom-table-content-strong">
                                            - {addr.address.phoneNumber}
                                        </span>
                                    </span>
                                }
                                description={
                                    <span
                                        className="text-muted"
                                    >
                                    {addr.address.fullAddress}
                                </span>
                                }
                            />
                        </List.Item>
                    )}
                />
            )}
            </div>
        </Modal>
    );
};

export default RecipientAddressPickerModal;