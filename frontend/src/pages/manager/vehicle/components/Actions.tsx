import React from 'react';
import {Button, Space} from 'antd';
import {FileExcelOutlined, PlusOutlined} from '@ant-design/icons';

interface ActionsProps {
    onExport: () => void,
    total: number,
}

const Actions: React.FC<ActionsProps> = ({
                                             onExport,
                                             total,
                                         }) => {
    return (
        <Space align="center">
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