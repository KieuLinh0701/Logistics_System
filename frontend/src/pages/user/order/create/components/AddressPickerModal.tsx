import React, { useEffect, useState } from "react";
import { Modal, Card, Switch, message } from "antd";
import { EditOutlined, DeleteOutlined } from "@ant-design/icons";
import type { Address } from "../../../../../types/address";
import locationApi from "../../../../../api/locationApi";

interface Props {
  open: boolean;
  addresses: Address[];
  initialSelected: Address | null;
  onSelect: (address: Address) => void;
  onAdd: () => void;
  onEdit: (address: Address) => void;
  onDelete: (id: number) => void;
  onSetDefault: (id: number) => void;
  onCancel: () => void;
}

const AddressPickerModal: React.FC<Props> = ({
  open,
  addresses,
  initialSelected,
  onSelect,
  onAdd,
  onEdit,
  onDelete,
  onSetDefault,
  onCancel,
}) => {
  const [selected, setSelected] = useState<Address | null>(initialSelected ?? null);

  const [locationMap, setLocationMap] = useState<
    Record<number, { city: string; ward: string }>
  >({});

  useEffect(() => {
    const fetchLocations = async () => {
      const map: Record<number, { city: string; ward: string }> = {};

      for (const addr of addresses) {
        const city = await locationApi.getCityNameByCode(addr.cityCode) || "";
        const ward =
          await locationApi.getWardNameByCode(addr.cityCode, addr.wardCode) || "";

        map[addr.id!] = { city, ward };
      }

      setLocationMap(map);
    };

    if (addresses.length > 0) {
      fetchLocations();
    }
  }, [addresses]);

  useEffect(() => {
    if (open) {
      setSelected(initialSelected ?? null);
    }
  }, [open, initialSelected]);

  const handleConfirm = () => {
    if (selected) onSelect(selected);
  };

  return (
    <Modal
      open={open}
      onCancel={onCancel}
      onOk={handleConfirm}
      okText="Xác nhận"
      cancelText="Hủy"
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      className="create-order-address-picker-modal modal-hide-scrollbar"
      title={<span className="modal-title">Chọn địa chỉ người gửi</span>}
    >
      <div className="create-order-addresses-container">
        {addresses.map((item) => {
          const isSelected = selected?.id === item.id;
          const location = locationMap[item.id!];
          const wardName = location?.ward || "";
          const cityName = location?.city || "";

          return (
            <Card
              key={item.id}
              className={`create-order-address-card ${isSelected ? "selected" : ""}`}
              onClick={() => setSelected(item)}
            >
              {/* ICONS */}
              <div className="create-order-card-icons">
                <EditOutlined
                  className="create-order-edit-icon"
                  onClick={(e) => {
                    e.stopPropagation();
                    onEdit(item);
                  }}
                />
                <DeleteOutlined
                  className="create-order-delete-icon"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(item.id!);
                  }}
                />
              </div>

              <div className="create-order-address-info">
                <b>{item.name}</b> - {item.phoneNumber}
                <br />
                {item.detail}
                <br />
                {wardName}, {cityName}
              </div>

              <div className="create-order-switch-container">
                <span className="create-order-switch-label">Mặc định</span>
                <Switch
                  className="create-order-custom-switch"
                  checked={item.isDefault}
                  onClick={(_, e) => {
                    e.stopPropagation();
                    onSetDefault(item.id!);
                  }}
                />
              </div>
            </Card>
          );
        })}

        <Card
          className="create-order-add-address-card"
          onClick={() => {
            if (addresses.length >= 10) {
              message.warning("Chỉ được tạo tối đa 10 địa chỉ");
              return;
            }
            onAdd();
          }}
        >
          + Thêm địa chỉ mới
        </Card>
      </div>
    </Modal>
  );
};

export default AddressPickerModal;