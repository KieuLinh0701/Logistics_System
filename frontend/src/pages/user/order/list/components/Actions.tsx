import React from "react";
import {Button, Space} from "antd";
import {FileExcelOutlined, PlusOutlined, PrinterOutlined} from "@ant-design/icons";
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface Props {
    onAdd: () => void;
    onPrint: () => void;
    onExport: () => void;
    disabled: boolean;
    recordNumber: number;
}

const Actions: React.FC<Props> = ({onAdd, onPrint, onExport, disabled, recordNumber}) => {
    return (
        <Space align="center">
            {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_CREATE']) && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAdd}
                    disabled={disabled}
                >
                    Tạo đơn hàng
                </Button>
            )}

            {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PRINT_BULK']) && (
                <Button
                    className="warning-button"
                    icon={<PrinterOutlined/>}
                    onClick={onPrint}
                    disabled={!disabled}>
                    In phiếu {recordNumber !== 0 ? `(${recordNumber})` : ""}
                </Button>
            )}

            {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_EXPORT']) && (
                <Button
                    className="success-button"
                    icon={<FileExcelOutlined/>}
                    onClick={onExport}>
                    Xuất Excel
                </Button>
            )}
        </Space>
    );
};

export default Actions;