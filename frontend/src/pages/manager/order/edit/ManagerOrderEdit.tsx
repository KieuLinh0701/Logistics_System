import React, {useEffect, useState} from "react";
import {Col, Form, message, Row} from "antd";
import Header from "./components/Header";
import Actions from "./components/Actions";
import RecipientInfo from "./components/RecipientInfo";
import NoteCard from "./components/NoteCard";
import PaymentCard from "./components/PaymentCard";
import OrderInfo from "./components/OrderInfo";
import SenderInfo from "./components/SenderInfo";
import PickupType from "./components/PickupType";
import PromotionCard from "./components/PromotionCard";
import type {ManagerOrderRequest, Order} from "../../../../types/order";
import type {OrderProduct} from "../../../../types/orderProduct";
import type {Office} from "../../../../types/office";
import type {ServiceType} from "../../../../types/serviceType";
import "./ManagerOrderEdit.css"
import serviceTypeApi from "../../../../api/serviceTypeApi";
import shippingFeeApi from "../../../../api/shippingFeeApi";
import officeApi from "../../../../api/officeApi";
import orderApi from "../../../../api/orderApi";
import {useNavigate, useParams} from "react-router-dom";
import {type OrderCreatorType, type OrderStatus} from "../../../../utils/orderUtils";
import ConfirmModal from "../../../common/ConfirmModal";

const ManagerOrderEdit: React.FC = () => {
    const { trackingNumber } = useParams();
    const navigate = useNavigate();

    const [order, setOrder] = useState<Order | null>(null);

    const [form] = Form.useForm();
    const [loadingOrder, setLoadingOrder] = useState(false);
    const [loadingOrderView, setLoadingOrderView] = useState(false);

    const [loadingOffice, setLoadingOffice] = useState(false);

    // Dịch vụ vận chuyển đang hoạt động
    const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([])
    const [selectedServiceType, setSelectedServiceType] = useState<ServiceType | null>(null);
    const [loadingService, setLoadingService] = useState(false);

    // Sản phẩm
    const [orderProducts, setOrderProducts] = useState<OrderProduct[]>([]);

    // Văn phòng nếu người dùng chọn đến bưu cục để gửi 
    const [localOffices, setLocalOffices] = useState<Office[]>([]);
    const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);

    // Gía trị khác đơn hàng
    const [orderValue, setOrderValue] = useState<number | undefined>(undefined);
    const [weight, setWeight] = useState<number | undefined>(undefined);
    const [codAmount, setCodAmount] = useState<number | undefined>(undefined);
    const [totalFee, setTotalFee] = useState<number | undefined>(undefined);
    const [payer, setPayer] = useState<string>("CUSTOMER");
    const [discountAmount, setDiscountAmount] = useState<number | undefined>(undefined);
    const [notes, setNotes] = useState("");
    const [_, setShippingFee] = useState<number | undefined>(undefined);
    const [pickupType, setPickupType] = useState<string | undefined>(undefined);
    const [serviceFee, setServiceFee] = useState<number | undefined>(undefined);
    const [originalWeight, setOriginalWeight] = useState<number | undefined>(undefined);
    const [length, setLength] = useState<number | undefined>(undefined);
    const [width, setWidth] = useState<number | undefined>(undefined);
    const [height, setHeight] = useState<number | undefined>(undefined);
    const [adjustedWeight, setAdjustedWeight] = useState<number | undefined>(undefined);
    const [adjustedOriginalWeight, setAdjustedOriginalWeight] = useState<number | undefined>(undefined);
    const [adjustedHeight, setAdjustedHeight] = useState<number | undefined>(undefined);
    const [adjustedLength, setAdjustedLength] = useState<number | undefined>(undefined);
    const [adjustedWidth, setAdjustedWidth] = useState<number | undefined>(undefined);
    const [modalConfirmOpen, setModalConfirmOpen] = useState(false);

    // Người gửi và người nhận
    const createEmptyAddress = () => ({
        name: "",
        phoneNumber: "",
        detail: "",
        wardCode: 0,
        wardName: "",
        cityCode: 0,
        cityName: "",
        latitude: 0,
        longitude: 0,
    });

    const [senderData, setSenderData] = useState(createEmptyAddress());
    const [recipientData, setRecipientData] = useState(createEmptyAddress());

    // Biến kiểm tra xem địa chỉ người nhận có tỉnh thành nằm trong khu vực hoạt động không
    const [isHasOfficeRecipient, setIsHasOfficerRecipient] = useState(true);

    // Form instances
    const [senderInfo] = Form.useForm();
    const [recipientInfo] = Form.useForm();
    const [paymentCard] = Form.useForm();
    const [fromOffice] = Form.useForm();
    const [orderInfo] = Form.useForm();

    // Đơn hàng để chỉnh sửa
    const fetchOrder = async () => {
        setLoadingOrderView(true);
        try {
            let result;
            if (trackingNumber) {
                result = await orderApi.getManagerOrderByTrackingNumber(trackingNumber);
            } else {
                return;
            }

            if (result.success) {
                setOrder(result.data);
            } else {
                message.error(result.message);
            }

        } catch (error: any) {
            message.error(error.message || "Lỗi tải đơn hàng");
        } finally {
            setLoadingOrderView(false);
        }
    };

    useEffect(() => {
        fetchOrder();
    }, [trackingNumber]);

    // Dịch vụ
    useEffect(() => {
        const fetchServiceTypes = async () => {
            try {
                setLoadingService(true);
                const response = await serviceTypeApi.getActiveServiceTypes();
                if (response.success && response.data) {
                    setServiceTypes(response.data);
                }
            } catch (error: any) {
                message.error(error.message || "Lấy danh sách dịch vụ thất bại");
            } finally {
                setLoadingService(false);
            }
        };

        fetchServiceTypes();
    }, []);

    useEffect(() => {
        if (!order) return;

        setSenderData({
            name: order.senderName || '',
            phoneNumber: order.senderPhone || '',
            detail: order?.senderDetail || '',
            cityCode: order?.senderCityCode ?? 0,
            cityName: order?.senderCityName || '',
            wardCode: order?.senderWardCode ?? 0,
            wardName: order?.senderWardName || '',
            latitude: order?.senderLatitude || 0,
            longitude: order?.senderLongitude || 0,
        });

        setRecipientData({
            name: order.recipientName || '',
            phoneNumber: order.recipientPhone || '',
            detail: order.recipientDetail || '',
            cityCode: order.recipientCityCode ?? 0,
            cityName: order?.recipientCityName || '',
            wardCode: order?.recipientWardCode ?? 0,
            wardName: order?.recipientWardName || '',
            latitude: order?.recipientLatitude || 0,
            longitude: order?.recipientLongitude || 0,
        });

        setSelectedServiceType(order.serviceType);
        setWeight(order.weight);
        setOriginalWeight(order.originalWeight)
        setLength(order.length)
        setWidth(order.width)
        setHeight(order.height)
        setAdjustedWeight(order.adjustedWeight)
        setAdjustedOriginalWeight(order.adjustedOriginalWeight)
        setAdjustedLength(order.adjustedLength)
        setAdjustedWidth(order.adjustedWidth)
        setAdjustedHeight(order.adjustedHeight)
        setCodAmount(order.cod);
        setOrderValue(order.orderValue);
        setPickupType(order.pickupType);
        setSelectedOffice(order.fromOffice);
        setPayer(order.payer);
        setDiscountAmount(order.discountAmount);
        setShippingFee(order.shippingFee);
        setServiceFee(order.totalFee + order.discountAmount);
        setTotalFee(order.totalFee);
        setOrderProducts(order.orderProducts);
        setNotes(order.notes);
    }, [order]);

    const handleOrderInfoChange = (changedValues: any) => {
        if (changedValues.adjustedWeight !== undefined) {
            setAdjustedWeight(changedValues.adjustedWeight);
            if (order?.createdByType === 'USER') {
                setDiscountAmount(0);
            }
        }

        if (changedValues.adjustedOriginalWeight !== undefined) {
            setAdjustedOriginalWeight(changedValues.adjustedOriginalWeight)
        }

        if (changedValues.adjustedHeight !== undefined) {
            setAdjustedHeight(changedValues.adjustedHeight)
        }

        if (changedValues.adjustedWidth !== undefined) {
            setAdjustedWidth(changedValues.adjustedWidth)
        }

        if (changedValues.adjustedOriginalWeight !== undefined) {
            setAdjustedOriginalWeight(changedValues.adjustedOriginalWeight)
        }

        if (changedValues.adjustedLength !== undefined) {
            setAdjustedLength(changedValues.adjustedLength);
        }

        if (changedValues.codAmount !== undefined) {
            setCodAmount(changedValues.codAmount);
        }

        if (changedValues.serviceType !== undefined) {
            const selected = serviceTypes?.find(s => s.id === changedValues.serviceType);
            setSelectedServiceType(selected || null);
        }
    };

    // Fetch bưu cục ở địa phương khi người dùng chọn giao tại bưu cục
    const fetchLocalOffices = async () => {
        if (senderData.cityCode === 0) return;

        setLoadingOffice(true);
        try {
            const param = {
                city: senderData.cityCode,
                ward: senderData.wardCode,
            }
            const result = await officeApi.listLocalOffices(param);
            if (result.success) {
                const list = result.data || [];
                setLocalOffices(list);
            } else {
                message.error(result.message || "Không thể tải danh sách bưu cục")
            }
        } catch (error: any) {
            message.error(error.message || "Không thể tải danh sách bưu cục");
        } finally {
            setLoadingOffice(false);
        }
    }

    useEffect(() => {
        if (!(pickupType === "AT_OFFICE")) return;
        fetchLocalOffices()
    }, [senderData.cityCode]);

    useEffect(() => {
        if (!selectedOffice) return;

        const stillExists = localOffices.some(o => o.id === selectedOffice.id);

        if (!stillExists) {
            setSelectedOffice(null);
            form.setFieldsValue({ senderOfficeId: undefined });
        }
    }, [localOffices]);

    useEffect(() => {
        const hasLocalOffices = async () => {
            if (recipientData.cityCode === 0) return;

            setLoadingOffice(true);
            try {
                const result = await officeApi.hasLocalOffice(recipientData.cityCode);
                if (result.success) {
                    if (result.data === false) {
                        setIsHasOfficerRecipient(false);
                        message.error("Rất tiếc, chúng tôi chưa phục vụ khu vực người nhận. Vui lòng chọn một thành phố khác");
                    } else {
                        setIsHasOfficerRecipient(true);
                    }
                } else {
                    message.error(result.message || "Không thể kiểm tra có bưu cục trong khu vực đã chọn")
                }
            } catch (error) {
                console.log("Lỗi tải kiểm tra có bưu cục trong khu vực đã chọn", error);
                message.error("Không thể kiểm tra có bưu cục trong khu vực đã chọn");
            } finally {
                setLoadingOffice(false);
            }
        }

        hasLocalOffices();

    }, [recipientData.cityCode, recipientData.wardCode]);

    const handleEdit = async () => {
        if (!order) return;
        try {
            // Validate tất cả form
            await Promise.all([
                senderInfo.validateFields(),
                recipientInfo.validateFields(),
                paymentCard.validateFields(),
                fromOffice.validateFields(),
                orderInfo.validateFields(),
            ]);

            setLoadingOrder(true);

            if (!isHasOfficeRecipient) {
                message.error("Rất tiếc, địa chỉ người nhận hiện nằm ngoài khu vực phục vụ của chúng tôi. Vui lòng chọn khu vực khác.");
                return;
            }

            const orderData = {
                senderName: senderData.name,
                senderPhone: senderData.phoneNumber,
                senderCityCode: senderData.cityCode,
                senderCityName: senderData.cityName,
                senderDetail: senderData.detail,
                senderWardCode: senderData.wardCode,
                senderWardName: senderData.wardName,
                senderLatitude: senderData.latitude,
                senderLongitude: senderData.longitude,
                recipientName: recipientData.name,
                recipientPhone: recipientData.phoneNumber,
                recipientCityCode: recipientData.cityCode,
                recipientCityName: recipientData.cityName,
                recipientDetail: recipientData.detail,
                recipientWardCode: recipientData.wardCode,
                recipientWardName: recipientData.wardName,
                recipientLatitude: recipientData.latitude,
                recipientLongitude: recipientData.longitude,
                pickupType: pickupType,
                weight: adjustedWeight ?? weight,
                originalWeight: adjustedOriginalWeight ?? originalWeight,
                length: adjustedLength ?? length,
                width: adjustedWidth ?? width,
                height: adjustedHeight ?? height,
                serviceTypeId: selectedServiceType?.id,
                cod: codAmount || 0,
                orderValue: orderValue || 0,
                payer: payer,
                notes: notes || "",
                fromOfficeId: selectedOffice?.id,
            } as ManagerOrderRequest;

            console.log("orderData", orderData);

            const result = await orderApi.updateManagerOrder(order.id, orderData);
            if (result.success) {
                message.success(result.message || "Chỉnh sửa đơn hàng thành công")
                if (result.data) {
                    navigate(-1);
                }
            } else {
                message.error(result.message || "Chỉnh sửa đơn hàng thất bại")
            }
        } catch (error: any) {
            message.error(error.message || "Chỉnh sửa đơn hàng thất bại");
        } finally {
            setLoadingOrder(false);
        }
    };

    useEffect(() => {
        // chọn weight dùng để tính phí
        const effectiveWeight = adjustedWeight != null ? order?.adjustedWeight : weight;

        const fetchShippingFee = async () => {
            const hasEnoughData =
                senderData.cityCode &&
                recipientData.cityCode &&
                effectiveWeight &&
                selectedServiceType &&
                codAmount !== undefined &&
                orderValue !== undefined;

            if (!hasEnoughData) {
                setShippingFee(0);
                setServiceFee(0);
                return;
            }

            try {
                const [result1, result2] = await Promise.all([
                    shippingFeeApi.calculateShippingFee({
                        weight: effectiveWeight,
                        serviceTypeId: selectedServiceType.id,
                        senderCodeCity: senderData.cityCode,
                        recipientCodeCity: recipientData.cityCode,
                    }),
                    shippingFeeApi.calculateTotalFeeUser({
                        weight: effectiveWeight,
                        serviceTypeId: selectedServiceType.id,
                        senderCodeCity: senderData.cityCode,
                        recipientCodeCity: recipientData.cityCode,
                        cod: codAmount,
                        orderValue: orderValue,
                    }),
                ]);

                if (result1?.data) setShippingFee(result1.data);
                if (result2?.data) setServiceFee(result2.data);
            } catch (error) {
                console.error(error);
                message.error("Tính cước thất bại");
                setShippingFee(0);
                setServiceFee(0);
            }
        };

        fetchShippingFee();
    }, [
        senderData.cityCode,
        recipientData.cityCode,
        weight,
        adjustedWeight,
        selectedServiceType,
        codAmount,
        orderValue,
    ]);


    useEffect(() => {
        if (serviceFee === undefined || discountAmount === undefined) return;
        setTotalFee(Math.max(serviceFee - discountAmount, 0))
    }, [discountAmount, serviceFee]);

    const handleCancelOrder = () => {
        navigate(-1);
    };

    const handleOpenCancelOrder = () => {
        setModalConfirmOpen(true);
    }

    const handleCalculateWeight = async (
        length: number,
        width: number,
        height: number,
        originalWeight: number
    ) => {
        try {
            const result = await shippingFeeApi.calculateWeight({
                length,
                width,
                height,
                originalWeight,
            });

            if (result.success && result.data !== undefined) {
                const calculated = result.data;
                setAdjustedWeight(calculated || undefined);
                orderInfo.setFieldValue("adjustedWeight", calculated);
            } else {
                message.error(result.message || "Không thể tính khối lượng");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi tính khối lượng");
        }
    };

    useEffect(() => {
        const effectiveOriginalWeight = adjustedOriginalWeight ?? originalWeight;
        const effectiveHeight = adjustedHeight ?? height;
        const effectiveLength = adjustedLength ?? length;
        const effectiveWidth = adjustedWidth ?? width;

        if (effectiveOriginalWeight && effectiveWidth && effectiveLength && effectiveHeight) {
            handleCalculateWeight(effectiveLength, effectiveWidth, effectiveHeight, effectiveOriginalWeight);
        }
    }, [adjustedOriginalWeight, adjustedHeight, adjustedLength, adjustedWidth, originalWeight, length, width, height]);

    const orderColumns = [
        {
            title: "Mã SP",
            dataIndex: "productCode",
            key: "productCode",
            align: "left",
        },
        {
            title: "Tên SP",
            dataIndex: "productName",
            key: "productName",
            align: "left",
        },
        {
            title: "Khối lượng (Kg)",
            dataIndex: "productWeight",
            key: "productWeight",
            align: "center",
            render: (weight: number) => weight ? `${weight.toLocaleString()}kg` : '0kg',
        },
        {
            title: "Giá (VNĐ)",
            dataIndex: "productPrice",
            key: "productPrice",
            align: "center",
            render: (price: number) => `${price?.toLocaleString() || '0'}₫`,
        },
        {
            title: "Số lượng",
            dataIndex: "quantity",
            key: "quantity",
            align: "center",
            render: (quantity: number) => `${quantity?.toLocaleString() || '0'}`,
        },
    ];

    if (loadingOrderView || !order) {
        return <div>Đang tải đơn hàng...</div>;
    }


    return (
        <div className="create-order-container-edit">
            <Row gutter={24} justify="center">
                {/* LEFT CONTENT */}
                <Col xs={24} lg={18} className="create-order-left-content">
                    <div className="create-order-scrollable-content">
                        <div
                            className="create-order-main">
                            <Header
                                trackingNumber={order?.trackingNumber}
                                createdByType={order?.createdByType}
                            />

                            <SenderInfo
                                form={senderInfo}
                                sender={senderData}
                                status={order.status as OrderStatus}
                                creator={order.createdByType as OrderCreatorType}
                                onChange={(values) => {
                                    console.log("SenderInfo onChange values:", values);
                                    setSenderData(prev => {
                                        const next = {
                                            ...prev,
                                            name: values.name ?? prev.name,
                                            phoneNumber: values.phoneNumber ?? prev.phoneNumber,
                                            detail: values.sender?.detail ?? prev.detail,
                                            wardCode: values.sender?.wardCode ?? prev.wardCode,
                                            cityCode: values.sender?.cityCode ?? prev.cityCode,
                                            cityName: values.sender?.cityName ?? prev.cityName,
                                            wardName: values.sender?.wardName ?? prev.wardName,
                                            latitude: values.sender?.latitude ?? prev.latitude,
                                            longitude: values.sender?.longitude ?? prev.longitude,
                                        };
                                        if (JSON.stringify(next) === JSON.stringify(prev)) return prev;
                                        return next;
                                    });
                                }}
                            />

                            <RecipientInfo
                                form={recipientInfo}
                                recipient={recipientData}
                                status={order.status as OrderStatus}
                                creator={order.createdByType as OrderCreatorType}
                                onChange={(values) => {
                                    setRecipientData(prev => {
                                        const next = {
                                            ...prev,
                                            name: values.name ?? prev.name,
                                            phoneNumber: values.phoneNumber ?? prev.phoneNumber,
                                            detail: values.recipient?.detail ?? prev.detail,
                                            wardCode: values.recipient?.wardCode ?? prev.wardCode,
                                            cityCode: values.recipient?.cityCode ?? prev.cityCode,
                                            cityName: values.recipient?.cityName ?? prev.cityName,
                                            wardName: values.recipient?.wardName ?? prev.wardName,
                                            latitude: values.recipient?.latitude ?? prev.latitude,
                                            longitude: values.recipient?.longitude ?? prev.longitude,
                                        };
                                        if (JSON.stringify(next) === JSON.stringify(prev)) return prev;
                                        return next;
                                    });
                                }}
                            />

                            <OrderInfo
                                form={orderInfo}
                                codAmount={codAmount}
                                weight={weight}
                                originalWeight={originalWeight}
                                length={length}
                                width={width}
                                height={height}
                                adjustedWeight={adjustedWeight ?? undefined}
                                adjustedLength={adjustedLength ?? undefined}
                                adjustedWidth={adjustedWidth ?? undefined}
                                adjustedHeight={adjustedHeight ?? undefined}
                                adjustedOriginalWeight={adjustedOriginalWeight ?? undefined}
                                orderValue={orderValue}
                                orderProducts={orderProducts}
                                orderColumns={orderColumns}
                                serviceTypes={serviceTypes}
                                loading={loadingService}
                                status={order.status as OrderStatus}
                                creator={order.createdByType as OrderCreatorType}
                                setSelectedServiceType={(service) => {
                                    setSelectedServiceType(service);
                                }}
                                onChangeOrderInfo={handleOrderInfoChange}
                                selectedServiceType={selectedServiceType}
                            />

                            {order.createdByType === "USER" && (
                                <PickupType
                                    form={fromOffice}
                                    selectedOffice={selectedOffice}
                                    offices={localOffices}
                                    status={order.status as OrderStatus}
                                    creator={order.createdByType as OrderCreatorType}
                                    onLoadOffices={() => {
                                        fetchLocalOffices();
                                    }}
                                    onChange={({ office, pickupType }) => {
                                        setPickupType(pickupType);

                                        if (pickupType === "AT_OFFICE") {
                                            setSelectedOffice(office);
                                        } else {
                                            setSelectedOffice(null);
                                        }
                                    }}
                                    loading={loadingOffice}
                                    initialPickupType={pickupType}
                                />
                            )}

                            <PaymentCard
                                form={paymentCard}
                                payer={payer}
                                status={order.status as OrderStatus}
                                creator={order.createdByType as OrderCreatorType}
                                onChangePayment={(changedValues) => {
                                    setPayer(changedValues.payer);
                                }}
                            />

                            <NoteCard
                                notes={notes}
                                status={order.status as OrderStatus}
                                creator={order.createdByType as OrderCreatorType}
                                onChange={(newNotes) => {
                                    setNotes(newNotes);
                                }}
                            />
                        </div>
                    </div>
                </Col>

                <Col xs={24} lg={6} className="rightSidebar">
                    <div>
                        {discountAmount !== undefined && totalFee != undefined && serviceFee !== undefined &&
                            <PromotionCard
                                discountAmount={discountAmount}
                                totalFee={totalFee}
                                serviceFee={serviceFee}
                            />
                        }
                    </div>

                    <Actions
                        onEdit={handleEdit}
                        onCancel={handleOpenCancelOrder}
                        loading={loadingOrder}
                        status={order.status as OrderStatus}
                    />
                </Col>
            </Row>

            <ConfirmModal
                title='Xác nhận hủy chỉnh sửa'
                message='Bạn chắc chắn muốn bỏ các chỉnh sửa này không?'
                open={modalConfirmOpen}
                onOk={handleCancelOrder}
                onCancel={() => setModalConfirmOpen(false)}
                loading={loadingOrder}
            />
        </div>
    );
};

export default ManagerOrderEdit;