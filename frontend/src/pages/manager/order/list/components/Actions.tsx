import React from "react";
import {Button, Dropdown, type MenuProps, Space} from "antd";
import {
    CheckCircleOutlined,
    CloseCircleOutlined, DownOutlined, EnvironmentOutlined,
    FileExcelOutlined,
    PlusOutlined,
    PrinterOutlined, RollbackOutlined,
    TruckOutlined
} from "@ant-design/icons";

interface Props {
    onAdd: () => void;
    onPrint: () => void;
    onAddShipment: () => void;
    onExport: () => void;
    onConfirm: () => void;
    onAtOriginOffice: () => void;
    onCancel: () => void;
    onReturned: () => void;
    disabled: boolean;
    recordNumber: number;
    total: number;
}

const Actions: React.FC<Props> = ({
                                      onAdd,
                                      onPrint,
                                      onCancel,
                                      onConfirm,
                                      onReturned,
                                      onAtOriginOffice,
                                      disabled,
                                      recordNumber,
                                      onAddShipment,
                                      onExport,
                                      total
                                  }) => {
    const bulkItems: MenuProps['items'] =[
        {
            key: 'confirm',
            label: 'Xác nhận đơn hàng',
            icon: <CheckCircleOutlined />,
            onClick: onConfirm,
        },
        {
            key: 'atOrginOffice',
            label: 'Đã đến bưu cục',
            icon: <EnvironmentOutlined />,
            onClick: onAtOriginOffice,
        },
        {
            key: 'addShipment',
            label: 'Thêm vào chuyến',
            icon: <TruckOutlined />,
            onClick: onAddShipment,
        },
        {
            key: 'print',
            label: 'In phiếu hàng loạt',
            icon: <PrinterOutlined />,
            onClick: onPrint,
        },
        {
            key: 'returned',
            label: 'Đã hoàn hàng',
            icon: <RollbackOutlined />,
            onClick: onReturned,
        },
        {
            key: 'cancel',
            label: 'Huỷ đơn hàng',
            icon: <CloseCircleOutlined />,
            onClick: onCancel,
        },
    ].filter(Boolean) as MenuProps['items'] || [];

    return (
        <Space align="center">
            <Button
                className="primary-button"
                icon={<PlusOutlined/>}
                onClick={onAdd}
                disabled={disabled}
            >
                Tạo đơn hàng
            </Button>
            {bulkItems.length > 0 && (
                <Dropdown
                    menu={{items: bulkItems}}
                    className="warning-button"
                    disabled={!disabled}>
                    <Button>
                        Thao tác hàng loạt {recordNumber > 0 ? `(${recordNumber})` : ''} <DownOutlined/>
                    </Button>
                </Dropdown>
            )}
            <Button
                className="success-button"
                icon={<FileExcelOutlined/>}
                onClick={onExport}
                disabled={total === 0}
            >
                Xuất Excel
            </Button>
        </Space>
    );
};

export default Actions;