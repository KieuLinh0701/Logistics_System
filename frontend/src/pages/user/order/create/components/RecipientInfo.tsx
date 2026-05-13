import React, { useCallback, useEffect, useImperativeHandle, useRef, useState, forwardRef } from "react";
import { Card, Col, Checkbox, Form, Input, Row, Button } from "antd";
import { UnorderedListOutlined } from "@ant-design/icons";
import type { FormInstance } from "antd/lib";
import AddressForm from "../../../../../components/common/AdressForm";
import RecipientAddressPickerModal from "../../../../common/order/RecipientAddressPickerModal.tsx";
import recipientAddressApi from "../../../../../api/recipientAddressApi.ts";
import type { RecipientAddressType, RecipientAddressWithStats } from "../../../../../types/recipientAddress.ts";

interface Props {
    form: FormInstance;
    recipient: {
        name: string;
        phoneNumber: string;
        detail: string;
        wardCode: number;
        wardName: string;
        cityCode: number;
        cityName: string;
        latitude: number;
        longitude: number;
        fullAddress: string;
    };
    disabled: boolean;
    onChange?: (values: any) => void;
    onSaveRecipientChange?: (save: boolean) => void;
    onSavedAddressSelect?: (addressId: number | null) => void;
}

export interface RecipientInfoRef {
    resetSuggestion: () => void;
}

/**
 * Snapshot 5 field cần theo dõi để dirty-check.
 * Bao gồm cả name và phoneNumber ở root level.
 */
function snapshotFields(values: {
    name?: string;
    phoneNumber?: string;
    recipient?: { cityCode?: number; wardCode?: number; detail?: string };
}): string {
    return JSON.stringify({
        name: values.name ?? "",
        phoneNumber: values.phoneNumber ?? "",
        cityCode: values.recipient?.cityCode ?? "",
        wardCode: values.recipient?.wardCode ?? "",
        detail: values.recipient?.detail ?? "",
    });
}

const RecipientInfo = forwardRef<RecipientInfoRef, Props>(({
                                                               form, recipient, disabled, onChange, onSaveRecipientChange, onSavedAddressSelect,
                                                           }, ref) => {
    const watchedName        = Form.useWatch("name", form);
    const watchedPhone       = Form.useWatch("phoneNumber", form);
    const watchedRecipient   = Form.useWatch("recipient", form);

    const [suggestionList, setSuggestionList] = useState<RecipientAddressWithStats[]>([]);
    const [suggestionType, setSuggestionType] = useState<RecipientAddressType>("NONE");
    const [selectedItem, setSelectedItem] = useState<RecipientAddressWithStats | null>(null);
    const [showDropdown, setShowDropdown] = useState(false);
    const phoneValueRef = useRef<string>(recipient.phoneNumber || "");

    const [saveRecipient, setSaveRecipient] = useState(false);
    const [showAddressPicker, setShowAddressPicker] = useState(false);

    /**
     * originalSnapshotRef: snapshot tại lúc chọn SAVED, không bao giờ thay đổi
     * cho đến khi reset hoặc chọn lại địa chỉ mới.
     * Dùng để so sánh ngược — nếu user sửa rồi sửa lại y chang → lock lại.
     */
    const originalSnapshotRef = useRef<string | null>(null);

    /**
     * isSavedLocked = true khi form hiện tại khớp với snapshot gốc.
     * Tính lại mỗi render dựa trên watched values.
     */
    const isSavedLocked = (() => {
        if (originalSnapshotRef.current === null) return false;
        const currentSnapshot = snapshotFields(form.getFieldsValue(true));
        return currentSnapshot === originalSnapshotRef.current;
    })();

    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const dropdownRef = useRef<HTMLDivElement>(null);

    useImperativeHandle(ref, () => ({
        resetSuggestion: () => {
            setSuggestionList([]);
            setSuggestionType("NONE");
            setSelectedItem(null);
            setShowDropdown(false);
            setSaveRecipient(false);
            setShowAddressPicker(false);

            phoneValueRef.current = "";

            originalSnapshotRef.current = null;

            onSaveRecipientChange?.(false);
            onSavedAddressSelect?.(null);
        },
    }));

    // Theo dõi tất cả field — cập nhật onChange và đồng bộ checkbox/savedAddress
    useEffect(() => {
        onChange?.(form.getFieldsValue(true));

        if (originalSnapshotRef.current === null) return;

        const currentSnapshot = snapshotFields(form.getFieldsValue(true));
        const isMatchingOriginal = currentSnapshot === originalSnapshotRef.current;

        if (isMatchingOriginal) {
            // User sửa rồi sửa lại y chang → khôi phục trạng thái đã lưu
            setSuggestionType("SAVED");
            setSaveRecipient(true);
            onSaveRecipientChange?.(true);
            // Không cần gọi onSavedAddressSelect lại vì id vẫn còn nguyên ở parent
        } else {
            // Đang dirty → mở khóa, bỏ tick
            setSuggestionType("NONE");
            setSaveRecipient(false);
            onSaveRecipientChange?.(false);
            onSavedAddressSelect?.(null);
        }
    }, [watchedName, watchedPhone, watchedRecipient]);

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const fetchSuggestion = useCallback(async (phone: string) => {
        try {
            const result = await recipientAddressApi.getUserSuggestion({ phone });
            if (result.success && result.data) {
                setSuggestionList(result.data.addresses ?? []);
                setSuggestionType(result.data.type);
                setShowDropdown((result.data.addresses ?? []).length > 0);
            }
        } catch (error) {
            console.error("Lỗi lấy suggestion người nhận:", error);
        }
    }, []);

    const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const phone = e.target.value.replace(/\D/g, "");

        phoneValueRef.current = phone;
        form.setFieldsValue({ phoneNumber: phone });

        const ready = /^\d{10}$/.test(phone);
        setSelectedItem(null);
        setShowDropdown(false);

        if (!ready) {
            setSuggestionList([]);
            setSuggestionType("NONE");
            return;
        }

        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => fetchSuggestion(phone), 400);
    };

    const handleSelectSuggestionItem = (item: RecipientAddressWithStats) => {
        setSelectedItem(item);
        setShowDropdown(false);

        const { address } = item;
        const currentPhone = address.phoneNumber || phoneValueRef.current;

        phoneValueRef.current = currentPhone;
        form.setFieldsValue({
            name: address.name,
            phoneNumber: currentPhone,
            recipient: {
                cityCode: address.cityCode || undefined,
                cityName: address.cityName,
                wardCode: address.wardCode || undefined,
                wardName: address.wardName,
                detail: address.detail,
                latitude: address.latitude,
                longitude: address.longitude,
            },
        });
        onChange?.(form.getFieldsValue(true));

        if (suggestionType === "SAVED") {
            setSaveRecipient(true);
            onSaveRecipientChange?.(true);
            onSavedAddressSelect?.(address.id ?? null);
            originalSnapshotRef.current = snapshotFields(form.getFieldsValue(true));
        } else {
            setSaveRecipient(false);
            onSaveRecipientChange?.(false);
            onSavedAddressSelect?.(null);
            originalSnapshotRef.current = null;
        }
    };

    const handleSelectSavedAddress = (addr: RecipientAddressWithStats) => {
        setSelectedItem(addr);

        phoneValueRef.current = addr.address.phoneNumber || "";
        form.setFieldsValue({
            name: addr.address.name,
            phoneNumber: addr.address.phoneNumber,
            recipient: {
                cityCode: addr.address.cityCode || undefined,
                cityName: addr.address.cityName,
                wardCode: addr.address.wardCode || undefined,
                wardName: addr.address.wardName,
                detail: addr.address.detail,
                latitude: addr.address.latitude,
                longitude: addr.address.longitude,
            },
        });
        setSuggestionType("SAVED");
        setSaveRecipient(true);
        onSaveRecipientChange?.(true);
        onChange?.(form.getFieldsValue(true));
        setShowAddressPicker(false);
        onSavedAddressSelect?.(addr.address.id ?? null);
        originalSnapshotRef.current = snapshotFields(form.getFieldsValue(true));
    };

    const handleSaveCheckboxChange = (checked: boolean) => {
        setSaveRecipient(checked);
        onSaveRecipientChange?.(checked);
    };

    return (
        <div className="create-order-card-container">
            <Form
                form={form}
                layout="vertical"
                initialValues={{
                    name: recipient.name,
                    phoneNumber: recipient.phoneNumber,
                    recipient: {
                        cityCode: recipient.cityCode !== 0 ? recipient.cityCode : undefined,
                        cityName: recipient.cityName,
                        wardCode: recipient.wardCode !== 0 ? recipient.wardCode : undefined,
                        wardName: recipient.wardName,
                        latitude: recipient.latitude,
                        longitude: recipient.longitude,
                        detail: recipient.detail,
                    },
                }}
                onValuesChange={() => onChange?.(form.getFieldsValue(true))}
            >
                <Card className="create-order-custom-card">
                    <div className="create-order-custom-card-title">Thông tin người nhận</div>

                    <div className="create-order-content">
                        <Row gutter={16}>
                            {/* CỘT TRÁI */}
                            <Col span={12}>
                                <div ref={dropdownRef} style={{ position: "relative" }}>
                                    <Form.Item
                                        name="phoneNumber"
                                        label={<span className="modal-lable">Số điện thoại</span>}
                                        rules={[
                                            { required: true, message: "Vui lòng nhập số điện thoại" },
                                            { pattern: /^\d{10}$/, message: "Số điện thoại phải đủ 10 số" },
                                        ]}
                                    >
                                        <Input.Search
                                            className="modal-custom-search"
                                            placeholder="Nhập số điện thoại người nhận"
                                            disabled={disabled}
                                            onChange={handlePhoneChange}
                                            onFocus={() => suggestionList.length > 0 && setShowDropdown(true)}
                                            enterButton={
                                                <Button
                                                    icon={<UnorderedListOutlined />}
                                                    disabled={disabled}
                                                    onClick={() => setShowAddressPicker(true)}
                                                >
                                                    Chọn từ danh sách
                                                </Button>
                                            }
                                            onSearch={() => setShowAddressPicker(true)}
                                        />
                                    </Form.Item>

                                    {/* Dropdown gợi ý */}
                                    {showDropdown && suggestionList.length > 0 && (
                                        <ul className="dropdown-list">
                                            {suggestionList.map((item, idx) => (
                                                <li
                                                    key={idx}
                                                    className="dropdown-item"
                                                    onMouseDown={(e) => e.preventDefault()}
                                                    onClick={() => handleSelectSuggestionItem(item)}
                                                >
                                                    <div style={styles.dropdownName}>{item.address.name}</div>
                                                    <div style={styles.dropdownSub}>
                                                        {item.address.phoneNumber} · {item.address.fullAddress || item.address.detail}
                                                    </div>
                                                </li>
                                            ))}
                                        </ul>
                                    )}

                                    {/* Stats */}
                                    {selectedItem && (
                                        <div style={styles.statsCard}>
                                            <div style={styles.statsHeader}>
                                                Thống kê hoạt động người nhận
                                            </div>

                                            <div style={styles.statsItem}>
                                                <span style={styles.statsLabel}>
                                                    Tổng số đơn hàng
                                                </span>
                                                <span style={styles.statsValueBlue}>
                                                    {selectedItem.recipientStats.totalSystemOrders}
                                                </span>
                                            </div>

                                            <div style={styles.statsItem}>
                                                <span style={styles.statsLabel}>
                                                    Tỉ lệ đơn thành công
                                                </span>
                                                <span style={styles.statsValueGreen}>
                                                    {selectedItem.recipientStats.successRate}%
                                                </span>
                                            </div>

                                            <div style={styles.statsItem}>
                                                <span style={styles.statsLabel}>
                                                    Tỉ lệ đơn hoàn
                                                </span>
                                                <span style={styles.statsValueRed}>
                                                    {selectedItem.recipientStats.returnedRate}%
                                                </span>
                                            </div>

                                            {selectedItem.recipientStats.latestOrderDate && (
                                                <div style={styles.statsItem}>
                                                    <span style={styles.statsLabel}>
                                                        Đơn hàng gần nhất
                                                    </span>
                                                    <span style={styles.statsValue}>
                                                        {new Date(
                                                            selectedItem.recipientStats.latestOrderDate
                                                        ).toLocaleDateString("vi-VN")}
                                                    </span>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>

                                {/* Tên người nhận */}
                                <Form.Item
                                    name="name"
                                    label={<span className="modal-lable">Tên người nhận</span>}
                                    rules={[{ required: true, message: "Vui lòng nhập tên" }]}
                                    style={{ marginTop: 12 }}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Nhập tên người nhận"
                                        disabled={disabled}
                                    />
                                </Form.Item>
                            </Col>

                            {/* CỘT PHẢI */}
                            <Col span={12}>
                                <AddressForm
                                    form={form}
                                    prefix="recipient"
                                    disableCity={disabled}
                                    disableDetailAddress={disabled}
                                />
                            </Col>
                        </Row>

                        <Row style={{ marginTop: 8 }}>
                            <Col span={24}>
                                <Checkbox
                                    checked={saveRecipient}
                                    disabled={disabled || isSavedLocked}
                                    onChange={(e) => handleSaveCheckboxChange(e.target.checked)}
                                >
                                    <span style={{ fontSize: 13, color: "#595959" }}>
                                        Lưu địa chỉ người nhận này
                                        {isSavedLocked && (
                                            <span style={{ color: "#8c8c8c", marginLeft: 6 }}>(đã lưu)</span>
                                        )}
                                    </span>
                                </Checkbox>
                            </Col>
                        </Row>
                    </div>
                </Card>
            </Form>

            <RecipientAddressPickerModal
                open={showAddressPicker}
                onCancel={() => setShowAddressPicker(false)}
                selectedAddress={selectedItem}
                onSelect={handleSelectSavedAddress}
            />
        </div>
    );
});

export default RecipientInfo;

const styles: Record<string, React.CSSProperties> = {
    dropdownName: {
        fontWeight: 600,
        fontSize: 13,
        color: "#262626",
    },
    dropdownSub: {
        fontSize: 12,
        color: "#8c8c8c",
        marginTop: 2,
    },
    statsCard: {
        marginTop: 12,
        border: "1px solid #e8e8e8",
        borderLeft: "4px solid #1C3D90",
        borderRadius: 10,
        padding: 14,
        background: "#ffffff",
        boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
    },

    statsHeader: {
        fontSize: 13,
        fontWeight: 700,
        color: "#1f1f1f",
        marginBottom: 12,
    },

    statsItem: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        padding: "6px 0",
        borderBottom: "1px dashed #f0f0f0",
    },

    statsLabel: {
        fontSize: 13,
        color: "#595959",
    },

    statsValueBlue: {
        fontSize: 13,
        fontWeight: 700,
        color: "#1677ff",
    },

    statsValueGreen: {
        fontSize: 13,
        fontWeight: 700,
        color: "#52c41a",
    },

    statsValueRed: {
        fontSize: 13,
        fontWeight: 700,
        color: "#ff4d4f",
    },

    statsValue: {
        fontSize: 13,
        fontWeight: 600,
    },
};