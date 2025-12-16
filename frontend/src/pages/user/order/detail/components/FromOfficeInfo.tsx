import React, { useEffect, useState } from "react";
import type { Office } from "../../../../../types/office";
import locationApi from "../../../../../api/locationApi";
import Title from "antd/es/typography/Title";
import { Tooltip } from "antd";
import { EnvironmentOutlined } from "@ant-design/icons";

interface FromOfficeInfoProps {
    office: Office;
}

const FromOfficeInfo: React.FC<FromOfficeInfoProps> = ({ office }) => {
    const [formattedAddress, setFormattedAddress] = useState<string>("");

    useEffect(() => {
        const formatAddress = async (detail?: string, wardCode?: number, cityCode?: number) => {
            try {
                const cityName = cityCode ? await locationApi.getCityNameByCode(cityCode) : "";
                const wardName = cityCode && wardCode ? await locationApi.getWardNameByCode(cityCode, wardCode) : "";
                return [detail, wardName, cityName].filter(Boolean).join(", ");
            } catch (error) {
                console.error("Error formatting address:", error);
                return detail || "";
            }
        };

        if (office) {
            formatAddress(office.detail, office.wardCode, office.cityCode).then(setFormattedAddress);
        }
    }, [office]);

    const openMap = () => {
        if (office.latitude && office.longitude) {
            window.open(
                `https://www.google.com/maps?q=${office.latitude},${office.longitude}`,
                "_blank",
                "noopener,noreferrer"
            );
        }
    };

    const renderField = (label: string, value?: string | number | null, clickable?: boolean) => {
        const displayValue =
            value === null || value === undefined || value === ""
                ? <span className="order-detail-card-na-tag">N/A</span>
                : value;

        return (
            <div className="order-detail-card-field">
                <strong className="order-detail-card-label">{label}</strong>
                <span className="order-detail-card-value">
                    {clickable ? (
                        <Tooltip title="Nhấn để mở Google Maps">
                            <span className="navigate-link" onClick={openMap}>{displayValue}</span>
                        </Tooltip>
                    ) : (
                        displayValue
                    )}
                </span>
            </div>
        );
    };

    if (!office) return null;

    return (
        <div className="order-detail-card">
            <Title level={5} className="order-detail-card-title order-detail-card-title-main">
                Bưu cục gửi
            </Title>

            <div className="order-detail-card-inner">
                <div className="order-detail-card-column">
                    {renderField("Tên:", office.name)}
                    {renderField("Địa chỉ:", formattedAddress, !!(office.latitude && office.longitude))}
                </div>

                <div className="order-detail-card-column">
                    {renderField("Email:", office.email)}
                    {renderField("Điện thoại:", office.phoneNumber)}
                    {renderField("Giờ hoạt động:", office.openingTime && office.closingTime ? `${office.openingTime} - ${office.closingTime}` : null)}
                </div>
            </div>
        </div>
    );
};

export default FromOfficeInfo;