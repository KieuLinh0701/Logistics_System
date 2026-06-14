import React from 'react';
import {Button} from 'antd';
import {PlusOutlined} from '@ant-design/icons';
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface ActionsProps {
    onAdd: () => void;
    total: number;
}

const Actions: React.FC<ActionsProps> = ({
                                             onAdd, total
                                         }) => {
    return (
        <>
            {hasPermissionGroup(['GROUP_USER', 'USER_PERMISSION_GROUP_CREATE']) && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAdd}
                    disabled={total >= 5}
                >
                    Thêm nhóm quyền
                </Button>
            )}
        </>
    );
};

export default Actions;