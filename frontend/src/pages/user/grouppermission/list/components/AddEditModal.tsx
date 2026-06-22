import React, {useEffect} from 'react';
import {Checkbox, Form, Input, Modal, Spin} from 'antd';
import type {Role} from "../../../../../types/role.ts";
import type {PermissionModule} from "../../../../../types/permissionModule.ts";
import type {PermissionGroup} from "../../../../../types/permissionGroup.ts";

interface AddEditModalProps {
    open: boolean;
    mode: 'create' | 'edit';
    role: Role | null;
    onOk: () => void;
    onCancel: () => void;
    onRoleChange: (role: Role) => void;
    form: any;
    permissionModules: PermissionModule[];
    loadingPermissionModule: boolean;
    loading: boolean;
}

const collectIds = (group: PermissionGroup): number[] => [
    group.id,
    ...(group.children ?? []).flatMap(collectIds),
];

const AddEditModal: React.FC<AddEditModalProps> = ({
                                                       open,
                                                       mode,
                                                       role,
                                                       onOk,
                                                       onCancel,
                                                       onRoleChange,
                                                       form,
                                                       loading,
                                                       permissionModules,
                                                       loadingPermissionModule
                                                   }) => {
    const selectedIds: number[] = role?.permissionGroupIds || [];

    const isGroupAllChecked = (group: PermissionGroup): boolean => {
        const ids = collectIds(group);
        return ids.every(id => selectedIds.includes(id));
    };

    const isGroupIndeterminate = (group: PermissionGroup): boolean => {
        const ids = collectIds(group);
        const checkedCount = ids.filter(id => selectedIds.includes(id)).length;
        return checkedCount > 0 && checkedCount < ids.length;
    };

    const isModuleAllChecked = (mod: PermissionModule) =>
        mod.permissionGroups.length > 0 &&
        mod.permissionGroups.every(g => isGroupAllChecked(g));

    const isModuleIndeterminate = (mod: PermissionModule) => {
        const allIds = mod.permissionGroups.flatMap(collectIds);
        const checkedCount = allIds.filter(id => selectedIds.includes(id)).length;
        return checkedCount > 0 && checkedCount < allIds.length;
    };

    const toggleGroup = (group: PermissionGroup, checked: boolean) => {
        const newIds = checked
            ? Array.from(new Set([...selectedIds, group.id]))
            : selectedIds.filter(id => id !== group.id);
        const formValues = form.getFieldsValue();
        onRoleChange({...role, ...formValues, permissionGroupIds: newIds});
    };

    const toggleChild = (child: PermissionGroup, parent: PermissionGroup, checked: boolean) => {
        const childIds = collectIds(child);
        let newIds: number[];
        if (checked) {
            newIds = Array.from(new Set([...selectedIds, parent.id, ...childIds]));
        } else {
            newIds = selectedIds.filter(id => !childIds.includes(id));
            const siblingIds = (parent.children ?? [])
                .filter(s => s.id !== child.id)
                .flatMap(collectIds);
            const hasOtherSelected = siblingIds.some(id => newIds.includes(id));
            if (!hasOtherSelected) {
                newIds = newIds.filter(id => id !== parent.id);
            }
        }
        const formValues = form.getFieldsValue();
        onRoleChange({...role, ...formValues, permissionGroupIds: newIds});
    };

    const toggleModule = (mod: PermissionModule, checked: boolean) => {
        const allIds = mod.permissionGroups.flatMap(collectIds);
        const newIds = checked
            ? Array.from(new Set([...selectedIds, ...allIds]))
            : selectedIds.filter(id => !allIds.includes(id));
        const formValues = form.getFieldsValue();
        onRoleChange({...role, ...formValues, permissionGroupIds: newIds});
    };

    const renderGroup = (group: PermissionGroup, parent?: PermissionGroup, depth = 0) => (
        <div key={group.id} className={depth === 0 ? 'permission-group-root' : ''}>
            <Checkbox
                checked={isGroupAllChecked(group)}
                indeterminate={isGroupIndeterminate(group)}
                onChange={(e) =>
                    parent
                        ? toggleChild(group, parent, e.target.checked)
                        : toggleGroup(group, e.target.checked)
                }
                style={{fontSize: 13, color: depth > 0 ? '#555' : '#333', margin: 0}}
            >
                {group.name}
            </Checkbox>

            {group.children?.length > 0 && (
                <div className="permission-children-grid">
                    {group.children.map(child => renderGroup(child, group, depth + 1))}
                </div>
            )}
        </div>
    );

    const renderModule = (mod: PermissionModule) => (
        <div key={mod.id} className="permission-module-card">
            <div className="permission-module-header">
                <span className="permission-module-dot"/>
                <span className="permission-module-title">{mod.name}</span>
                <Checkbox
                    indeterminate={isModuleIndeterminate(mod)}
                    checked={isModuleAllChecked(mod)}
                    onChange={(e) => toggleModule(mod, e.target.checked)}
                    className="permission-select-all-checkbox"
                >
                    <span style={{color: '#1C3D90', fontSize: 12, fontWeight: 500}}>Chọn tất cả</span>
                </Checkbox>
            </div>
            <div className="permission-checkbox-grid">
                {mod.permissionGroups.map(group => renderGroup(group))}
            </div>
        </div>
    );

    const half = Math.ceil(permissionModules.length / 2);
    const col1 = permissionModules.slice(0, half);
    const col2 = permissionModules.slice(half);

    useEffect(() => {
        if (!open) return;

        if (mode === 'edit' && role) {
            setTimeout(() => {
                form.setFieldsValue({
                    name: role.name,
                    description: role.description
                });
            }, 0);
        } else if (mode === 'create') {
            setTimeout(() => {
                form.resetFields();
            }, 0);
        }
    }, [open]); 

    return (
        <Modal
            title={<span
                className="modal-title">{mode === 'edit' ? 'Chỉnh sửa nhóm quyền' : 'Thêm nhóm quyền mới'}</span>}
            open={open}
            onOk={onOk}
            width={1200}
            onCancel={onCancel}
            okText={mode === 'edit' ? 'Cập nhật' : 'Thêm'}
            okButtonProps={{className: 'modal-ok-button', loading}}
            cancelButtonProps={{className: 'modal-cancel-button'}}
            cancelText="Hủy"
            className="modal-hide-scrollbar"
            maskClosable={false}
            keyboard={false}
        >
            <Form form={form} layout="vertical">
                {loadingPermissionModule ? (
                    <div style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        padding: '50px 0'
                    }}>
                        <Spin size="large" />
                        <div style={{ marginTop: '16px', color: '#1C3D90', fontSize: '14px' }}>
                            Đang tải danh mục phân quyền...
                        </div>
                    </div>
                ) : (
                    <>
                        <p className="permission-section-label">| Thông tin</p>
                        <hr className="separator"/>
                        <div className="permission-info-grid">
                            <Form.Item
                                label={<span className="modal-lable">Tên nhóm quyền</span>}
                                name="name"
                                rules={[{required: true, message: 'Nhập tên nhóm quyền!'}]}
                            >
                                <Input
                                    className="modal-custom-input"
                                    placeholder="Nhập tên nhóm quyền"
                                />
                            </Form.Item>
                            <Form.Item
                                label={<span className="modal-lable">Mô tả nhóm quyền</span>}
                                name="description"
                            >
                                <Input
                                    className="modal-custom-input"
                                    placeholder="Nhập mô tả chi tiết của nhóm quyền"
                                />
                            </Form.Item>
                        </div>

                        {permissionModules.length > 0 && (
                            <>
                                <p className="permission-section-label" style={{marginTop: 8}}>| Phân quyền</p>
                                <hr className="separator"/>
                                <div className="permission-modules-grid">
                                    <div>{col1.map(renderModule)}</div>
                                    <div>{col2.map(renderModule)}</div>
                                </div>
                            </>
                        )}
                    </>
                )}
            </Form>
        </Modal>
    );
};

export default AddEditModal;