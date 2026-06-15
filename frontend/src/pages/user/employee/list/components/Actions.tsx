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
            {hasPermissionGroup(['GROUP_USER', 'USER_EMPLOYEE_CREATE']) && (
                <Button
                    className="primary-button"
                    icon={<PlusOutlined/>}
                    onClick={onAdd}
                    disabled={total >= 5}
                >
                    Thêm nhân viên
                </Button>
            )}
        </>
    );
};

export default Actions;