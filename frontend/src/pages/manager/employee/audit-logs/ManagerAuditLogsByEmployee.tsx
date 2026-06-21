import {useEffect, useRef, useState} from "react";
import {
    message,
    Row,
    Col,
    Tag,
    Form,
} from "antd";
import {
    TeamOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import Title from "antd/es/typography/Title";
import {useNavigate, useParams, useSearchParams} from "react-router-dom";
import EmployeeTable from "./components/Table";
import Actions from "./components/Actions";
import SearchFilters from "./components/SearchFilters";
import AddEditModal from "./components/AddEditModal";
import type {AuditLog, AuditLogSearchRequest} from "../../../../types/auditLog.ts";
import auditLogApi from "../../../../api/auditLogApi.ts";

const ManagerAuditLogsByEmployee = () => {
    const { id } = useParams();
    const employeeId = Number(id);
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const [loading, setLoading] = useState(false);

    const [log, setLog] = useState<AuditLog[] | []>([]);

    const [searchText, setSearchText] = useState("");
    const [filterAction, setFilterAction] = useState<string>("ALL");
    const [filterEntity, setFilterEntity] = useState<string>("ALL");
    const [filterStatus, setFilterStatus] = useState<string>("ALL");
    const [filterSort, setFilterSort] = useState<string>("NEWEST");
    const [filterRole, setFilterRole] = useState<string>("ALL");
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [total, setTotal] = useState(0);
    const [hover, setHover] = useState(false);
    const [form] = Form.useForm();
    const navigate = useNavigate();

    const updateURL = () => {
        const params: any = {};

        if (searchText) params.search = searchText;
        if (filterAction !== "ALL") params.action = filterAction.toLowerCase();
        if (filterEntity !== "ALL") params.entity = filterEntity.toLowerCase();
        if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
        if (filterRole !== "ALL") params.role = filterRole;
        params.sort = filterSort.toLowerCase();
        if (currentPage) params.page = currentPage;

        if (dateRange) {
            params.start = dateRange[0].format("YYYY-MM-DD");
            params.end = dateRange[1].format("YYYY-MM-DD");
        }

        setSearchParams(params, {replace: true});
    };

    useEffect(() => {
        const pageParam = Number(searchParams.get("page")) || 1;
        const s = searchParams.get("search");
        const status = searchParams.get("status")?.toLocaleUpperCase();
        const r = searchParams.get("role");
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");
        const action = searchParams.get("action");
        const entity = searchParams.get("entity");

        setCurrentPage(pageParam);
        if (s) setSearchText(s);
        if (shift) setFilterShift(shift);
        if (status) setFilterStatus(status);
        if (r) setFilterRole(r);
        if (sort) setFilterSort(sort);
        if (action) setFilterAction(action);
        if (entity) setFilterEntity(entity);

        if (startDate && endDate) {
            setDateRange([
                dayjs(startDate, "YYYY-MM-DD"),
                dayjs(endDate, "YYYY-MM-DD")
            ]);
        }
    }, [searchParams]);

    const fetchLogs = async (page = currentPage) => {
        if (employeeId === null) return;
        try {
            setLoading(true);
            const requestId = ++latestRequestRef.current;
            const param: AuditLogSearchRequest = {
                page,
                limit: pageSize,
                search: searchText || undefined,
                action: filterAction !== "ALL" ? filterAction : undefined,
                entity: filterEntity !== "ALL" ? filterEntity : undefined,
                status: filterStatus !== "ALL" ? filterStatus : undefined,
                role: filterRole !== "ALL" ? filterRole : undefined,
                sort: filterSort,
            };

            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").toISOString();
                param.endDate = dateRange[1].endOf("day").toISOString();
            }

            const result = await auditLogApi.listManagerAuditLogsByEmployeeId(employeeId, param);
            if (requestId !== latestRequestRef.current) return;
            if (result.success && result.data) {
                const list = result.data?.list || [];
                setLog(list);
                setTotal(result.data.pagination?.total || 0);
            } else {
                message.error(result.message || "Lỗi khi lấy danh sách lịch sử làm việc của nhân viên");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi lấy danh sách lịch sử làm việc của nhân viên");
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async () => {
        try {
            const param: ManagerlogearchRequest = {
                page: currentPage,
                limit: pageSize,
                search: searchText || undefined,
                shift: filterShift !== "ALL" ? filterShift : undefined,
                status: filterStatus !== "ALL" ? filterStatus : undefined,
                role: filterRole !== "ALL" ? filterRole : undefined,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await employeeApi.exportManagerlog(param);


            if (!result.success) {
                console.error("Export thất bại:", result.error);
                message.error("Xuất file Excel thất bại");
            }

        } catch (error: any) {
            message.error("Xuất file Excel thất bại");
            console.error("Export thất bại:", error);
        }
    };

    useEffect(() => {
        fetchLogs(currentPage);
        updateURL();
    }, [currentPage, pageSize, searchText, filterShift, filterStatus, filterRole, filterSort, dateRange]);

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'sort':
                setFilterSort(value);
                break;
            case 'shift':
                setFilterShift(value);
                break;
            case 'role':
                setFilterRole(value);
                break;
            case 'status':
                setFilterStatus(value);
                break;
        }
        setCurrentPage(1);
    };

    const handleViewLog = async (id: number) => {
        navigate(`/log/${id}/logs`)
    }

    const handleClearFilters = () => {
        setSearchText('');
        setFilterShift('ALL');
        setFilterStatus('ALL');
        setFilterRole('ALL');
        setFilterSort('NEWEST');
        setDateRange(null);
        setCurrentPage(1);
    };

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <SearchFilters
                    searchText={searchText}
                    filterShift={filterShift}
                    filterStatus={filterStatus}
                    filterRole={filterRole}
                    filterSort={filterSort}
                    dateRange={dateRange}
                    hover={hover}
                    onSearchChange={setSearchText}
                    onFilterChange={handleFilterChange}
                    onDateRangeChange={setDateRange}
                    onClearFilters={handleClearFilters}
                    onHoverChange={setHover}
                />

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <TeamOutlined className="title-icon"/>
                            Danh sách nhân viên
                        </Title>
                    </Col>

                    <Col>
                        <div className="list-page-actions">
                            <Actions
                                onAdd={() => {
                                    setIsModalOpen(true);
                                    setModalMode("create");
                                    setNewEmployee({});
                                    form.resetFields();
                                }}
                                onExport={handleExport}
                                total={total}
                            />
                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} nhân viên</Tag>

                <EmployeeTable
                    data={log}
                    page={currentPage}
                    limit={pageSize}
                    total={total}
                    loading={loading}
                    onEdit={(employee) => {
                        setModalMode('edit');
                        setNewEmployee(employee);
                        setIsModalOpen(true);
                        form.setFieldsValue({
                            lastName: employee.lastName,
                            firstName: employee.firstName,
                            email: employee.email,
                            phoneNumber: employee.phoneNumber,
                            role: employee.role,
                            shift: employee.shift,
                            status: employee.status,
                            hireDate: dayjs(employee.hireDate)
                        });
                    }}
                    onViewLog={handleViewLog}
                    onPageChange={(page, size) => {
                        setCurrentPage(page);
                        if (size) setPageSize(size);
                    }}
                />

                <AddEditModal
                    open={isModalOpen}
                    mode={modalMode}
                    employee={newEmployee}
                    onOk={handleSubmit}
                    onCancel={() => setIsModalOpen(false)}
                    loading={loading}
                    form={form}
                />
            </div>
        </div>
    );
};

export default ManagerAuditLogsByEmployee;