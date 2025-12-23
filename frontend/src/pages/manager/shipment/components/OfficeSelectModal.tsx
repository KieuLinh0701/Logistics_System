import React, { useEffect, useState } from "react";
import { Modal, Form, Select, message } from "antd";
import type { Office } from "../../../../types/office";
import locationApi from "../../../../api/locationApi";
import type { City, Ward } from "../../../../types/location";
import officeApi from "../../../../api/officeApi";

const { Option } = Select;

interface OfficeSelectModalProps {
    open: boolean;
    value?: Office | null;
    onSelect: (office: Office) => void;
    onCancel: () => void;
}

const OfficeSelectModal: React.FC<OfficeSelectModalProps> = ({
    open,
    onSelect,
    onCancel,
    value,
}) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [cities, setCities] = useState<City[]>([]);
    const [wards, setWards] = useState<Ward[]>([]);
    const [offices, setOffices] = useState<Office[]>([]);
    const [loadingOffices, setLoadingOffices] = useState(false);
    const isRestoringRef = React.useRef(false);
    const [addressMap, setAddressMap] = useState<Record<number, string>>({});

    const selectedCity = Form.useWatch("cityCode", form);
    const selectedWard = Form.useWatch("wardCode", form);

    useEffect(() => {
        if (!open) return;

        locationApi.getCities().then(setCities);

        if (value) {
            isRestoringRef.current = true;
            form.setFieldsValue({
                cityCode: value.cityCode,
            });
        } else {
            form.resetFields();
            setWards([]);
            setOffices([]);
        }
    }, [open]);

    useEffect(() => {
        if (!selectedCity) {
            setWards([]);
            return;
        }

        locationApi.getWardsByCity(selectedCity)
            .then((data) => {
                setWards(data);
            })
            .catch(console.error);
    }, [selectedCity]);

    useEffect(() => {
        if (!value || !selectedCity || wards.length === 0) return;

        form.setFieldsValue({
            wardCode: value.wardCode,
        });
    }, [wards]);

    useEffect(() => {
        if (!value || offices.length === 0) return;

        const exists = offices.some(o => o.id === value.id);
        if (exists) {
            form.setFieldsValue({ officeId: value.id });
        }

        isRestoringRef.current = false;
    }, [offices]);

    useEffect(() => {
        if (!selectedCity) {
            setOffices([]);
            return;
        }

        setLoadingOffices(true);
        officeApi.listLocalOffices({ city: selectedCity, ward: selectedWard })
            .then(async (res) => {
                const officesData = res.data || [];
                setOffices(officesData);

                const currentOfficeId = form.getFieldValue("officeId");
                const stillValid = officesData.some(o => o.id === currentOfficeId);

                if (!stillValid) {
                    form.setFieldsValue({ officeId: undefined });
                }

                console.log("result", res.data);
                console.log("office", officesData);

                if (officesData.length === 0) {
                    message.warning("Bưu cục hiện chưa hoạt động ở khu vực này, vui lòng chọn khu vực khác");
                    form.setFieldsValue({ officeId: undefined });
                    return;
                } else {
                    const newAddressMap: Record<number, string> = {};
                    await Promise.all(
                        officesData.map(async (o) => {
                            try {
                                const wardName = await locationApi.getWardNameByCode(o.cityCode, o.wardCode);
                                const cityName = await locationApi.getCityNameByCode(o.cityCode);
                                newAddressMap[o.id] = [o.detail, wardName || "?", cityName || "?"]
                                    .filter(Boolean)
                                    .join(", ");
                            } catch (err) {
                                newAddressMap[o.id] = o.detail;
                            }
                        })
                    );
                    setAddressMap(newAddressMap);
                }
            })
            .catch(console.error)
            .finally(() => setLoadingOffices(false));
    }, [selectedCity, selectedWard]);

    const handleOk = () => {
        const officeId = form.getFieldValue("officeId");
        const office = offices.find((o) => o.id === officeId);
        if (office) onSelect(office);
    };

    const selectedOfficeId = Form.useWatch("officeId", form);

    return (
        <Modal
            title={
                <span className='modal-title'>Chọn bưu cục</span>}
            centered
            width={600}
            open={open}
            onOk={handleOk}
            onCancel={onCancel}
            okButtonProps={{
                className: "modal-ok-button",
                loading: loading,
                disabled: !selectedOfficeId
            }}
            cancelButtonProps={{
                className: "modal-cancel-button"
            }}
            className="modal-hide-scrollbar"
        >
            <Form form={form} layout="vertical">
                <Form.Item
                    name="cityCode"
                    label={<span className="modal-lable">Tỉnh/Thành phố</span>}
                    rules={[{ required: true, message: "Chọn tỉnh/thành phố" }]}
                >
                    <Select
                        className='modal-custom-select'
                        placeholder="Chọn tỉnh/thành phố"
                        showSearch
                        optionFilterProp="label">
                        {cities.map(c => (
                            <Option key={c.code} value={c.code} label={c.name}>{c.name}</Option>
                        ))}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="wardCode"
                    label={<span className="modal-lable">Phường/Xã</span>}
                    rules={[{ required: true, message: "Chọn phường/xã" }]}
                >
                    <Select
                        className='modal-custom-select'
                        placeholder="Chọn phường/xã"
                        showSearch
                        optionFilterProp="label"
                        disabled={!selectedCity}
                    >
                        {wards.map(w => (
                            <Option key={w.code} value={w.code} label={w.name}>{w.name}</Option>
                        ))}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="officeId"
                    label={<span className="modal-lable">Bưu cục</span>}
                    rules={[{ required: true, message: "Chọn bưu cục" }]}
                >
                    <Select
                        className='modal-custom-select'
                        placeholder="Chọn bưu cục"
                        disabled={!selectedCity}
                        loading={loadingOffices}
                        showSearch
                        optionLabelProp="label"
                        filterOption={(input, option) =>
                            (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
                        }
                    >
                        {offices.map((o) => (
                            <Option key={o.id} value={o.id} label={`${o.name}`}>
                                <div className="shipment-add-edit-select-contain">
                                    <span className="shipment-add-edit-select-name">{o.name}</span>
                                    <span className="shipment-add-edit-select-extra">Địa chỉ: {addressMap[o.id] || "Đang tải..."}</span>
                                    <span className="shipment-add-edit-select-extra">Liên hệ: {o.email || "Unknown"} - {o.phoneNumber || "Unknown"}</span>
                                    <span className="shipment-add-edit-select-extra">Giờ làm việc: {o.openingTime} - {o.closingTime}</span>
                                </div>
                            </Option>
                        ))}
                    </Select>
                </Form.Item>
            </Form>
        </Modal>
    );
};

export default OfficeSelectModal;