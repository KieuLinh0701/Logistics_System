import React from 'react';
import {Button, Space, Upload} from 'antd';
import {PlusOutlined, UploadOutlined, DownloadOutlined, FileExcelOutlined} from '@ant-design/icons';
import {hasPermissionGroup} from "../../../../utils/authUtils.ts";

interface ActionsProps {
    onAdd: () => void;
    onImportExcel: (file: File) => boolean | Promise<boolean>;
    onDownloadTemplate: () => void;
    onExport: () => void;
}

const Actions: React.FC<ActionsProps> = ({
                                             onAdd,
                                             onImportExcel,
                                             onDownloadTemplate,
                                             onExport
                                         }) => {
    return (
        <Space align="center">
            {hasPermissionGroup(['GROUP_USER', 'USER_PRODUCT_CREATE']) && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAdd}
                >
                    Thêm sản phẩm
                </Button>
            )}

            {hasPermissionGroup(['GROUP_USER', 'USER_PRODUCT_IMPORT']) && (
                <><Upload beforeUpload={onImportExcel} showUploadList={false}>
                    <Button className="warning-button" icon={<UploadOutlined/>}>
                        Nhập từ Excel
                    </Button>
                </Upload><Button
                    icon={<DownloadOutlined/>}
                    onClick={onDownloadTemplate}
                >
                    File mẫu
                </Button></>
            )}

            {hasPermissionGroup(['GROUP_USER', 'USER_PRODUCT_EXPORT']) && (
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