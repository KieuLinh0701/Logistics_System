import React, { useEffect, useState } from "react";
import Title from "antd/es/typography/Title";
import locationApi from "../../../../../api/locationApi";

interface SenderRecipientInfoProps {
  sender: {
    name: string;
    phone: string;
    detail: string;
    wardCode: number;
    cityCode: number;
  };
  recipient: {
    name: string;
    phone: string;
    detail: string;
    wardCode: number;
    cityCode: number;
  };
}

const SenderRecipientInfo: React.FC<SenderRecipientInfoProps> = ({ sender, recipient }) => {
  const [senderAddress, setSenderAddress] = useState<string>("");
  const [recipientAddress, setRecipientAddress] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const formatAddress = async (detail: string, wardCode: number, cityCode: number) => {
    try {
      const cityName = (await locationApi.getCityNameByCode(cityCode)) || "";
      const wardName = (await locationApi.getWardNameByCode(cityCode, wardCode)) || "";
      return [detail, wardName, cityName].filter(Boolean).join(", ");
    } catch (error) {
      console.error("Error formatting address:", error);
      return detail || "";
    }
  };

  useEffect(() => {
    const fetchAddresses = async () => {
      setIsLoading(true);
      try {
        const [senderAddr, recipientAddr] = await Promise.all([
          formatAddress(sender.detail, sender.wardCode, sender.cityCode),
          formatAddress(recipient.detail, recipient.wardCode, recipient.cityCode)
        ]);

        setSenderAddress(senderAddr);
        setRecipientAddress(recipientAddr);
      } catch (error) {
        console.error("Error fetching addresses:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAddresses();
  }, [sender, recipient]);

  const renderField = (label: string, value: string, isAddressLoading: boolean = false) => {
    let displayValue;

    if (isAddressLoading) {
      displayValue = <span className="order-detail-card-loading">Đang tải...</span>;
    } else if (!value) {
      displayValue = <span className="order-detail-card-na-tag">N/A</span>;
    } else {
      displayValue = value;
    }

    return (
      <div className="order-detail-card-field">
        <strong className="order-detail-card-label">{label}</strong>
        <span className={`order-detail-card-value ${label === "Địa chỉ:" ? "order-detail-card-address" : ""}`}>
          {displayValue}
        </span>
      </div>
    );
  };

  return (
    <div className="order-detail-card">
      {/* Thông tin người gửi */}
      <div className="order-detail-card-column">
        <Title level={5} className="order-detail-card-column-title">
          Thông tin người gửi
        </Title>
        {renderField("Tên:", sender.name)}
        {renderField("SĐT:", sender.phone)}
        {renderField("Địa chỉ:", senderAddress, isLoading)}
      </div>

      {/* Thông tin người nhận */}
      <div className="order-detail-card-column">
        <Title level={5} className="order-detail-card-column-title">
          Thông tin người nhận
        </Title>
        {renderField("Tên:", recipient.name)}
        {renderField("SĐT:", recipient.phone)}
        {renderField("Địa chỉ:", recipientAddress, isLoading)}
      </div>
    </div>
  );
};

export default SenderRecipientInfo;