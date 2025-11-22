import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { Button, Col, Form, InputNumber, message, Row, Tooltip } from "antd";
import Header from "./components/Header";
import Actions from "./components/Actions";
import RecipientInfo from "./components/RecipientInfo";
import NoteCard from "./components/NoteCard";
import PaymentCard from "./components/PaymentCard";
import OrderInfo from "./components/OrderInfo";
import SenderInfo from "./components/SenderInfo";
import SelectProductModal from "./components/SelectProductModal";
import { DeleteOutlined } from "@ant-design/icons";
import FromOffice from "./components/FromOffice";
import SelectedPromoModal from "./components/SelectPromoModal";
import PromotionCard from "./components/PromotionCard";
import type { Order } from "../../../../types/order";
import type { Product } from "../../../../types/product";
import type { OrderProduct } from "../../../../types/orderProduct";
import type { City, Ward } from "../../../../types/location";
import type { Office } from "../../../../types/office";
import type { Promotion } from "../../../../types/promotion";
import type { ServiceType } from "../../../../types/serviceType";
import "./UserOrderCreate.css"
import { getCurrentUser } from "../../../../utils/authUtils";
import serviceTypeApi from "../../../../api/serviceTypeApi";

const UserOrderCreate: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState<boolean>(false);

    const user = getCurrentUser();

    const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);

    const [provinces, setCitys] = useState<City[]>([]);
    const [wards, setWards] = useState<Ward[]>([]);

    const [showProductModal, setShowProductModal] = useState<boolean>(false);

    // Lấy products sản phẩm đang Active của shop
    // const { products, nextCursor } = useState<Product | null></Product>(null);

    // Quản lý searchText
    const [searchText, setSearchText] = useState("");

    // Các sản phẩm thuộc order đã chọn
    const [orderProducts, setOrderProducts] = useState<OrderProduct[]>([]);

    const [selectedProductIds, setSelectedProductIds] = useState<number[]>([]);

    const [selectedServiceType, setSelectedServiceType] = useState<ServiceType | null>(null);

    const [orderValue, setOrderValue] = useState(0);
    const [weight, setWeight] = useState(0);
    const [codAmount, setCodAmount] = useState(0);
    const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);
    const [selectedPromo, setSelectedPromo] = useState<Promotion | null>(null);
    const [showPromoModal, setShowPromoModal] = useState<boolean>(false);
    // const { promotions, nextCursor: promotionNextCursor } = useSelector((state: RootState) => state.promotion);
    // const { shippingFee, loading: orderLoading, payers = [], paymentMethods = [] } =
    //     useSelector((state: RootState) => state.order);
    const [senderProvince, setSenderProvince] = useState<number | null>(null);
    const [recipientProvince, setRecipientProvince] = useState<number | null>(null);
    const [orderWeight, setOrderWeight] = useState(0);
    const [serviceTypeId, setServiceTypeId] = useState<number | null>(null);
    const [totalFee, setTotalFee] = useState(0);
    const [localOffices, setLocalOffices] = useState<Office[]>([]);
    const [discountAmount, setDiscountAmount] = useState(0);

    // Thêm state để quản lý số lượng
    const [quantityValues, setQuantityValues] = useState<{ [key: number]: number }>({});

    // Form instances
    const [senderInfo] = Form.useForm();
    const [recipientInfo] = Form.useForm();
    const [paymentCard] = Form.useForm();
    const [fromOffice] = Form.useForm();
    const [orderInfo] = Form.useForm();

    const [isSubmitting, setIsSubmitting] = useState(false);

    // Biến kiểm tra xem địa chỉ người gửi, người nhận có tỉnh thành nằm trong khu vực hoạt động không
    const [isHasOfficeSender, setIsHasOfficeSender] = useState(true);
    const [isHasOfficeRecipient, setIsHasOfficerRecipient] = useState(true);

    // State cho sender và recipient info
    const [senderData, setSenderData] = useState({
        name: "",
        phone: "",
        detailAddress: "",
        wardCode: 0,
        cityCode: 0,
    });

    const [recipientData, setRecipientData] = useState({
        name: "",
        phone: "",
        detailAddress: "",
        wardCode: 0,
        cityCode: 0,
    });

    const [paymentData, setPaymentData] = useState({
        payer: "Customer",
        paymentMethod: "Cash",
    });

    const [notes, setNotes] = useState("");

    // Lấy serviceType slice
    // const { serviceTypes, loading: serviceLoading } =
    //     useSelector((state: RootState) => state.serviceType);

    const handleOrderInfoChange = (changedValues: any) => {
        if (changedValues.weight !== undefined) {
            setWeight(changedValues.weight);
            setOrderWeight(changedValues.weight);
        }

        if (changedValues.orderValue !== undefined) {
            setOrderValue(changedValues.orderValue);
        }

        if (changedValues.codAmount !== undefined) {
            setCodAmount(changedValues.codAmount);
        }

        if (changedValues.serviceType !== undefined) {
            // const selected = serviceTypes?.find(s => s.id === changedValues.serviceType);
            // setSelectedServiceType(selected || null);
            // setServiceTypeId(selected?.id ?? null);
        }
    };

    const handleSearchPromo = (value: string) => {
        setSearchText(value);
        // dispatch(getActivePromotions({ limit: 10, searchText: value, shippingFee }));
    };

    const handleLoadMorePromotions = () => {
        // if (promotionNextCursor) {
        // dispatch(
        //     getActivePromotions({
        //         limit: 10,
        //         searchText,
        //         lastId: promotionNextCursor,
        //         shippingFee,
        //     })
        // );
        // }
    };

    // Hàm 1: Xử lý khi tạo đơn hàng thành công
    const handleOrderSuccess = async (
        order: Order,
        status: "draft" | "pending",
        orderData: any
    ) => {
        // Hiển thị thông báo
        message.success(status === "draft" ? "Lưu nháp thành công!" : "Tạo đơn hàng thành công!");
        setOrderProducts([]);

        // Kiểm tra ID
        if (!order.id) {
            message.error("Không tìm thấy ID đơn hàng");
            return;
        }

        // Điều hướng sang trang thành công
        navigate(`/user/orders/success/${order.trackingNumber}`, { replace: true });

        // Nếu thanh toán VNPay
        if (orderData.paymentMethod === "VNPay" && status === "pending") {
            // try {
            //     const res = await dispatch(createVNPayURL(order.id)).unwrap();
            //     if (res.paymentUrl) {
            //         window.location.href = res.paymentUrl;
            //     }
            // } catch (err: any) {
            //     console.error("Lỗi tạo URL thanh toán:", err);
            //     message.error(err.message || "Không thể tạo URL thanh toán VNPay");
            // }
        }
    };

    const handleCreateOrder = async (status: "draft" | "pending") => {
        try {
            // Validate tất cả form
            await Promise.all([
                senderInfo.validateFields(),
                recipientInfo.validateFields(),
                paymentCard.validateFields(),
                fromOffice.validateFields(),
                orderInfo.validateFields(),
            ]);

            setIsSubmitting(true);

            // Kiểm tra khu vực phục vụ
            if (!isHasOfficeSender && !isHasOfficeRecipient) {
                message.error(
                    "Rất tiếc, cả địa chỉ người gửi và người nhận đều nằm ngoài khu vực phục vụ. Vui lòng chọn khu vực khác."
                );
                return;
            }

            if (!isHasOfficeSender) {
                message.error(
                    "Rất tiếc, địa chỉ người gửi hiện nằm ngoài khu vực phục vụ của chúng tôi. Vui lòng chọn khu vực khác."
                );
                return;
            }

            if (!isHasOfficeRecipient) {
                message.error(
                    "Rất tiếc, địa chỉ người nhận hiện nằm ngoài khu vực phục vụ của chúng tôi. Vui lòng chọn khu vực khác."
                );
                return;
            }

            // Kiểm tra nếu có sản phẩm tươi sống => bắt buộc dùng dịch vụ hỏa tốc
            // const hasFreshProduct = orderProducts.some(op => op.product.type === "Fresh");
            // if (hasFreshProduct && serviceTypes) {
            //     const fastService = serviceTypes.find(s => s.name.toLowerCase().includes("hỏa tốc"));
            //     if (!selectedServiceType || selectedServiceType.id !== fastService?.id) {
            //         message.warning("Đơn hàng có sản phẩm tươi sống. Vui lòng chọn dịch vụ HỎA TỐC!");
            //         setSelectedServiceType(fastService || null);
            //         setServiceTypeId(fastService?.id ?? null);
            //         return; 
            //     }
            // }

            // Chuẩn bị dữ liệu đơn hàng
            // const orderData = {
            //     senderName: senderData.name || `${user?.lastName} ${user?.firstName}`,
            //     senderPhone: senderData.phone || user?.phoneNumber,
            //     senderCityCode: senderData.cityCode,
            //     senderWardCode: senderData.wardCode,
            //     senderDetailAddress: senderData.detailAddress,

            //     recipientName: recipientData.name,
            //     recipientPhone: recipientData.phone,
            //     recipientCityCode: recipientData.cityCode,
            //     recipientWardCode: recipientData.wardCode,
            //     recipientDetailAddress: recipientData.detailAddress,

            //     weight,
            //     serviceType: selectedServiceType || undefined,
            //     cod: codAmount || 0,
            //     orderValue: orderValue || 0,
            //     payer: paymentData.payer,
            //     paymentMethod: paymentData.paymentMethod,
            //     paymentStatus: "Unpaid",
            //     notes: notes || "",
            //     user: user || undefined,
            //     promotion: selectedPromo || undefined,
            //     discountAmount: discountAmount || 0,
            //     shippingFee: shippingFee || 0,
            //     status,
            //     orderProducts: orderProducts.map((op) => ({
            //         product: op.product,
            //         price: op.price,
            //         quantity: op.quantity,
            //     })),
            //     fromOffice: selectedOffice || undefined,
            // } as Order;

            // const response = await dispatch(createUserOrder(orderData)).unwrap();

            // if (response.success && response.order) {
            //     handleOrderSuccess(response.order, status, orderData);
            // } else {
            //     message.error(response.message || "Có lỗi xảy ra khi tạo đơn hàng");
            // }
        } catch (error: any) {
            console.error("Create error:", error);
            if (error.errorFields) {
                message.error("Vui lòng điền đầy đủ các trường bắt buộc");
            } else if (error.message) {
                message.error(error.message);
            } else {
                message.error("Có lỗi xảy ra khi tạo đơn hàng");
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleSelectProducts = (selected: Product[]) => {
        // const newOrderProducts: OrderProduct[] = selected.map((p) => {
        //     const existing = orderProducts.find(op => op.product.id === p.id);
        //     return {
        //         product: p,
        //         quantity: existing ? existing.quantity : 1,
        //         price: p.price,
        //     };
        // });

        // setOrderProducts(newOrderProducts);
        // setSelectedProductIds(selected.map(s => s.id));

        // const totalValue = newOrderProducts.reduce(
        //     (sum, op) => sum + op.product.price * op.quantity,
        //     0
        // );
        // const totalWeight = newOrderProducts.reduce(
        //     (sum, op) => sum + (op.product.weight || 0) * op.quantity,
        //     0
        // );

        // setOrderValue(totalValue);
        // setWeight(totalWeight);
        // setOrderWeight(totalWeight);
        // setShowProductModal(false);
    };

    const handleSearch = (value: string) => {
        setSearchText(value);
        // dispatch(listActiveUserProducts({ limit: 10, searchText: value }));
    };

    const handleLoadMoreProducts = () => {
        // if (nextCursor) {
        //     dispatch(
        //         listActiveUserProducts({
        //             limit: 10,
        //             lastId: nextCursor,
        //             searchText,
        //         })
        //     );
        // }
    };


    // const hasUserAddress =
    //     user &&
    //     user.detailAddress &&
    //     user.codeCity &&
    //     user.codeWard;

    useEffect(() => {
        const initialQuantities: { [key: number]: number } = {};
        // orderProducts.forEach((op, index) => {
        //     initialQuantities[index] = op.quantity;
        // });
        setQuantityValues(initialQuantities);
    }, [orderProducts]);

    const handleQuantityChange = (value: number | null, index: number) => {
        if (value !== null && value > 0) {
            // Cập nhật state số lượng
            setQuantityValues(prev => ({
                ...prev,
                [index]: value
            }));

            const updated = orderProducts.map((op, i) =>
                i === index ? { ...op, quantity: value } : op
            );

            setOrderProducts(updated);

            // const totalValue = updated.reduce(
            //     (sum, op) => sum + op.product.price * op.quantity,
            //     0
            // );
            // const totalWeight = updated.reduce(
            //     (sum, op) => sum + (op.product.weight || 0) * op.quantity,
            //     0
            // );
            // setOrderValue(totalValue);
            // setWeight(Number(totalWeight.toFixed(2)));
            // setOrderWeight(Number(totalWeight.toFixed(2)));
        }
    };

    // Theo dõi khi recipient.cityCode thay đổi
    useEffect(() => {
        if (recipientData.cityCode && recipientData.cityCode !== 0) {
            //     dispatch(getOfficesByArea({ codeCity: recipientData.cityCode }))
            //         .unwrap()
            //         .then((response) => {
            //             const fetchedOffices = response.offices || [];
            //             if (fetchedOffices.length == 0) {
            //                 setIsHasOfficerRecipient(false);
            //                 message.error("Rất tiếc, chúng tôi chưa phục vụ khu vực này. Vui lòng chọn một thành phố khác")
            //             } else {
            //                 setIsHasOfficerRecipient(true);
            //             }
            //         })
            //         .catch((error) => {
            //             setLocalOffices([]);
            //             setIsHasOfficerRecipient(false);
            //         });
        }
    }, [recipientData.cityCode]);

    useEffect(() => {
        // if (showProductModal) {
        //     setSelectedProductIds(orderProducts.map(op => op.product.id));
        //     dispatch(listActiveUserProducts({ limit: 10, searchText: "" }));
        // }
    }, [showProductModal]);

    useEffect(() => {
        const fetchServiceTypes = async () => {
            try {
                setLoading(true);
                const response = await serviceTypeApi.getActiveServiceTypes();
                if (response.success && response.data) {
                    setServiceTypes(response.data);
                }
            } catch (error) {
                console.error("Error fetching Service types:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchServiceTypes();
    }, []);

    // Tính shipping fee khi có đủ thông tin
    useEffect(() => {
        if (senderProvince && recipientProvince && orderWeight && serviceTypeId) {
            // dispatch(calculateShippingFeeThunk({
            //     weight: orderWeight,
            //     serviceTypeId: serviceTypeId,
            //     senderCodeCity: senderProvince,
            //     recipientCodeCity: recipientProvince,
            // }))
            //     .unwrap()
            //     .then((response) => {
            //         const shippingFee = Math.floor(response.shippingFee);
            //         setTotalFee(Math.max(0, shippingFee - discountAmount));
            //     })
            //     .catch((error) => {
            //         console.error("Lỗi tính phí vận chuyển:", error);
            //         setTotalFee(0);
            //     });
        } else {
            setTotalFee(0);
        }
    }, [senderProvince, recipientProvince, orderWeight, serviceTypeId, discountAmount]);

    // Khi senderProvince thay đổi thì lấy office
    useEffect(() => {
        if (senderProvince && senderData.wardCode) {
            setSelectedOffice(null);
            setLocalOffices([]);

            //     dispatch(getOfficesByArea({ codeCity: senderProvince, codeWard: senderData.wardCode }))
            //         .unwrap()
            //         .then((response) => {
            //             const fetchedOffices = response?.offices ?? [];
            //             if (!fetchedOffices || fetchedOffices.length === 0) {
            //                 // Đảm bảo message luôn chạy
            //                 setLocalOffices([]);
            //                 setIsHasOfficeSender(false);
            //                 message.error(
            //                     "Rất tiếc, chúng tôi chưa phục vụ khu vực này. Vui lòng chọn một thành phố khác"
            //                 );
            //             } else {
            //                 setIsHasOfficeSender(true);
            //                 setLocalOffices(fetchedOffices);
            //             }
            //         })
            //         .catch((error) => {
            //             // Khi API lỗi thật sự
            //             setLocalOffices([]);
            //             message.error("Đã xảy ra lỗi khi tải thông tin văn phòng.");
            //         });
        }
    }, [senderProvince, senderData.wardCode]);

    // useEffect(() => {
    //     setSearchText("");
    //     dispatch(getActivePromotions({ limit: 10, searchText: "", shippingFee }));
    //     console.log("promotion", promotions);
    // }, [shippingFee]);

    // Tính discount amount khi selectedPromo thay đổi
    // useEffect(() => {
    //     if (!selectedPromo) {
    //         setDiscountAmount(0);
    //         return;
    //     }

    //     let discount = 0;

    //     if (selectedPromo.discountType === 'fixed') {
    //         discount = selectedPromo.discountValue;
    //     } else if (selectedPromo.discountType === 'percentage') {
    //         // discount = (shippingFee * selectedPromo.discountValue) / 100;
    //     }

    //     if (selectedPromo.maxDiscountAmount) {
    //         discount = Math.min(discount, selectedPromo.maxDiscountAmount);
    //     }

    //     discount = Math.floor(discount);

    //     // Nếu phí ship không đủ để áp promo thì bỏ promo
    //     if (shippingFee < selectedPromo.minOrderValue) {
    //         setSelectedPromo(null);
    //         setDiscountAmount(0);
    //         return;
    //     }

    //     setDiscountAmount(discount);
    // }, [selectedPromo, shippingFee]);

    // useEffect(() => {
    //     if (user) {
    //         setSenderData({
    //             name: `${user.lastName} ${user.firstName}`,
    //             phone: user.phoneNumber,
    //             detailAddress: user.detailAddress || "",
    //             wardCode: user.codeWard || 0,
    //             cityCode: user.codeCity || 0,
    //         });
    //     }
    // }, [user]);

    const orderColumns = [
        {
            title: "Tên sản phẩm",
            dataIndex: ["product", "name"],
            key: "name",
            align: "center",
        },
        {
            title: "Trọng lượng (Kg)",
            dataIndex: ["product", "weight"],
            key: "weight",
            align: "center",
            render: (weight: number) => weight ? `${weight.toLocaleString()}kg` : '0kg',
        },
        {
            title: "Giá (VNĐ)",
            dataIndex: ["product", "price"],
            key: "price",
            align: "center",
            render: (price: number) => `${price?.toLocaleString() || '0'}₫`,
        },
        {
            title: "Số lượng",
            dataIndex: "quantity",
            key: "quantity",
            align: "center",
            render: (value: number, record: OrderProduct, index: number) => {
                const currentValue = quantityValues[index] || value;

                const handleChange = (newValue: number | null) => {
                    const numValue = newValue || 0;

                    // Validate stock
                    // if (numValue > record.product.stock) {
                    //     message.error(`Số lượng vượt quá tồn kho! Tồn kho hiện tại: ${record.product.stock}`);
                    //     return;
                    // }

                    handleQuantityChange(numValue, index);
                };

                return (
                    <Form.Item
                        name={['orderProducts', index, 'quantity']}
                        rules={[
                            {
                                validator: (_, val) => {
                                    if (val === undefined || val === null || val === '') {
                                        return Promise.reject(new Error("Vui lòng nhập số lượng"));
                                    }

                                    const numValue = Number(val);
                                    if (isNaN(numValue)) {
                                        return Promise.reject(new Error("Số lượng phải là số"));
                                    }

                                    if (numValue <= 0) {
                                        return Promise.reject(new Error("Số lượng phải lớn hơn 0"));
                                    }

                                    // if (numValue > record.product.stock) {
                                    //     return Promise.reject(
                                    //         new Error(`Số lượng tối đa hiện tại: ${record.product.stock}`)
                                    //     );
                                    // }

                                    return Promise.resolve();
                                },
                            },
                        ]}
                        initialValue={currentValue}
                        validateTrigger={['onChange', 'onBlur']}
                    >
                        <InputNumber
                            min={1}
                            // max={record.product.stock}
                            value={currentValue}
                            onChange={handleChange}
                            onBlur={(e) => {
                                const value = e.target.value;
                                // if (value && Number(value) > record.product.stock) {
                                //     message.warning(`Số lượng không được vượt quá ${record.product.stock}`);
                                // }
                            }}
                            style={{
                                width: 100,
                                // borderColor: currentValue > record.product.stock ? '#ff4d4f' : undefined
                            }}
                            placeholder="Nhập số lượng"
                            formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                            parser={(value) => value?.replace(/\$\s?|(,*)/g, '') as any}
                        />
                    </Form.Item>
                );
            },
        },
        {
            title: "Tồn kho",
            dataIndex: ["product", "stock"],
            key: "stock",
            align: "center",
            render: (stock: number, record: OrderProduct) => (
                <div>
                    <span style={{
                        color: stock === 0 ? '#ff4d4f' :
                            stock < 10 ? '#faad14' : '#52c41a',
                        fontWeight: 'bold'
                    }}>
                        {stock.toLocaleString()}
                    </span>
                </div>
            ),
        },
        {
            title: "Hành động",
            key: "action",
            align: "center",
            render: (_: any, record: OrderProduct, index: number) => (
                <Tooltip title="Xoá sản phẩm">
                    <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => {
                            // setOrderProducts((prev) => {
                            // const updated = prev.filter(
                            //     (op) => op.product.id !== record.product.id
                            // );

                            // setSelectedProductIds((prevIds) =>
                            //     prevIds.filter(id => id !== record.product.id)
                            // );

                            // const totalValue = updated.reduce(
                            //     (sum, op) => sum + op.product.price * op.quantity,
                            //     0
                            // );
                            // const totalWeight = updated.reduce(
                            //     (sum, op) => sum + (op.product.weight || 0) * op.quantity,
                            //     0
                            // );
                            // setOrderValue(totalValue);
                            // setWeight(totalWeight);
                            // setOrderWeight(totalWeight);

                            // return updated;
                            // });
                        }}
                    />
                </Tooltip>
            ),
        },
    ];

    return (
        <div className="containerEdit">
            <Row gutter={24} justify="center">
                {/* LEFT CONTENT */}
                <Col xs={24} lg={18} className="leftContent">
                    <div className="scrollableContent">
                        <div style={{
                            width: "100%",
                            maxWidth: 900,
                            position: 'relative'
                        }}>
                            <Header />

                            <SenderInfo
                                form={senderInfo}
                                sender={senderData}
                                // user={user ? {
                                //     firstName: user.firstName,
                                //     lastName: user.lastName,
                                //     phoneNumber: user.phoneNumber,
                                //     detailAddress: user.detailAddress || "",
                                //     codeWard: user.codeWard || 0,
                                //     codeCity: user.codeCity || 0,
                                //     role: user.role,
                                // } : undefined}
                                cityList={provinces}
                                wardList={wards}
                                onChange={(newSender) => {
                                    setSenderData(newSender);
                                    if (newSender?.cityCode) {
                                        setSenderProvince(newSender.cityCode);
                                    }
                                }}
                            />

                            <RecipientInfo
                                form={recipientInfo}
                                recipient={recipientData}
                                disabled={false}
                                // disabled={!hasUserAddress}
                                onChange={(values) => {
                                    // if (!hasUserAddress) return;
                                    // setRecipientData({
                                    //     name: values.recipientName,
                                    //     phone: values.recipientPhone,
                                    //     detailAddress: values.recipient?.address || "",
                                    //     wardCode: values.recipient?.commune,
                                    //     cityCode: values.recipient?.province,
                                    // });
                                    // if (values.recipient?.province) {
                                    //     setRecipientProvince(values.recipient.province);
                                    // }
                                }}
                            />

                            <PaymentCard
                                form={paymentCard}
                                payer={paymentData.payer}
                                payers={[]}
                                // payers={payers}
                                paymentMethod={paymentData.paymentMethod}
                                paymentMethods={[]}
                                // paymentMethods={paymentMethods}
                                disabled={false}
                                // disabled={!hasUserAddress}
                                onChangePayment={(changedValues) => {
                                    // if (!hasUserAddress) return;
                                    // setPaymentData(prev => ({ ...prev, ...changedValues }));
                                }}
                            />

                            <OrderInfo
                                form={orderInfo}
                                weight={weight}
                                orderValue={orderValue}
                                cod={codAmount}
                                orderProducts={orderProducts}
                                orderColumns={orderColumns}
                                serviceTypes={serviceTypes}
                                // serviceLoading={serviceLoading}
                                serviceLoading={false}
                                selectedServiceType={selectedServiceType}
                                disabled={false}
                                // disabled={!hasUserAddress}
                                setSelectedServiceType={(service) => {
                                    // if (!hasUserAddress) return;
                                    // setSelectedServiceType(service);
                                    // setServiceTypeId(service?.id ?? null);
                                }}
                                onOpenProductModal={() => {
                                    // if (!hasUserAddress) return;
                                    setShowProductModal(true);
                                }}
                                onChangeOrderInfo={handleOrderInfoChange}
                            />

                            <FromOffice
                                form={fromOffice}
                                selectedOffice={selectedOffice}
                                offices={localOffices}
                                disabled={false}
                                // disabled={!hasUserAddress}
                                onChange={(office) => {
                                    // if (!hasUserAddress) return;
                                    setSelectedOffice(office);
                                }}
                                wards={wards}
                                cities={provinces}
                            />

                            <NoteCard
                                notes={notes}
                                disabled={false}
                                // disabled={!hasUserAddress}
                                onChange={(newNotes) => {
                                    // if (!hasUserAddress) return;
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
                            shippingFee={0}
                            // shippingFee={shippingFee}
                            discountAmount={discountAmount}
                            totalFee={Math.ceil(
                                0
                                // Math.max(((shippingFee || 0) - (discountAmount || 0)), 0) * 1.1 +
                                // (orderValue ? orderValue * 0.005 : 0) +
                                // (codAmount ? codAmount * 0.02 : 0)
                            )}
                            cod={codAmount}
                            orderValue={orderValue}
                            selectedPromo={selectedPromo}
                            setSelectedPromo={setSelectedPromo}
                            setShowPromoModal={setShowPromoModal}
                            disabled={false}
                        />

                        {/* Modal */}
                        <SelectProductModal
                            open={showProductModal}
                            products={[]}
                            nextCursor={null}
                            // products={products}
                            // nextCursor={nextCursor ?? null}
                            selectedProductIds={selectedProductIds}
                            setSelectedProductIds={setSelectedProductIds}
                            loading={loading}
                            onClose={() => setShowProductModal(false)}
                            onSearch={handleSearch}
                            onSelectProducts={handleSelectProducts}
                            onLoadMore={handleLoadMoreProducts}
                        />

                        <SelectedPromoModal
                            open={showPromoModal}
                            onCancel={() => setShowPromoModal(false)}
                            promotions={[]}
                            // promotions={promotions}
                            selectedPromo={selectedPromo}
                            setSelectedPromo={setSelectedPromo}
                            onSearch={handleSearchPromo}
                            onLoadMore={handleLoadMorePromotions}
                            nextCursor={null}
                        // nextCursor={promotionNextCursor ?? null}
                        />
                    </div>

                    <Actions
                        onCreate={handleCreateOrder}
                        loading={isSubmitting}
                        // disabled={!hasUserAddress}
                        disabled={false}
                    />
                </Col>
            </Row>
        </div>
    );
};

export default UserOrderCreate;