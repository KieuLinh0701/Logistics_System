import React from "react";
import {Space, Button} from "antd";
import {PlusOutlined, PrinterOutlined, TruckOutlined} from "@ant-design/icons";
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface Props {
    onAdd: () => void;
    onPrint: () => void;
    disabled: boolean;
    recordNumber: number;
}

const Actions: React.FC<Props> = ({onAdd, onPrint, disabled, recordNumber}) => {
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
                    className="success-button"
                    icon={<PrinterOutlined/>}
                    onClick={onPrint}
                    disabled={!disabled}>
                    In phiếu {recordNumber !== 0 ? `(${recordNumber})` : ""}
                </Button>
            )}
        </Space>
    );
};

export default Actions;