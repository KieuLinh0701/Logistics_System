import {useEffect, useRef, useState} from "react";
import {Col, message, Row, Tag,} from "antd";
import {HistoryOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import Title from "antd/es/typography/Title";
import {useParams, useSearchParams} from "react-router-dom";
import Actions from "./components/Actions";
import SearchFilters from "./components/SearchFilters";
import type {AuditLog, AuditLogSearchRequest} from "../../../../types/auditLog.ts";
import auditLogApi from "../../../../api/auditLogApi.ts";
import AuditLogTable from "./components/Table";

const UserAuditLogsByUser = () => {
    const { id } = useParams();
    const userId = Number(id);
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const [loading, setLoading] = useState(false);

    const [log, setLog] = useState<AuditLog[] | []>([]);

    const [searchText, setSearchText] = useState("");
    const [filterAction, setFilterAction] = useState<string>("ALL");
    const [filterEntity, setFilterEntity] = useState<string>("ALL");
    const [filterStatus, setFilterStatus] = useState<string>("ALL");
    const [filterSort, setFilterSort] = useState<string>("NEWEST");
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [total, setTotal] = useState(0);
    const [hover, setHover] = useState(false);

    const updateURL = () => {
        const params: any = {};

        if (searchText) params.search = searchText;
        if (filterAction !== "ALL") params.action = filterAction.toLowerCase();
        if (filterEntity !== "ALL") params.entity = filterEntity.toLowerCase();
        if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
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
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");
        const action = searchParams.get("action")?.toLocaleUpperCase();
        const entity = searchParams.get("entity")?.toLocaleUpperCase();

        setCurrentPage(pageParam);
        if (s) setSearchText(s);
        if (status) setFilterStatus(status);
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
        if (userId === null) return;
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
                sort: filterSort,
            };

            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").toISOString();
                param.endDate = dateRange[1].endOf("day").toISOString();
            }

            const result = await auditLogApi.listUserAuditLogsByUserId(userId, param);
            if (requestId !== latestRequestRef.current) return;
            if (result.success && result.data) {
                const list = result.data?.list || [];
                setLog(list);
                setTotal(result.data.pagination?.total || 0);
            } else {
                message.error(result.message || "Lỗi khi lấy danh sách lịch sử hoạt động của nhân viên");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi lấy danh sách lịch sử hoạt động của nhân viên");
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async () => {
        if (userId === null) return;
        try {
            const param: AuditLogSearchRequest = {
                page: currentPage,
                limit: pageSize,
                search: searchText || undefined,
                action: filterAction !== "ALL" ? filterAction : undefined,
                entity: filterEntity !== "ALL" ? filterEntity : undefined,
                status: filterStatus !== "ALL" ? filterStatus : undefined,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await auditLogApi.exportUserByUserId(userId, param);

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
    }, [currentPage, pageSize, searchText, filterEntity, filterAction, filterStatus, filterSort, dateRange]);

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'sort':
                setFilterSort(value);
                break;
            case 'action':
                setFilterAction(value);
                break;
            case 'entity':
                setFilterEntity(value);
                break;
            case 'status':
                setFilterStatus(value);
                break;
        }
        setCurrentPage(1);
    };

    const handleClearFilters = () => {
        setSearchText('');
        setFilterAction('ALL');
        setFilterEntity('ALL');
        setFilterStatus('ALL');
        setFilterSort('NEWEST');
        setDateRange(null);
        setCurrentPage(1);
    };

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <SearchFilters
                    searchText={searchText}
                    filterAction={filterAction}
                    filterEntity={filterEntity}
                    filterStatus={filterStatus}
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
                            <HistoryOutlined className="title-icon"/>
                            Lịch sử hoạt động
                        </Title>
                    </Col>

                    <Col>
                        <div className="list-page-actions">
                            <Actions
                                onExport={handleExport}
                                total={total}
                            />
                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} bản ghi</Tag>

                <AuditLogTable
                    data={log}
                    page={currentPage}
                    limit={pageSize}
                    total={total}
                    loading={loading}
                    onPageChange={(page, size) => {
                        setCurrentPage(page);
                        if (size) setPageSize(size);
                    }}
                />
            </div>
        </div>
    );
};

export default UserAuditLogsByUser;