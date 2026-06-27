import React from "react";
import {Button, Dropdown, type MenuProps, Space} from "antd";
import {
    CheckSquareOutlined,
    CloseCircleOutlined, DeleteOutlined,
    DownOutlined, EnvironmentOutlined,
    FileExcelOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    PrinterOutlined, SendOutlined
} from "@ant-design/icons";
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface Props {
    onAdd: () => void;
    onPrint: () => void;
    onExport: () => void;
    onTransitToOffice: () => void;
    onPublic: () => void;
    hasSelection: boolean;
    selectedCount: number;
    total: number;
    onReadyBulk: () => void;
    onCancel: () => void;
    onDelete: () => void;
}

const Actions: React.FC<Props> = ({
                                      onAdd,
                                      onPrint,
                                      onReadyBulk,
                                      onExport,
                                      hasSelection,
                                      selectedCount,
                                      total,
                                      onCancel,
                                      onDelete,
                                      onPublic,
                                      onTransitToOffice
                                  }) => {
    const bulkItems: MenuProps['items'] = [
        hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PRINT_BULK']) && {
            key: 'print',
            label: 'In phiếu hàng loạt',
            icon: <PrinterOutlined />,
            onClick: onPrint,
        },
        hasPermissionGroup(['GROUP_USER', 'USER_READY_FOR_PICKUP_BULK']) && {
            key: 'ready',
            label: 'Sẵn sàng để lấy',
            icon: <CheckSquareOutlined />,
            onClick: onReadyBulk,
        },
        hasPermissionGroup(['GROUP_USER', 'USER_ORDER_TRANSIT_TO_OFFICE_BULK']) && {
            key: 'transit',
            label: 'Chuyển về bưu cục',
            icon: <EnvironmentOutlined />,
            onClick: onTransitToOffice,
        },
        hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PROCESS_BULK']) && {
            key: 'public',
            label: 'Chuyển xử lý',
            icon: <SendOutlined />,
            onClick: onPublic,
        },
        hasPermissionGroup(['GROUP_USER', 'USER_ORDER_CANCEL_BULK']) && {
            key: 'cancel',
            label: 'Huỷ đơn hàng',
            icon: <CloseCircleOutlined />,
            onClick: onCancel,
        },
        hasPermissionGroup(['GROUP_USER', 'USER_ORDER_DELETE_BULK']) && {
            key: 'delete',
            label: 'Xóa đơn hàng',
            icon: <DeleteOutlined />,
            onClick: onDelete,
        },
    ].filter(Boolean) as MenuProps['items'] || [];

    return (
        <Space align="center">
            {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_CREATE']) && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAdd}
                    disabled={hasSelection}
                >
                    Tạo đơn hàng
                </Button>
            )}

            {bulkItems.length > 0 && (
                <Dropdown
                    menu={{items: bulkItems}}
                    className="warning-button"
                    disabled={!hasSelection}>
                    <Button>
                        Thao tác hàng loạt {selectedCount > 0 ? `(${selectedCount})` : ''} <DownOutlined/>
                    </Button>
                </Dropdown>
            )}

            {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_EXPORT']) && (
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