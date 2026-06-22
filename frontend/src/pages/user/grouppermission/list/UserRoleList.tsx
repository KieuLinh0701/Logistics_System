import React, {useEffect, useRef, useState} from 'react';
import {Col, Form, message, Row, Tag} from 'antd';
import Actions from './components/Actions';
import RoleTable from './components/Table';
import AddEditModal from './components/AddEditModal';
import Title from 'antd/es/typography/Title';
import {UserSwitchOutlined} from '@ant-design/icons';
import "./UserRoleList.css"
import type {Role, UserRoleSearchRequest} from "../../../../types/role.ts";
import {useNavigate, useSearchParams} from "react-router-dom";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters";
import roleApi from "../../../../api/roleApi.ts";
import type {PermissionModule} from "../../../../types/permissionModule.ts";
import permissionModuleApi from "../../../../api/permissionModuleApi.ts";

const UserRoleList: React.FC = () => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
    const [form] = Form.useForm();
    const [roles, setRoles] = useState<Role[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingModal, setLoadingModal] = useState(false);
    const [loadingPermissionModule, setLoadingermissionModule] = useState(false);
    const [search, setSearch] = useState("");
    const [limit, setLimit] = useState(10);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState<number>(0);
    const navigate = useNavigate();
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

    const [newRole, setNewRole] = useState<Role | null>(null);
    const [permissionModules, setPermissionModules] = useState<PermissionModule[]>([]);

    const updateURL = () => {
        const params: any = {};

        if (search) params.search = search;
        if (page >= 1) params.page = page;
        if (dateRange) {
            params.start = dateRange[0].format("YYYY-MM-DD");
            params.end = dateRange[1].format("YYYY-MM-DD");
        }
        setSearchParams(params, {replace: true});
    };

    useEffect(() => {
        const pageParam = Number(searchParams.get("page")) || 1;
        const s = searchParams.get("search");
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");

        setPage(pageParam);
        if (s) setSearch(s);

        if (startDate && endDate) {
            setDateRange([
                dayjs(startDate, "YYYY-MM-DD"),
                dayjs(endDate, "YYYY-MM-DD")
            ]);
        }
    }, [searchParams]);

    const openCreateModal = () => {
        fetchPermissionModules();
        setModalMode('create');
        setNewRole({ id: undefined, name: '', description: '', permissionGroupIds: [] });
        form.resetFields();
        setIsModalOpen(true);
    };

    const openEditModal = async (roleId: number) => {
        fetchPermissionModules();
        try {

            const result = await roleApi.getUserRoleById(roleId);

            if (result && result.success && result.data) {
                setNewRole(result.data);
                setModalMode('edit');
                setIsModalOpen(true);
            } else {
                setNewRole(null);
                message.error(result.message || "Lỗi khi lấy thông tin chi tiết nhóm quyền");
            }
        } catch (error: any) {
            setNewRole(null);
            message.error(error.message || "Có lỗi khi lấy thong tin chi tiết nhóm quyền");
        }
    };

    const handleAddRole = async () => {
        if (newRole == null) return;
        setLoadingModal(true);
        try {

            const values = await form.validateFields();
            const payload = {
                ...newRole,
                name: values.name,
                description: values.description,
            };
            const result = await roleApi.createUserRole(payload);
            if (result.success) {
                message.success("Thêm thành công!");
                fetchRoles();
                setIsModalOpen(false);
                setNewRole(null);
                form.resetFields();
            }
            else {
                message.error(result.message || "Có lỗi khi thêm");
            }
        } catch (error: any) {
            if (error?.errorFields) return;
            message.error(error.message || "Có lỗi khi thêm");
        } finally {
            setLoadingModal(false);
        }
    };

    const handleEditRole = async () => {
        if (newRole == null) return;
        setLoadingModal(true);
        try {
            const values = await form.validateFields();
            const payload = {
                ...newRole,
                name: values.name,
                description: values.description,
            };
            const result = await roleApi.updateUserRole(newRole.id!, payload);
            if (result.success) {
                message.success("Cập nhật thành công!");
                fetchRoles();
                setIsModalOpen(false);
                setNewRole(null);
                form.resetFields();
            }
            else {
                message.error(result.message || "Có lỗi khi cập nhật");
            }
        } catch (error: any) {
            if (error?.errorFields) return;
            message.error(error.message || "Có lỗi khi cập nhật");
        } finally {
            setLoadingModal(false);
        }
    };

    const handleDeleteRole = async (id: number) => {
        try {
            const result = await roleApi.deleteUserRole(id);

            if (result.success) {
              message.success('Xóa thành công!');
              fetchRoles();
            } else {
              message.error(result.message || 'Xóa thất bại!');
            }
        } catch (error: any) {
            message.error(error.message || 'Có lỗi khi xóa tài khoản!');
        }
    };

    const fetchPermissionModules = async () => {
        try {
            setLoadingermissionModule(true)
            const result = await permissionModuleApi.listUserActivePermissionModules();

            if (result && result.success && result.data) {
                setPermissionModules(result.data || []);
            } else {
                setPermissionModules([]);
                message.error(result.message || "Lỗi khi lấy danh sách danh mục phân quyền");
            }
        } catch (error: any) {
            setPermissionModules([]);
            message.error(error.message || "Có lỗi khi lấy danh sách danh mục phân quyền");
        } finally {
            setLoadingermissionModule(false);
        }
    };

    const fetchRoles = async (currentPage = page) => {
        try {
            const requestId = ++latestRequestRef.current;
            setLoading(true);

            const param: UserRoleSearchRequest = {
                page: currentPage,
                limit: limit,
                search: search,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await roleApi.listUserRoles(param);

            if (requestId !== latestRequestRef.current) return;


            if (result && result.success && result.data) {
                setRoles(result.data?.list || []);
                setTotal(result.data.pagination?.total || 0);
            } else {
                setRoles([]);
                setTotal(0);
                message.error(result.message || "Lỗi khi lấy danh sách quyền");
            }
        } catch (error: any) {
            setRoles([]);
            setTotal(0);
            message.error(error.message || "Có lỗi khi lấy danh sách quyền");
        } finally {
            setLoading(false);
        }
    };

    const handleViewRoleUsers = (id: number) => {
        if (!id) return;
        navigate(`/roles/${id}/employees`);
    };

    useEffect(() => {
        updateURL();
        fetchRoles();
    }, [
        page,
        limit,
        search,
        dateRange
    ]);

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <SearchFilters
                    search={search}
                    setSearch={setSearch}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                />

                <Row justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <UserSwitchOutlined className="title-icon"/>
                            Danh sách nhóm quyền
                        </Title>
                    </Col>
                    <Col>
                        <div className="list-page-actions">
                            <Actions onAdd={openCreateModal} total={total}/>
                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} nhóm</Tag>

                <RoleTable
                    data={roles}
                    onEdit={openEditModal}
                    onDelete={handleDeleteRole}
                    page={page}
                    total={total}
                    loading={loading}
                    limit={limit}
                    onPageChange={(page, size) => {
                        setPage(page);
                        if (size) setLimit(size);
                    }}
                    onViewUsersByRole={handleViewRoleUsers}
                />

                <AddEditModal
                    open={isModalOpen}
                    mode={modalMode}
                    role={newRole}
                    onOk={modalMode === 'edit' ? handleEditRole : handleAddRole}
                    onCancel={() => {
                        setIsModalOpen(false);
                        setLoadingModal(false);
                    }}
                    onRoleChange={setNewRole}
                    form={form}
                    permissionModules={permissionModules}
                    loading={loadingModal}
                    loadingPermissionModule={loadingPermissionModule}
                />
            </div>
        </div>
    );
};

export default UserRoleList;