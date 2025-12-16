import React, { useEffect, useState } from "react";
import { Col, Form, message, Row } from "antd";
import Header from "./components/Header";
import Actions from "./components/Actions";
import RecipientInfo from "./components/RecipientInfo";
import NoteCard from "./components/NoteCard";
import PaymentCard from "./components/PaymentCard";
import OrderInfo from "./components/OrderInfo";
import SenderInfo from "./components/SenderInfo";
import PromotionCard from "./components/PromotionCard";
import type { ManagerOrderRequest } from "../../../../types/order";
import type { ServiceType } from "../../../../types/serviceType";
import "./ManagerOrderCreate.css"
import serviceTypeApi from "../../../../api/serviceTypeApi";
import shippingFeeApi from "../../../../api/shippingFeeApi";
import officeApi from "../../../../api/officeApi";
import SuccessOrderModal from "./components/SuccessOrderModal";
import orderApi from "../../../../api/orderApi";

const ManagerOrderCreate: React.FC = () => {
    const [form] = Form.useForm();
    const [loadingOrder, setLoadingOrder] = useState(false);
    const [officeCityCode, setOfficeCityCode] = useState<number | undefined>(undefined);

    const [loadingOffice, setLoadingOffice] = useState(false);

    // Dịch vụ vận chuyển đang hoạt động
    const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([])
    const [selectedServiceType, setSelectedServiceType] = useState<ServiceType | null>(null);
    const [loadingService, setLoadingService] = useState(false);

    // Gía trị khác đơn hàng
    const [orderValue, setOrderValue] = useState<number | undefined>(undefined);
    const [weight, setWeight] = useState<number | undefined>(undefined);
    const [totalFee, setTotalFee] = useState<number | undefined>(undefined);
    const [payer, setPayer] = useState<string>("CUSTOMER");
    const [notes, setNotes] = useState("");
    const [_, setShippingFee] = useState<number | undefined>(undefined);

    // Người gửi và người nhận
    const [empty] = useState({
        name: "",
        phoneNumber: "",
        detail: "",
        wardCode: 0,
        cityCode: 0,
    });
    const [senderData, setSenderData] = useState(empty);
    const [recipientData, setRecipientData] = useState(empty);

    // Biến kiểm tra xem địa chỉ người nhận có tỉnh thành nằm trong khu vực hoạt động không
    const [isHasOfficeRecipient, setIsHasOfficerRecipient] = useState(true);

    // Đặt hàng thành công
    const [showSuccessModal, setShowSuccessModal] = useState(false);
    const [successTrackingNumber, setSuccessTrackingNumber] = useState("");

    // Form instances
    const [senderInfo] = Form.useForm();
    const [recipientInfo] = Form.useForm();
    const [paymentCard] = Form.useForm();
    const [orderInfo] = Form.useForm();

    // Dịch vụ
    useEffect(() => {
        const fetchServiceTypes = async () => {
            try {
                setLoadingService(true);
                const response = await serviceTypeApi.getActiveServiceTypes();
                if (response.success && response.data) {
                    setServiceTypes(response.data);
                }
            } catch (error) {
                console.error("Error fetching Service types:", error);
            } finally {
                setLoadingService(false);
            }
        };

        fetchServiceTypes();
    }, []);

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

    // Dịch vụ
    useEffect(() => {
        const fetchOfficeCityCode = async () => {
            try {
                setLoadingOffice(true);
                const response = await officeApi.getManagerOfficeCityCode();
                if (response.success && response.data) {
                    setOfficeCityCode(response.data);
                } else {
                    message.error(response.message || "Lỗi khi lấy địa chỉ của bưu cục")
                }
            } catch (error: any) {
                message.error(error.message || "Lỗi khi lấy địa chỉ của bưu cục");
                console.error("Lỗi:", error);
            } finally {
                setLoadingOffice(false);
            }
        };

        fetchOfficeCityCode();
    }, []);

    // Xử lý khi tạo đơn hàng thành công
    const handleOrderSuccess = () => {
        setShowSuccessModal(true);
    };

    const handleCreateOrder = async () => {
        try {
            // Validate tất cả form
            await Promise.all([
                senderInfo.validateFields(),
                recipientInfo.validateFields(),
                paymentCard.validateFields(),
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
                senderWardCode: senderData.wardCode,
                senderDetail: senderData.detail,
                recipientName: recipientData.name,
                recipientPhone: recipientData.phoneNumber,
                recipientCityCode: recipientData.cityCode,
                recipientWardCode: recipientData.wardCode,
                recipientDetail: recipientData.detail,
                weight,
                serviceTypeId: selectedServiceType?.id,
                orderValue: orderValue || 0,
                payer: payer,
                notes: notes || "",
            } as ManagerOrderRequest;

            console.log("orderData", orderData);

            const result = await orderApi.createManagerOrder(orderData);
            if (result.success) {
                if (result.data) {
                    setSuccessTrackingNumber(result.data);
                    handleOrderSuccess();
                }
            } else {
                message.error(result.message || "Tạo đơn hàng thất bại")
            }
        } catch (error) {
            console.log("Lỗi tạo đơn hàng", error);
            message.error("Tạo đơn hàng thất bại");
        } finally {
            setLoadingOrder(false);
        }
    };

    useEffect(() => {
        const fetchShippingFee = async () => {
            const hasEnoughData = officeCityCode && recipientData.cityCode &&
                weight && selectedServiceType && orderValue !== undefined;

            if (!hasEnoughData) {
                setShippingFee(0);
                return;
            }

            try {
                const [result1, result2] = await Promise.all([
                    shippingFeeApi.calculateShippingFee({
                        weight,
                        serviceTypeId: selectedServiceType.id,
                        senderCodeCity: officeCityCode,
                        recipientCodeCity: recipientData.cityCode,
                    }),
                    shippingFeeApi.calculateTotalFeeMananager({
                        weight,
                        serviceTypeId: selectedServiceType.id,
                        senderCodeCity: officeCityCode,
                        recipientCodeCity: recipientData.cityCode,
                        cod: 0,
                        orderValue: orderValue
                    })
                ]);

                if (result1?.data) setShippingFee(result1.data);
                if (result2?.data) setTotalFee(result2.data);

            } catch (error) {
                console.error(error);
                message.error("Tính cước thất bại");
                setShippingFee(0);
                setTotalFee(0);
            }
        };

        fetchShippingFee();
    }, [officeCityCode, recipientData.cityCode, weight, selectedServiceType, orderValue]);

    // Tạo đơn mới sau khi tạo thành công 
    const handleResetForm = async () => {
        // Reset tất cả form
        senderInfo.resetFields();
        recipientInfo.resetFields();
        paymentCard.resetFields();
        orderInfo.resetFields();

        setOrderValue(undefined);
        setWeight(undefined);
        setTotalFee(undefined);
        setPayer("CUSTOMER");
        setNotes("");
        setShippingFee(undefined);
        setSenderData(empty);
        setRecipientData(empty);

        recipientInfo.setFieldsValue({
            name: "",
            phoneNumber: "",
            recipient: {
                cityCode: undefined,
                wardCode: undefined,
                detail: ""
            }
        });
    };

    return (
        <div className="create-order-container-edit">
            <Row gutter={24} justify="center">
                {/* LEFT CONTENT */}
                <Col xs={24} lg={18} className="create-order-left-content">
                    <div className="create-order-scrollable-content">
                        <div
                            className="create-order-main">
                            <Header />

                            <SenderInfo
                                form={senderInfo}
                                sender={senderData}
                                onChange={(values) => {
                                    setSenderData(prev => ({
                                        ...prev,
                                        name: values.name ?? prev.name,
                                        phoneNumber: values.phoneNumber ?? prev.phoneNumber,
                                        detail: values.sender?.detail ?? prev.detail,
                                        wardCode: values.sender?.wardCode ?? prev.wardCode,
                                        cityCode: values.sender?.cityCode ?? prev.cityCode,
                                    }));
                                }}
                            />

                            <RecipientInfo
                                form={recipientInfo}
                                recipient={recipientData}
                                onChange={(values) => {
                                    setRecipientData(prev => ({
                                        ...prev,
                                        name: values.name ?? prev.name,
                                        phoneNumber: values.phoneNumber ?? prev.phoneNumber,
                                        detail: values.recipient?.detail ?? prev.detail,
                                        wardCode: values.recipient?.wardCode ?? prev.wardCode,
                                        cityCode: values.recipient?.cityCode ?? prev.cityCode,
                                    }));
                                }}
                            />

                            <OrderInfo
                                form={orderInfo}
                                serviceTypes={serviceTypes}
                                loading={loadingService}
                                setSelectedServiceType={(service) => {
                                    const s = serviceTypes?.find(s => s.id === service?.id) || null;
                                    setSelectedServiceType(s);
                                }}
                                onChangeOrderInfo={(values) => {
                                    if (values.weight !== undefined) setWeight(values.weight);
                                    if (values.orderValue !== undefined) setOrderValue(values.orderValue);
                                }}
                            />

                            <PaymentCard
                                form={paymentCard}
                                payer={payer}
                                onChangePayment={(changedValues) => {
                                    setPayer(changedValues.payer);
                                }}
                            />

                            <NoteCard
                                notes={notes}
                                onChange={(newNotes) => {
                                    setNotes(newNotes);
                                }}
                            />
                        </div>
                    </div>
                </Col>

                {/* RIGHT SIDEBAR */}
                <Col xs={24} lg={6} className="rightSidebar">
                    <div>
                        <PromotionCard
                            totalFee={totalFee ?? 0}
                        />
                    </div>

                    <Actions
                        onCreate={handleCreateOrder}
                        loading={loadingOrder}
                    />
                </Col>
            </Row>

            <SuccessOrderModal
                open={showSuccessModal}
                trackingNumber={successTrackingNumber}
                onClose={() => setShowSuccessModal(false)}
                onCreateNew={handleResetForm}
            />
        </div>
    );
};

export default ManagerOrderCreate;