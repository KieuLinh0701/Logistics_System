import React from "react";
import {Button, Space} from "antd";
import {FileExcelOutlined} from "@ant-design/icons";
import {hasPermissionGroup} from "../../../../utils/authUtils.ts";

interface Props {
    onExport: () => void;
}

const Actions: React.FC<Props> = ({onExport}) => {
    return (
        <Space align="center">
            {hasPermissionGroup(['GROUP_USER', 'USER_COD_EXPORT_DETAIL']) && (
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