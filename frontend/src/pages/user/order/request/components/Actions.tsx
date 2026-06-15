import React from 'react';
import {Button, Space} from 'antd';
import {FileExcelOutlined, PlusOutlined} from '@ant-design/icons';
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface ActionsProps {
    onAddRequest: () => void;
    onExport: () => void;
}

const Actions: React.FC<ActionsProps> = ({
                                             onAddRequest,
                                             onExport
                                         }) => {
    return (
        <Space align="center">
            {hasPermissionGroup(['GROUP_USER', 'USER_SUPPORT_CREATE']) && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAddRequest}
                >
                    Tạo yêu cầu
                </Button>
            )}

            {hasPermissionGroup(['GROUP_USER', 'USER_SUPPORT_EXPORT']) && (
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