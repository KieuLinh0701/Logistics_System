import React, { useCallback, useEffect, useImperativeHandle, useRef, useState, forwardRef } from "react";
import { Card, Col, Checkbox, Form, Input, Row, Button } from "antd";
import { UnorderedListOutlined } from "@ant-design/icons";
import type { FormInstance } from "antd/lib";
import AddressForm from "../../../../../components/common/AdressForm";
import RecipientAddressPickerModal from "./RecipientAddressPickerModal";
import type { Address } from "../../../../../types/address";
import recipientAddressApi from "../../../../../api/recipientAddressApi.ts";
import type { RecipientAddressType, RecipientSuggestionResponse } from "../../../../../types/recipientAddress.ts";
import RecipientSuggestionBox from "../../../../common/recipientaddress/RecipientSuggestionBox.tsx";

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

const RecipientInfo = forwardRef<RecipientInfoRef, Props>(({
                                                               form,
                                                               recipient,
                                                               disabled,
                                                               onChange,
                                                               onSaveRecipientChange,
                                                               onSavedAddressSelect,
                                                           }, ref) => {
    const recipientValues = Form.useWatch("recipient", form);

    const [suggestion, setSuggestion] = useState<RecipientSuggestionResponse | null>(null);
    const [suggestionLoading, setSuggestionLoading] = useState(false);
    const [phoneReady, setPhoneReady] = useState(false);
    const [savedAddresses, setSavedAddresses] = useState<Address[]>([]);
    const [addressApplied, setAddressApplied] = useState(false);
    const [selectedRecipientAddressId, setSelectedRecipientAddressId] = useState<number | null>(null);
    const [originalSavedAddress, setOriginalSavedAddress] = useState<Address | null>(null);

    const [suggestionType, setSuggestionType] = useState<RecipientAddressType>("NONE");

    const [saveRecipient, setSaveRecipient] = useState(false);

    // Modal chọn từ danh sách
    const [showAddressPicker, setShowAddressPicker] = useState(false);

    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    useImperativeHandle(ref, () => ({
        resetSuggestion: () => {
            setSuggestion(null);
            setSuggestionType("NONE");
            setPhoneReady(false);
            setSaveRecipient(false);
            setSelectedRecipientAddressId(null);
            setAddressApplied(false);
        }
    }));

    useEffect(() => {
        onChange?.(form.getFieldsValue(true));
    }, [recipientValues]);

    const fetchSuggestion = useCallback(
        async (phone: string) => {
            setSuggestionLoading(true);
            try {
                const result = await recipientAddressApi.getUserSuggestion({ phone });
                if (result.success && result.data) {
                    console.log("recipient address", result.data);
                    setSuggestion(result.data);
                    setSuggestionType(result.data.type);
                }
            } catch (error) {
                console.error("Lỗi lấy suggestion người nhận:", error);
            } finally {
                setSuggestionLoading(false);
            }
        },
        [form]
    );

    const fillFormFromSuggestion = (s: RecipientSuggestionResponse) => {
        const {address} = s;
        form.setFieldsValue({
            name: address.name,
            phoneNumber: address.phoneNumber,
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
    };

    const isSameAsOriginalSaved = (addr: Address) => {
        const values = form.getFieldsValue(true);

        return (
            values.name === addr.name &&
            values.phoneNumber === addr.phoneNumber &&

            values.recipient?.cityCode === addr.cityCode &&
            values.recipient?.wardCode === addr.wardCode &&
            values.recipient?.detail === addr.detail
        );
    };

    // User bấm nút "Dùng địa chỉ này" trên suggestion box
    const handleApplySuggestion = () => {
        if (!suggestion) return;
        setAddressApplied(true);
        fillFormFromSuggestion(suggestion);

        if (suggestion.type === "SAVED") {
            setOriginalSavedAddress(suggestion.address as Address);

            setSelectedRecipientAddressId(suggestion.address.id ?? null);
            onSavedAddressSelect?.(suggestion.address.id ?? null);

            setSaveRecipient(true);
            onSaveRecipientChange?.(true);
        } else {
            // HISTORY → fill nhưng không tick checkbox, để user tự chọn
            setSelectedRecipientAddressId(null);
            onSavedAddressSelect?.(null);
            setSaveRecipient(false);
            onSaveRecipientChange?.(false);
        }
    };

    const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        handleManualEdit();
        setAddressApplied(false);

        const phone = e.target.value.replace(/\D/g, "");

        const ready = /^\d{10}$/.test(phone);
        setPhoneReady(ready);

        if (!ready) {
            setSuggestion(null);
            setSuggestionType("NONE");
            return;
        }

        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => {
            fetchSuggestion(phone);
        }, 400);
    };

    const handleSelectSavedAddress = (addr: Address) => {
        form.setFieldsValue({
            name: addr.name,
            phoneNumber: addr.phoneNumber,
            recipient: {
                cityCode: addr.cityCode || undefined,
                cityName: addr.cityName,
                wardCode: addr.wardCode || undefined,
                wardName: addr.wardName,
                detail: addr.detail,
                latitude: addr.latitude,
                longitude: addr.longitude,
            },
        });

        // Update suggestion type thành SAVED để disable checkbox
        setSuggestionType("SAVED");

        setSuggestion((prev) =>
            prev
                ? {...prev, address: addr as any, type: "SAVED"}
                : null
        );

        setOriginalSavedAddress(addr);

        setSaveRecipient(true);
        onSaveRecipientChange?.(true);

        onChange?.(form.getFieldsValue(true));
        setShowAddressPicker(false);

        setSelectedRecipientAddressId(addr.id ?? null);
        onSavedAddressSelect?.(addr.id ?? null);
    };

    const handleSaveCheckboxChange = (checked: boolean) => {
        setSaveRecipient(checked);
        onSaveRecipientChange?.(checked);
    };

    // Khi user sửa bất kỳ field nào sau khi đã có suggestion SAVED → coi như địa chỉ mới
    const handleManualEdit = () => {

        // Nếu form hiện tại giống hệt saved address gốc
        // → restore trạng thái SAVED
        if (
            originalSavedAddress &&
            isSameAsOriginalSaved(originalSavedAddress)
        ) {

            setSuggestionType("SAVED");

            setSaveRecipient(true);
            onSaveRecipientChange?.(true);

            setSelectedRecipientAddressId(originalSavedAddress.id ?? null);
            onSavedAddressSelect?.(originalSavedAddress.id ?? null);

            return;
        }

        // Nếu khác → coi như address mới
        setSuggestionType("NONE");

        setSaveRecipient(false);
        onSaveRecipientChange?.(false);

        setSelectedRecipientAddressId(null);
        onSavedAddressSelect?.(null);
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
                onValuesChange={(_, allValues) => {
                    onChange?.(form.getFieldsValue(true));
                }}
            >
                <Card className="create-order-custom-card">
                    <div className="create-order-custom-card-title">
                        Thông tin người nhận
                    </div>

                    <div className="create-order-content">
                        <Row gutter={16}>
                            {/* CỘT TRÁI: SĐT + tên + suggestion */}
                            <Col span={12}>
                                {/* SĐT — có nút chọn từ danh sách */}
                                <Form.Item
                                    name="phoneNumber"
                                    label={<span className="modal-lable">Số điện thoại</span>}
                                    rules={[
                                        {required: true, message: "Vui lòng nhập số điện thoại"},
                                        {
                                            pattern: /^\d{10}$/,
                                            message: "Số điện thoại phải đủ 10 số",
                                        },
                                    ]}
                                >
                                    <Input.Search
                                        className="modal-custom-search"
                                        placeholder="Ví dụ: 0123456789"
                                        disabled={disabled}
                                        onChange={handlePhoneChange}
                                        enterButton={
                                            <Button
                                                icon={<UnorderedListOutlined/>}
                                                disabled={disabled}
                                                onClick={() => setShowAddressPicker(true)}
                                            >
                                                Chọn từ danh sách
                                            </Button>
                                        }
                                        onSearch={() => setShowAddressPicker(true)}
                                    />
                                </Form.Item>

                                {/* Suggestion box ngay dưới SĐT */}
                                <RecipientSuggestionBox
                                    loading={suggestionLoading}
                                    suggestion={suggestion}
                                    phoneReady={phoneReady}
                                    addressApplied={addressApplied}
                                    onApply={
                                        suggestion && suggestion.type !== "NONE"
                                            ? handleApplySuggestion
                                            : undefined
                                    }
                                />

                                {/* Tên người nhận */}
                                <Form.Item
                                    name="name"
                                    label={<span className="modal-lable">Tên người nhận</span>}
                                    rules={[{required: true, message: "Vui lòng nhập tên"}]}
                                    style={{marginTop: 12}}
                                >
                                    <Input
                                        className="modal-custom-input"
                                        placeholder="Nhập tên người nhận"
                                        disabled={disabled}
                                        onChange={handleManualEdit}
                                    />
                                </Form.Item>
                            </Col>

                            {/* CỘT PHẢI: địa chỉ */}
                            <Col span={12}>
                                <AddressForm
                                    form={form}
                                    prefix="recipient"
                                    disableCity={disabled}
                                    disableDetailAddress={disabled}
                                    onManualChange={handleManualEdit}
                                />
                            </Col>
                        </Row>

                        {/* CHECKBOX lưu địa chỉ — luôn hiện, disabled nếu SAVED */}
                        <Row style={{marginTop: 8}}>
                            <Col span={24}>
                                <Checkbox
                                    checked={saveRecipient}
                                    disabled={disabled || suggestionType === "SAVED"}
                                    onChange={(e) => handleSaveCheckboxChange(e.target.checked)}
                                >
                                    <span style={{fontSize: 13, color: "#595959"}}>
                                        Lưu địa chỉ người nhận này
                                        {suggestionType === "SAVED" && (
                                            <span style={{color: "#8c8c8c", marginLeft: 6}}>
                                                (đã lưu)
                                            </span>
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
                addresses={savedAddresses}
                onCancel={() => setShowAddressPicker(false)}
                onSelect={handleSelectSavedAddress}
            />
        </div>
    );
});

export default RecipientInfo;