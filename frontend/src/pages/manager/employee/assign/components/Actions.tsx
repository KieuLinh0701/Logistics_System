import React from 'react';
import {Button, Space} from 'antd';
import {FileExcelOutlined, PlusOutlined} from '@ant-design/icons';

interface ActionsProps {
    onAdd: () => void;
    onExport: () => void;
    total: number;
}

const Actions: React.FC<ActionsProps> = ({
                                             onAdd,
                                             onExport,
                                             total
                                         }) => {
    return (
        <Space align="center">
            <Button
                className="primary-button"
                icon={<PlusOutlined/>}
                onClick={onAdd}
            >
                Tạo phân công
            </Button>
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