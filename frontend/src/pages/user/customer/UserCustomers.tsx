import React, {useEffect, useRef, useState} from 'react';
import {Col, Form, message, Row, Tag} from 'antd';
import {ContactsOutlined} from '@ant-design/icons';
import AddressTable from './components/AddressTable';
import AddressModal from './components/AddressModal';
import recipientAddressApi from "../../../api/recipientAddressApi.ts";
import type {RecipientAddressRequest, RecipientAddressWithStats} from "../../../types/recipientAddress.ts";
import dayjs from "dayjs";
import {useSearchParams} from "react-router-dom";
import Title from "antd/es/typography/Title";
import Actions from "./components/Actions.tsx";
import SearchFilters from "./components/SearchFilters.tsx";
import type {Address} from "../../../types/address.ts";
import type {SearchRequest} from "../../../types/request.ts";

const UserCustomers: React.FC = () => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [addresses, setAddresses] = useState<RecipientAddressWithStats[]>([]);
    const [editingAddress, setEditingAddress] = useState<RecipientAddressRequest | null>(null);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
    const [modalKey, setModalKey] = useState(0);

    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();

    const [limit, setLimit] = useState(10);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState<number>(0);
    const [search, setSearch] = useState("");
    const [filterSort, setFilterSort] = useState("NEWEST");
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

    const updateURL = () => {
        const params: any = {};

        if (search) params.search = search;
        params.sort = filterSort.toLowerCase();
        if (page >= 1) params.page = page;

        if (dateRange) {
            params.start = dateRange[0].format("YYYY-MM-DD");
            params.end = dateRange[1].format("YYYY-MM-DD");
        }

        setSearchParams(params, { replace: true });
    };

    useEffect(() => {
        const pageParam = Number(searchParams.get("page")) || 1;
        const s = searchParams.get("search");
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");

        setPage(pageParam);
        if (s) setSearch(s);
        if (sort) setFilterSort(sort);

        if (startDate && endDate) {
            setDateRange([
                dayjs(startDate, "YYYY-MM-DD"),
                dayjs(endDate, "YYYY-MM-DD")
            ]);
        }
    }, [searchParams]);

    const fetchAddresses = async (currentPage = page) => {
        try {
            const requestId = ++latestRequestRef.current;
            setLoading(true);

            const param: SearchRequest = {
                page: currentPage,
                limit: limit,
                search: search,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const response = await recipientAddressApi.getUserAddresses(param);

            if (requestId !== latestRequestRef.current) return;

            if (response.success && response.data) {
                setAddresses(response.data.list);
                setTotal(response.data.list.length);
            } else {
                message.error(response.message || "Lỗi khi lấy danh sách khách hàng");
            }
        } catch (error) {
            console.error("Error fetching Addresses:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async () => {
        try {
            const param: SearchRequest = {
                page: page,
                limit: limit,
                search: search,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await recipientAddressApi.exportUserAddresses(param);


            if (!result.success) {
                console.error("Export thất bại:", result.error);
            }

        } catch (error: any) {
            message.error(error.message || "Xuất Excel thất bại!");
        }
    };

    const handleClearFilters = () => {
        setSearch("");
        setFilterSort("NEWEST");
        setDateRange(null);
        setPage(1);
    };

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'sort':
                setFilterSort(value);
                break;
        }
        setPage(1);
    };

    useEffect(() => {
        updateURL();
        fetchAddresses();
    }, [
        page,
        limit,
        search,
        dateRange,
        filterSort
    ]);

    const showModal = (mode: 'create' | 'edit', address?: Address) => {
        setModalMode(mode);

        if (mode === 'edit' && address) {
            setEditingAddress(address);
            form.resetFields();
            form.setFieldsValue({
                ...address,
                address: {
                    cityCode: address.cityCode || undefined,
                    wardCode: address.wardCode || undefined,
                    detail: address.detail || ''
                }
            });
        } else {
            const emptyAddress = {
                name: '',
                phoneNumber: '',
                detail: '',
                wardCode: 0,
                wardName: '',
                cityCode: 0,
                cityName: '',
                latitude: 0,
                longitude: 0,
            };
            setEditingAddress(emptyAddress);
            form.resetFields();
            form.setFieldsValue({
                ...emptyAddress,
                address: { cityCode: undefined, wardCode: undefined, detail: '' }
            });
        }

        setModalKey(prev => prev + 1);
        setIsModalVisible(true);
    };

    const handleCancel = () => {
        setIsModalVisible(false);
        setEditingAddress(null);
        form.resetFields();
        form.setFieldsValue({
            address: { cityCode: undefined, wardCode: undefined, detail: 'Hello' }
        });
    };

    const handleSaveAddress = async () => {
        try {
            setLoading(true);

            const values = await form.validateFields();

            const payload: RecipientAddressRequest = {
                name: values.name,
                phoneNumber: values.phoneNumber,
                cityCode: values.address.cityCode,
                wardCode: values.address.wardCode,
                detail: values.address.detail,
                latitude: values.address.latitude,
                longitude: values.address.longitude,
                cityName: values.address.cityName,
                wardName: values.address.wardName,
            };

            console.log("Payload to save:", payload);

            if (modalMode === 'edit' && editingAddress?.id) {
                const response = await recipientAddressApi.updateUserAddress(editingAddress.id, payload);

                if (response.success && response.data) {
                    fetchAddresses();
                    message.success('Cập nhật địa chỉ thành công!');
                    handleCancel();
                } else {
                    message.error(response.message || "Cập nhật địa chỉ thất bại");
                }
            } else {
                const response = await recipientAddressApi.createUserAddress(payload);

                if (response.success && response.data) {
                    fetchAddresses();
                    message.success('Thêm địa chỉ thành công!');
                    handleCancel();
                } else {
                    message.error(response.message || "Thêm địa chỉ thất bại");
                }
            }

        } catch (error) {
            console.error("Error saving address:", error);
            message.error('Vui lòng kiểm tra lại thông tin!');
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteAddress = async (addressId: number) => {
        try {
            setLoading(true);
            const response = await recipientAddressApi.deleteUserAddress(addressId);
            if (response.success && response.data) {
                fetchAddresses();
                message.success('Xóa địa chỉ thành công');
            } else {
                message.error(response.message || "Xóa địa chỉ thành công");
            }
        } catch (error) {
            message.error("Có lỗi khi xóa địa chỉ");
            console.error("Error Delete Addresses:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleAddressChange = (address: RecipientAddressRequest) => {
        setEditingAddress(address);
    };

    const handleShowModal = () => {
        showModal("create");
    };

    return (
        <div className="list-page-layout">
            <div className="list-page-content">

                <SearchFilters
                    search={search}
                    setSearch={setSearch}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                    filters={{ sort: filterSort }}
                    setFilters={handleFilterChange}
                    onReset={handleClearFilters}
                />

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <ContactsOutlined className="title-icon" />
                            Danh sách khách hàng
                        </Title>
                    </Col>
                    <Col>
                        <div className="list-page-actions">
                            <Actions
                                onAdd={handleShowModal}
                                onExport={handleExport}
                            />
                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} khách hàng</Tag>

                <AddressTable
                    data={addresses}
                    onEdit={(address) => showModal('edit', address)}
                    onDelete={handleDeleteAddress}
                    page={page}
                    total={total}
                    loading={loading}
                    limit={limit}
                    onPageChange={(page, size) => {
                        setPage(page);
                        if (size) setLimit(size);
                    }}
                />

                <AddressModal
                    key={modalKey}
                    open={isModalVisible}
                    mode={modalMode}
                    address={editingAddress || {
                        name: '',
                        phoneNumber: '',
                        detail: '',
                        wardCode: 0,
                        wardName: '',
                        cityCode: 0,
                        cityName: '',
                        latitude: 0,
                        longitude: 0,
                    }}
                    onOk={handleSaveAddress}
                    onCancel={handleCancel}
                    onAddressChange={handleAddressChange}
                    form={form}
                    total={addresses.length}
                    loading={loading}
                />
            </div>
        </div>
    );
};

export default UserCustomers;