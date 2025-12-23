import React, { useEffect, useState } from "react";
import { Card, Form, Select, Radio } from "antd";
import type { FormInstance } from "antd/lib";
import type { Office } from "../../../../../types/office";
import { ORDER_PICKUP_TYPES, translateOrderPickupType } from "../../../../../utils/orderUtils";
import locationApi from "../../../../../api/locationApi";

interface Props {
  form: FormInstance;
  selectedOffice?: Office | null;
  offices: Office[];
  onChange: (data: { office: Office | null; pickupType: string }) => void;
  disabled: boolean;
  loading: boolean;
}

const PickupType: React.FC<Props> = ({
  form,
  selectedOffice,
  offices,
  onChange,
  disabled,
  loading,
}) => {
  const pickupType = Form.useWatch("pickupType", form);
  const [addressMap, setAddressMap] = useState<Record<number, string>>({});

  useEffect(() => {
    form.setFieldsValue({ senderOfficeId: selectedOffice?.id ?? undefined });
  }, [selectedOffice, form]);

  useEffect(() => {
    const office = offices.find((o) => o.id === form.getFieldValue("senderOfficeId")) || null;
    onChange({ office, pickupType });
  }, [pickupType, form, offices, onChange]);

  useEffect(() => {
    const fetchAddresses = async () => {
      const newMap: Record<number, string> = {};
      await Promise.all(
        offices.map(async (office) => {
          try {
            const city = await locationApi.getCityNameByCode(office.cityCode);
            const ward = await locationApi.getWardNameByCode(office.cityCode, office.wardCode);
            newMap[office.id] = [office.detail, ward || "Unknown", city || "Unknown"]
              .filter(Boolean)
              .join(", ");
          } catch (error) {
            console.error("Error fetching address for office", office.id, error);
            newMap[office.id] = "Unknown";
          }
        })
      );
      setAddressMap(newMap);
    };

    if (offices.length > 0) fetchAddresses();
  }, [offices]);

  return (
    <div className="create-order-card-container">
      <Card className="create-order-custom-card">
        <div className="create-order-custom-card-title">Hình thức lấy hàng</div>

        <Form className="create-order-form" form={form} layout="vertical">
          <Form.Item
            name="pickupType"
            initialValue={"PICKUP_BY_COURIER"}
            rules={[{ required: true, message: "Vui lòng chọn hình thức lấy hàng" }]}
          >
            <Radio.Group disabled={disabled} className="custom-radio-group">
              {ORDER_PICKUP_TYPES.map((type) => (
                <Radio key={type} value={type} className="custom-radio">
                  <span className="custom-radio-label">{translateOrderPickupType(type)}</span>
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>

          {pickupType === "AT_OFFICE" && (
            <Form.Item
              name="senderOfficeId"
              label={<span className="modal-lable">Chọn bưu cục lấy hàng</span>}
              rules={[{ required: true, message: "Vui lòng chọn bưu cục" }]}
            >
              <Select
                className="modal-custom-select"
                placeholder="Chọn bưu cục"
                disabled={disabled || pickupType !== "AT_OFFICE"}
                value={form.getFieldValue("senderOfficeId")}
                onChange={(value) => {
                  const office = offices.find((o) => o.id === value) || null;
                  onChange({ office, pickupType });
                  form.setFieldsValue({ senderOfficeId: value });
                }}
                showSearch
                optionLabelProp="label"
                filterOption={(input, option) =>
                  (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
                }
                loading={loading}
              >
                {offices.map((o) => {
                  const address = addressMap[o.id] || "Đang tải...";
                  return (
                    <Select.Option key={o.id} value={o.id} label={`${o.name} - ${o.postalCode}`}>
                      <div className="create-order-pickup-type office-contain">
                        <span className="create-order-pickup-type office-name">{o.name} - {o.postalCode}</span>
                        <span className="create-order-pickup-type office-address"><strong>Địa chỉ:</strong> {address}</span>
                        <span className="create-order-pickup-type office-address"><strong>Liên hệ:</strong> {o.email || "Unknow"} - {o.phoneNumber || "Unknow"}</span>
                        <span className="create-order-pickup-type office-address"><strong>Giờ làm việc:</strong> {o.openingTime} - {o.closingTime}</span>
                      </div>
                    </Select.Option>
                  );
                })}
              </Select>
            </Form.Item>
          )}
        </Form>
      </Card>
    </div>
  );
};

export default PickupType;