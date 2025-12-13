import React, { useEffect, useState } from "react";
import type { Office } from "../../../../../types/office";
import locationApi from "../../../../../api/locationApi";
import Title from "antd/es/typography/Title";
import { Tooltip } from "antd";

interface OfficeInfoProps {
    fromOffice?: Office;
    toOffice?: Office;
}

const OfficeInfo: React.FC<OfficeInfoProps> = ({ fromOffice, toOffice }) => {
    const [fromAddress, setFromAddress] = useState<string>("");
    const [toAddress, setToAddress] = useState<string>("");

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

        if (fromOffice) {
            formatAddress(fromOffice.detail, fromOffice.wardCode, fromOffice.cityCode).then(setFromAddress);
        }
        if (toOffice) {
            formatAddress(toOffice.detail, toOffice.wardCode, toOffice.cityCode).then(setToAddress);
        }
    }, [fromOffice, toOffice]);

    const openMap = (office?: Office) => {
        if (office?.latitude && office?.longitude) {
            window.open(
                `https://www.google.com/maps?q=${office.latitude},${office.longitude}`,
                "_blank",
                "noopener,noreferrer"
            );
        }
    };

    const renderField = (label: string, value?: string | number | null, clickable?: boolean, office?: Office) => {
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
                            <span className="navigate-link" onClick={() => openMap(office)}>{displayValue}</span>
                        </Tooltip>
                    ) : (
                        displayValue
                    )}
                </span>
            </div>
        );
    };

    return (
        <div className="order-detail-card">
            <div className="order-detail-card-inner">
                {/* Cột bưu cục gửi */}
                <div className="order-detail-card-column">
                    <Title level={5} className="order-detail-card-title order-detail-card-title-sub">
                        Bưu cục gửi
                    </Title>
                    {fromOffice && renderField("Tên:", fromOffice.name)}
                    {fromOffice && renderField("Địa chỉ:", fromAddress, !!(fromOffice.latitude && fromOffice.longitude), fromOffice)}
                    {fromOffice && renderField("Email:", fromOffice.email)}
                    {fromOffice && renderField("Điện thoại:", fromOffice.phoneNumber)}
                    {fromOffice && renderField(
                        "Giờ hoạt động:",
                        fromOffice.openingTime && fromOffice.closingTime ? `${fromOffice.openingTime} - ${fromOffice.closingTime}` : null
                    )}
                </div>

                {/* Cột bưu cục nhận */}
                {toOffice &&
                    <div className="order-detail-card-column">
                        <Title level={5} className="order-detail-card-title order-detail-card-title-sub">
                            Bưu cục nhận
                        </Title>
                        {toOffice && renderField("Tên:", toOffice.name)}
                        {toOffice && renderField("Địa chỉ:", toAddress, !!(toOffice.latitude && toOffice.longitude), toOffice)}
                        {toOffice && renderField("Email:", toOffice.email)}
                        {toOffice && renderField("Điện thoại:", toOffice.phoneNumber)}
                        {toOffice && renderField(
                            "Giờ hoạt động:",
                            toOffice.openingTime && toOffice.closingTime ? `${toOffice.openingTime} - ${toOffice.closingTime}` : null
                        )}
                    </div>
                }
            </div>
        </div>
    );
};

export default OfficeInfo;