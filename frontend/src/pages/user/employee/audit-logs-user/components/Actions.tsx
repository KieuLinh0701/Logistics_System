import React from 'react';
import {Button, Space} from 'antd';
import {FileExcelOutlined} from '@ant-design/icons';
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface ActionsProps {
    onExport: () => void;
    total: number;
}

const Actions: React.FC<ActionsProps> = ({
                                             onExport,
                                             total
                                         }) => {
    return (
        <Space align="center">
            {hasPermissionGroup(['GROUP_USER', 'USER_AUDIT_LOG_DETAIL_EXPORT']) && (
                <Button
                    className="success-button"
                    icon={<FileExcelOutlined/>}
                    onClick={onExport}
                    disabled={total === 0}
                >
                    Xuất Excel
                </Button>
            )}
        </Space>
    );
};

export default Actions;