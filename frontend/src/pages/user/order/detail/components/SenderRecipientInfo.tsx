import React from "react";
import Title from "antd/es/typography/Title";

interface SenderRecipientInfoProps {
    sender: {
        name: string;
        phone: string;
        fullAddress: string;
    };
    recipient: {
        name: string;
        phone: string;
        fullAddress: string;
    };
}


const SenderRecipientInfo: React.FC<SenderRecipientInfoProps> = ({sender, recipient}) => {
    const renderField = (label: string, value: string) => {

        return (
            <div className="order-detail-card-field">
                <strong className="order-detail-card-label">{label}</strong>
                <span className={`order-detail-card-value ${label === "Địa chỉ:" ? "order-detail-card-address" : ""}`}>
          {value}
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
                {renderField("Địa chỉ:", sender.fullAddress)}
            </div>

            {/* Thông tin người nhận */}
            <div className="order-detail-card-column">
                <Title level={5} className="order-detail-card-column-title">
                    Thông tin người nhận
                </Title>
                {renderField("Tên:", recipient.name)}
                {renderField("SĐT:", recipient.phone)}
                {renderField("Địa chỉ:", recipient.fullAddress)}
            </div>
        </div>
    );
};

export default SenderRecipientInfo;