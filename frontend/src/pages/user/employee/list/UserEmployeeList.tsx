import React, {useEffect, useRef, useState} from 'react';
import {Col, Form, message, Row, Tag} from 'antd';
import Actions from './components/Actions.tsx';
import AddEditModal from './components/AddEditModal.tsx';
import Title from 'antd/es/typography/Title';
import {UserSwitchOutlined} from '@ant-design/icons';
import type {Role} from "../../../../types/role.ts";
import {useNavigate, useSearchParams} from "react-router-dom";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters.tsx";
import roleApi from "../../../../api/roleApi.ts";
import type {User, UserEmployeeSearchRequest} from "../../../../types/user.ts";
import userApi from "../../../../api/userApi.ts";
import DataTable from "./components/Table.tsx";
import {getActiveValue} from "../../../../utils/userUtils.ts";

const UserEmployeeList: React.FC = () => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
    const [form] = Form.useForm();
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingModal, setLoadingModal] = useState(false);
    const [search, setSearch] = useState("");
    const [limit, setLimit] = useState(10);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState<number>(0);
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

    const [user, setUser] = useState<User | null>(null);
    const [roles, setRoles] = useState<Role[]>([]);

    const [filterSort, setFilterSort] = useState("NEWEST");
    const [filterActive, setFilterActive] = useState("ALL");

    const navigate = useNavigate();

    const updateURL = () => {
        const params: any = {};

        if (search) params.search = search;
        params.active = filterSort.toLowerCase();
        params.sort = filterActive.toLowerCase();
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
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const active = searchParams.get("active")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");

        setPage(pageParam);
        if (s) setSearch(s);
        if (sort) setFilterSort(sort);
        if (active) setFilterActive(active);

        if (startDate && endDate) {
            setDateRange([
                dayjs(startDate, "YYYY-MM-DD"),
                dayjs(endDate, "YYYY-MM-DD")
            ]);
        }
    }, []);

    const openCreateModal = () => {
        fetchRoles();
        setModalMode('create');
        setUser(null);
        form.resetFields();
        setIsModalOpen(true);
    };

    const openEditModal = async (user: User) => {
        fetchRoles();

        setUser(user);
        form.setFieldsValue({
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            phoneNumber: user.phoneNumber,
            roleId: user.roleId
        });
        setModalMode('edit');
        setIsModalOpen(true);
    };

    const handleAddRole = async () => {
        setLoadingModal(true);
        try {

            const values = await form.validateFields();
            const result = await userApi.createUserUser(values);
            if (result.success) {
                message.success("Thêm thành công!");
                fetchData();
                setIsModalOpen(false);
                setUser(null);
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
        setLoadingModal(true);
        try {
            const values = await form.validateFields();

            const result = await userApi.updateUserUser(user.id!, values);
            if (result.success) {
                message.success("Cập nhật thành công!");
                fetchData();
                setIsModalOpen(false);
                setUser(null);
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

    const handleViewRolesOfEmployee = async (id: number) => {
        if (!id) return;
        navigate(`/employees/${id}/work-history`);
    }

    const fetchRoles = async () => {
        try {

            const result = await roleApi.listUserAllRoles();

            if (result && result.success && result.data) {
                setRoles(result.data || []);
            } else {
                setRoles([]);
                message.error(result.message || "Lỗi khi lấy danh sách nhóm quyền");
            }
        } catch (error: any) {
            setRoles([]);
            message.error(error.message || "Có lỗi khi lấy danh sách nhóm quyền");
        }
    };

    const fetchData = async (currentPage = page) => {
        try {
            const requestId = ++latestRequestRef.current;
            setLoading(true);

            const param: UserEmployeeSearchRequest = {
                page: currentPage,
                limit: limit,
                search: search,
                sort: filterSort,
                active: getActiveValue(filterActive)
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await userApi.listUserEmployees(param);

            if (requestId !== latestRequestRef.current) return;

            if (result && result.success && result.data) {
                setUsers(result.data?.list || []);
                setTotal(result.data.pagination?.total || 0);
            } else {
                setUsers([]);
                setTotal(0);
                message.error(result.message || "Lỗi khi lấy danh sách quyền");
            }
        } catch (error: any) {
            setUsers([]);
            setTotal(0);
            message.error(error.message || "Có lỗi khi lấy danh sách quyền");
        } finally {
            setLoading(false);
        }
    };

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'sort':
                setFilterSort(value);
                break;
            case 'active':
                setFilterActive(value);
                break;
        }
        setPage(1);
    };

    const handleClearFilters = () => {
        setSearch("");
        setFilterActive("ALL");
        setFilterSort("NEWEST");
        setDateRange(null);
        setPage(1);
    };

    useEffect(() => {
        setPage(1);
    }, [search]);


    useEffect(() => {
        updateURL();
        fetchData(page);
    }, [
        page,
        limit,
        search,
        filterActive,
        filterSort,
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
                    filters={{ sort: filterSort, active: filterActive }}
                    setFilters={handleFilterChange}
                    onReset={handleClearFilters}
                />

                <Row justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <UserSwitchOutlined className="title-icon"/>
                            Danh sách nhân viên
                        </Title>
                    </Col>
                    <Col>
                        <div className="list-page-actions">
                            <Actions onAdd={openCreateModal} total={total}/>
                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} nhân viên</Tag>

                <DataTable
                    data={users}
                    onEdit={openEditModal}
                    page={page}
                    total={total}
                    loading={loading}
                    limit={limit}
                    onViewRoles={handleViewRolesOfEmployee}
                    onPageChange={(page, size) => {
                        setPage(page);
                        if (size) setLimit(size);
                        fetchData(page);
                    }}
                />

                <AddEditModal
                    open={isModalOpen}
                    mode={modalMode}
                    onOk={modalMode === 'edit' ? handleEditRole : handleAddRole}
                    onCancel={() => {
                        setIsModalOpen(false);
                        setLoadingModal(false);
                    }}
                    form={form}
                    roles={roles}
                    loading={loadingModal}
                />
            </div>
        </div>
    );
};

export default UserEmployeeList;