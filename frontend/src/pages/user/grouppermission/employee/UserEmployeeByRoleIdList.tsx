import React, {useEffect, useRef, useState} from 'react';
import {Col, message, Row, Tag} from 'antd';
import Title from 'antd/es/typography/Title';
import {TeamOutlined} from '@ant-design/icons';
import userApi from "../../../../api/userApi.ts";
import type {User} from "../../../../types/user.ts";
import dayjs from "dayjs";
import {useParams, useSearchParams} from "react-router-dom";
import type {UserRoleSearchRequest} from "../../../../types/role.ts";
import EmployeeTable from "./components/Table";
import SearchFilters from "./components/SearchFilters.tsx";

const UserEmployeeByRoleIdList: React.FC = () => {
    const [employees, setEmployees] = useState<User[]>([]);
    const [loading, setLoading] = useState(false);

    const { id } = useParams<{ id: string }>();
    const roleId = Number(id);
    const [search, setSearch] = useState("");
    const [limit, setLimit] = useState(10);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState<number>(0);
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

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

    const handleUpdateIsActive = async (id: number, isActive: boolean) => {
        try {
            const result = await userApi.updateUserIsActive(id, isActive, roleId);

            if (result.success) {
                message.success(result.message || 'Đặt tài khoản mặc định thành công!');
                fetchEmployees();
            } else {
                message.error(result.message || 'Cập nhật mặc định thất bại!');
            }
        } catch (error: any) {
            message.error(error.message || "Có lỗi khi cập nhật mặc định!");
        }
    };

    const fetchEmployees = async (currentPage = page) => {
        if (!roleId) { return }
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

            const result = await userApi.listUserEmployeesByRoleId(param, roleId);

            if (requestId !== latestRequestRef.current) return;

            if (result && result.success && result.data) {
                setEmployees(result.data?.list || []);
                setTotal(result.data.pagination?.total || 0);
            } else {
                setEmployees([]);
                setTotal(0);
                message.error("Có lỗi khi lấy danh sách nhân viên");
            }
        } catch (error: any) {
            setEmployees([]);
            setTotal(0);
            message.error(error.message || "Có lỗi khi lấy danh sách nhân viên");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        updateURL();
        fetchEmployees();
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
                            <TeamOutlined className="title-icon"/>
                            Danh sách nhân viên</Title>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} nhân viên</Tag>

                <EmployeeTable
                    data={employees}
                    onSetActive={handleUpdateIsActive}
                    page={page}
                    total={total}
                    loading={loading}
                    limit={limit}
                    onPageChange={(page, size) => {
                        setPage(page);
                        if (size) setLimit(size);
                    }}
                />
            </div>
        </div>
    );
};

export default UserEmployeeByRoleIdList;