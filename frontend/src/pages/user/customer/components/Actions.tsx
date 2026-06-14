import React from "react";
import {Space, Button} from "antd";
import {FileExcelOutlined, PlusOutlined} from "@ant-design/icons";
import {hasPermissionGroup} from "../../../../utils/authUtils.ts";

interface Props {
    onAdd: () => void;
    onExport: () => void;
}

const Actions: React.FC<Props> = ({onAdd, onExport}) => {
    return (
        <Space align="center">
            {hasPermissionGroup(['GROUP_USER', 'USER_CUSTOMER_CREATE']) && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAdd}
                >
                    Thêm khách hàng mới
                </Button>
            )}

            {hasPermissionGroup(['GROUP_USER', 'USER_CUSTOMER_EXPORT']) && (
                <Button
                    className="success-button"
                    icon={<FileExcelOutlined/>}
                    onClick={onExport}
                >
                    Xuất Excel
                </Button>
            )}
        </Space>
    );
};

export default Actions;