import React from "react";
import {Space, Button} from "antd";
import {PlusOutlined} from "@ant-design/icons";
import {hasPermissionGroup} from "../../../../utils/authUtils.ts";

interface Props {
    onAdd: () => void;
}

const Actions: React.FC<Props> = ({onAdd}) => {
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
        </Space>
    );
};

export default Actions;