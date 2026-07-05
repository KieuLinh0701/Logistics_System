import React from 'react';
import {Button, Space} from 'antd';
import {FileExcelOutlined, PlusOutlined} from '@ant-design/icons';

interface ShipmentHistoryActionsProps {
    onAddRequest?: () => void;
    onExport: () => void;
    total: number;
}

const ShipmentHistoryActions: React.FC<ShipmentHistoryActionsProps> = ({
    onAddRequest,
    onExport,
    total,
}) => {
    return (
        <Space align="center">
            {onAddRequest && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAddRequest}
                >
                    Tạo chuyến hàng
                </Button>
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

export default ShipmentHistoryActions;
