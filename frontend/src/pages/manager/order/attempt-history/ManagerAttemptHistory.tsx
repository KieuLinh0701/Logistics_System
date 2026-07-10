import {useEffect, useRef, useState} from "react";
import {Col, message, Row, Tag} from "antd";
import dayjs from "dayjs";
import {HistoryOutlined} from "@ant-design/icons";
import Title from "antd/es/typography/Title";
import {useSearchParams} from "react-router-dom";
import attemptHistoryApi from "../../../../api/attemptHistoryApi";
import type {AttemptHistoryItem} from "../../../../types/attemptHistory";
import AttemptHistorySearchFilters from "./components/AttemptHistorySearchFilters";
import AttemptHistoryTable from "./components/AttemptHistoryTable";
import AttemptHistoryDetailModal from "./components/AttemptHistoryDetailModal";

const ManagerAttemptHistory = () => {
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();

    const [loading, setLoading] = useState(false);
    const [items, setItems] = useState<AttemptHistoryItem[]>([]);
    const [total, setTotal] = useState(0);
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);

    const [searchText, setSearchText] = useState("");
    const [filterCategory, setFilterCategory] = useState("ALL");
    const [filterStatus, setFilterStatus] = useState("ALL");
    const [filterSort, setFilterSort] = useState("NEWEST");
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

    const [selectedItem, setSelectedItem] = useState<AttemptHistoryItem | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const updateURL = () => {
        const params: Record<string, string> = {sort: filterSort.toLowerCase()};

        if (searchText) params.search = searchText;
        if (filterCategory !== "ALL") params.category = filterCategory.toLowerCase();
        if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
        if (currentPage) params.page = String(currentPage);

        if (dateRange) {
            params.start = dateRange[0].format("YYYY-MM-DD");
            params.end = dateRange[1].format("YYYY-MM-DD");
        }

        setSearchParams(params, {replace: true});
    };

    useEffect(() => {
        const pageParam = Number(searchParams.get("page")) || 1;
        const s = searchParams.get("search");
        const cat = searchParams.get("category")?.toUpperCase();
        const st = searchParams.get("status")?.toUpperCase();
        const sort = searchParams.get("sort")?.toUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");

        setCurrentPage(pageParam);
        if (s) setSearchText(s);
        if (cat) setFilterCategory(cat);
        if (st) setFilterStatus(st);
        if (sort) setFilterSort(sort);

        if (startDate && endDate) {
            setDateRange([dayjs(startDate, "YYYY-MM-DD"), dayjs(endDate, "YYYY-MM-DD")]);
        }
    }, [searchParams]);

    const fetchList = async (page = currentPage) => {
        const requestId = ++latestRequestRef.current;
        setLoading(true);
        try {
            const result = await attemptHistoryApi.listManagerAttemptHistory({
                page,
                limit: pageSize,
                search: searchText || undefined,
                category: filterCategory === "ALL" ? undefined : filterCategory,
                status: filterStatus === "ALL" ? undefined : filterStatus,
                sort: filterSort,
                startDate: dateRange ? dateRange[0].startOf("day").toISOString() : undefined,
                endDate: dateRange ? dateRange[1].endOf("day").toISOString() : undefined,
            });
            if (requestId !== latestRequestRef.current) return;
            if (result.success && result.data) {
                setItems(result.data.list || []);
                setTotal(result.data.pagination?.total || 0);
            } else {
                message.error(result.message || "Lỗi khi lấy danh sách lịch sử xử lý");
            }
        } catch (error: any) {
            message.error(error?.message || "Lỗi khi lấy danh sách lịch sử xử lý");
        } finally {
            if (requestId === latestRequestRef.current) {
                setLoading(false);
            }
        }
    };

    useEffect(() => {
        updateURL();
        fetchList(currentPage);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentPage, pageSize, searchText, filterCategory, filterStatus, filterSort, dateRange]);

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <AttemptHistorySearchFilters
                    searchText={searchText}
                    setSearchText={setSearchText}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                    filters={{category: filterCategory, sort: filterSort, status: filterStatus}}
                    setFilters={(key, val) => {
                        if (key === "category") setFilterCategory(val);
                        if (key === "status") setFilterStatus(val);
                        if (key === "sort") setFilterSort(val);
                        setCurrentPage(1);
                    }}
                    onReset={() => {
                        setSearchText("");
                        setFilterCategory("ALL");
                        setFilterStatus("ALL");
                        setFilterSort("NEWEST");
                        setDateRange(null);
                        setCurrentPage(1);
                    }}
                />

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <HistoryOutlined className="title-icon"/>
                            Lịch sử xử lý đơn
                        </Title>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} bản ghi</Tag>

                <AttemptHistoryTable
                    items={items}
                    currentPage={currentPage}
                    pageSize={pageSize}
                    total={total}
                    loading={loading}
                    onView={(item) => {
                        setSelectedItem(item);
                        setIsModalOpen(true);
                    }}
                    onPageChange={(page, size) => {
                        setCurrentPage(page);
                        if (size) setPageSize(size);
                    }}
                />

                <AttemptHistoryDetailModal
                    item={selectedItem}
                    open={isModalOpen}
                    onClose={() => {
                        setIsModalOpen(false);
                        setSelectedItem(null);
                    }}
                />
            </div>
        </div>
    );
};

export default ManagerAttemptHistory;
