import React, { useEffect, useState } from "react";
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
import PickupType from "./components/PickupType";
import SelectedPromoModal from "./components/SelectPromoModal";
import PromotionCard from "./components/PromotionCard";
import type { UserOrderRequest } from "../../../../types/order";
import type { Product } from "../../../../types/product";
import type { OrderProduct } from "../../../../types/orderProduct";
import type { Office } from "../../../../types/office";
import type { Promotion } from "../../../../types/promotion";
import type { ServiceType } from "../../../../types/serviceType";
import "./UserOrderCreate.css"
import serviceTypeApi from "../../../../api/serviceTypeApi";
import promotionApi from "../../../../api/promotionApi";
import type { Address, AddressRequest } from "../../../../types/address";
import addressApi from "../../../../api/addressApi";
import AddressModal from "../../../common/profile/components/userAddress/components/AddressModal";
import productApi from "../../../../api/productApi";
import shippingFeeApi from "../../../../api/shippingFeeApi";
import officeApi from "../../../../api/officeApi";
import SuccessOrderModal from "./components/SuccessOrderModal";
import orderApi from "../../../../api/orderApi";
import bankAccountApi from "../../../../api/bankAccountApi";

const UserOrderCreate: React.FC = () => {
    const [form] = Form.useForm();
    const [loadingOrder, setLoadingOrder] = useState(false);

    const [loadingOffice, setLoadingOffice] = useState(false);

    // Thông tin cửa hàng
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [selectedAddress, setSelectedAddress] = useState<Address | null>(null);
    const [editingAddress, setEditingAddress] = useState<AddressRequest | null>(null);
    const [modalModeAddress, setModalModeAddress] = useState<'create' | 'edit'>('create');
    const [modalKeyAddress, setModalKeyAddress] = useState(0);
    const [modalAddEditAddress, setModalAddEditAddress] = useState(false);
    const [loadingAddress, setLoadingAddress] = useState(false);

    // Dịch vụ vận chuyển đang hoạt động
    const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([])
    const [selectedServiceType, setSelectedServiceType] = useState<ServiceType | null>(null);
    const [loadingService, setLoadingService] = useState(false);

    // Khuyến mãi 
    const [selectedPromotion, setSelectedPromotion] = useState<Promotion | null>(null);
    const [showPromoModal, setShowPromoModal] = useState<boolean>(false);
    const [promotions, setPromotions] = useState<Promotion[]>([]);
    const [promotionSearch, setPromotionSearch] = useState("");
    const [loadingPromotion, setLoadingPromotion] = useState(false);
    const [loadingBank, setLoadingBank] = useState(false);

    // Sản phẩm
    const [loadingProduct, setLoadingProduct] = useState(false);
    const [productSearch, setProductSearch] = useState("");
    const [showProductModal, setShowProductModal] = useState<boolean>(false);
    const [orderProducts, setOrderProducts] = useState<OrderProduct[]>([]);     // Các sản phẩm thuộc order đã chọn
    const [products, setProducts] = useState<Product[]>([]);    // Danh sách các sản phẩm đang còn hàng và ACTIVE
    const [productFilterType, setProductFilterType] = useState<string>("ALL");
    const [selectedProductIds, setSelectedProductIds] = useState<number[]>([]); // Danh sách id các sản phẩm đã chọn
    const [quantityValues, setQuantityValues] = useState<{ [key: number]: number }>({});    // Số lượng từng sản phẩm đã chọn

    // Các phân trang sản phẩm, chương trình khuyến mãi
    const [page, setPage] = useState<number>(1);
    const [total, setTotal] = useState<number>(0);
    const [limit, setLimit] = useState<number>(10);

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
    const [shippingFee, setShippingFee] = useState<number | undefined>(undefined);
    const [pickupType, setPickupType] = useState<string | undefined>(undefined);
    const [serviceFee, setServiceFee] = useState<number | undefined>(undefined);

    const [existBankAccount, setExistBankAccount] = useState<Boolean>(false);

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

    // Biến kiểm tra xem địa chỉ người gửi, người nhận có tỉnh thành nằm trong khu vực hoạt động không
    const [isHasOfficeSender, setIsHasOfficeSender] = useState(true);
    const [isHasOfficeRecipient, setIsHasOfficerRecipient] = useState(true);

    // Đặt hàng thành công
    const [showSuccessModal, setShowSuccessModal] = useState(false);
    const [successOrderId, setSuccessOrderId] = useState<number | null>(null);
    const [successTrackingNumber, setSuccessTrackingNumber] = useState("");
    const [successStatus, setSuccessStatus] = useState<"DRAFT" | "PENDING">("DRAFT");

    // Form instances
    const [senderInfo] = Form.useForm();
    const [recipientInfo] = Form.useForm();
    const [paymentCard] = Form.useForm();
    const [fromOffice] = Form.useForm();
    const [orderInfo] = Form.useForm();

    // Khuyến mãi
    const fetchPromotions = async () => {
        setLoadingPromotion(true);
        try {
            const param = {
                page: page,
                limit: limit,
                search: promotionSearch,
                serviceFee: serviceFee !== 0 ? serviceFee : undefined,
                weight: weight !== 0 ? weight : undefined,
                serviceTypeId: selectedServiceType !== null ? selectedServiceType.id : undefined,
            }
            const result = await promotionApi.getActiveUserPromotions(param);
            if (result.success) {
                const promoList = result.data?.list || [];
                setPromotions(promoList);
                setTotal(result.data?.pagination.total || 0);

                if (promoList.length > 0) {
                    setSelectedPromotion(prev =>
                        prev && promoList.find(p => p.id === prev.id) ? prev : null
                    );
                } else {
                    setSelectedPromotion(null);
                }
            } else {
                message.error(result.message || "Không thể tải thông tin khuyến mãi")
            }
        } catch (error) {
            console.log("Lỗi tải thông tin khuyến mãi", error);
            message.error("Không thể tải thông tin khuyến mãi");
        } finally {
            setLoadingPromotion(false);
        }
    }

    useEffect(() => {
        const allSet =
            serviceFee !== undefined &&
            weight !== undefined &&
            selectedServiceType !== null;

        if (!allSet) {
            return;
        }

        fetchPromotions();
    }, [serviceFee, weight, selectedServiceType, promotionSearch]);


    const handlePromotionSearch = async (value: string) => {
        setPromotionSearch(value);
    }

    const handlePageChangePromotion = (newPage: number, newLimit?: number) => {
        setPage(newPage);
        setLimit(newLimit || limit);
        fetchPromotions();
    };

    const handleOpenPromoModal = () => {
        const allSet =
            serviceFee !== undefined &&
            weight !== undefined &&
            selectedServiceType !== null;

        if (!allSet) {
            message.error("Vui lòng nhập thông tin người nhận, thông tin đơn hàng trước khi xem khuyến mãi");
            return;
        }

        setShowPromoModal(true);
    };


    // Tính discount amount khi selectedPromotion thay đổi
    useEffect(() => {
        if (!selectedPromotion) {
            setDiscountAmount(0);
            return;
        }

        if (serviceFee === undefined) return;

        let discount = 0;

        if (selectedPromotion.discountType === 'FIXED') {
            discount = selectedPromotion.discountValue;
        } else if (selectedPromotion.discountType === 'PERCENTAGE') {
            discount = (serviceFee * selectedPromotion.discountValue) / 100;
        }

        if (selectedPromotion.maxDiscountAmount) {
            discount = Math.min(discount, selectedPromotion.maxDiscountAmount);
        }

        discount = Math.floor(discount);

        if (serviceFee < selectedPromotion.minOrderValue) {
            setSelectedPromotion(null);
            setDiscountAmount(0);
            return;
        }

        setDiscountAmount(discount);
    }, [selectedPromotion, serviceFee]);


    // Kiểm tra tài khoản nhận cod
    const existBankAccounts = async () => {
        try {
            setLoadingBank(true);
            const response = await bankAccountApi.existUserBankAccounts();
            if (response.success && response.data) {
                setExistBankAccount(response.data);
            }
        } catch (error) {
            console.error("Error fetching Addresses:", error);
            setExistBankAccount(false);
        } finally {
            setLoadingBank(false);
        }
    };

    useEffect(() => {
        existBankAccounts();
    }, []);

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

    // Địa chỉ
    const fetchAddresses = async () => {
        try {
            setLoadingAddress(true);
            const response = await addressApi.getUserAddresses();
            if (response.success && response.data) {
                setAddresses(response.data);
            }
        } catch (error) {
            console.error("Error fetching Addresses:", error);
        } finally {
            setLoadingAddress(false);
        }
    };

    useEffect(() => {
        fetchAddresses();
    }, []);

    // Lấy địa chỉ mặc định
    useEffect(() => {
        if (!selectedAddress && addresses && addresses.length > 0) {
            const defaultAddr = addresses.find(addr => addr.isDefault);
            setSelectedAddress(defaultAddr ?? null);
        }
    }, [addresses]);

    // Người gửi với địa chỉ mặc định
    useEffect(() => {
        if (addresses && addresses.length > 0) {
            setSenderData({
                name: selectedAddress?.name || '',
                phoneNumber: selectedAddress?.phoneNumber || '',
                detail: selectedAddress?.detail || '',
                cityCode: selectedAddress?.cityCode ?? 0,
                wardCode: selectedAddress?.wardCode ?? 0
            });
        }

        const hasLocalOffices = async () => {
            if (senderData.cityCode === 0) return;

            setLoadingOffice(true);
            try {
                const result = await officeApi.hasLocalOffice(senderData.cityCode);
                if (result.success) {
                    if (result.data === false) {
                        setIsHasOfficeSender(false);
                        message.warning("Rất tiếc, chúng tôi chưa phục vụ khu vực người gửi. Vui lòng chọn một thành phố khác");
                    } else {
                        setIsHasOfficeSender(true);
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

    }, [selectedAddress]);

    const showModal = (mode: 'create' | 'edit', address?: Address) => {
        setModalModeAddress(mode);

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
                cityCode: 0,
                isDefault: addresses.length === 0
            };
            setEditingAddress(emptyAddress);
            form.resetFields();
            form.setFieldsValue({
                ...emptyAddress,
                address: { cityCode: undefined, wardCode: undefined, detail: '' }
            });
        }

        setModalKeyAddress(prev => prev + 1);
        setModalAddEditAddress(true);
    };

    const handleCancel = () => {
        setModalAddEditAddress(false);
        setEditingAddress(null);
        form.resetFields();
        form.setFieldsValue({
            address: { cityCode: undefined, wardCode: undefined, detail: '' }
        });
    };

    const handleSaveAddress = async () => {
        try {
            setLoadingAddress(true);

            const values = await form.validateFields();

            const payload: AddressRequest = {
                name: values.name,
                phoneNumber: values.phoneNumber,
                cityCode: values.address.cityCode,
                wardCode: values.address.wardCode,
                detail: values.address.detail,
                isDefault: values.isDefault,
            };

            if (modalModeAddress === 'edit' && editingAddress?.id) {
                const response = await addressApi.updateUserAddress(editingAddress.id, payload);

                if (response.success && response.data) {
                    fetchAddresses();
                    message.success('Cập nhật địa chỉ thành công!');
                } else {
                    message.error(response.message || "Cập nhật địa chỉ thất bại");
                }
            } else {
                const response = await addressApi.createUserAddress(payload);

                if (response.success && response.data) {
                    fetchAddresses();
                    message.success('Thêm địa chỉ thành công!');
                } else {
                    message.error(response.message || "Thêm địa chỉ thất bại");
                }
            }

            handleCancel();
        } catch (error) {
            console.error("Error saving address:", error);
            message.error('Vui lòng kiểm tra lại thông tin!');
        } finally {
            setLoadingAddress(false);
        }
    };

    const handleDeleteAddress = async (addressId: number) => {
        try {
            setLoadingAddress(true);
            const response = await addressApi.deleteUserAddress(addressId);
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
            setLoadingAddress(false);
        }
    };

    const handleSetDefault = async (addressId: number) => {
        try {
            setLoadingAddress(true);
            const response = await addressApi.setDefaultUserAddress(addressId);
            if (response.success && response.data) {
                fetchAddresses();
                message.success("Đã đặt làm địa chỉ mặc định")
            } else {
                message.error(response.message || "Có lỗi khi đặt địa chỉ làm mặc định");
            }
        } catch (error) {
            message.error("Có lỗi khi đặt địa chỉ làm mặc định");
            console.error("Error Set Default Addresses:", error);
        } finally {
            setLoadingAddress(false);
        }
    };

    const handleAddressChange = (address: AddressRequest) => {
        setEditingAddress(address);
    };

    const handleSelectedAddress = (address: Address) => {
        setSenderData({
            name: address.name,
            phoneNumber: address.phoneNumber,
            detail: address.detail,
            wardCode: address.wardCode,
            cityCode: address.cityCode,
        });

        setSelectedAddress(address);
    }

    // Sản phẩm
    const fetchProducts = async () => {
        setLoadingProduct(true);
        try {
            const param = {
                page: page,
                limit: limit,
                search: productSearch,
                type: productFilterType !== "ALL" ? productFilterType : undefined,
            }
            const result = await productApi.getActiveAndInstockUserProducts(param);
            if (result.success) {
                const productList = result.data?.list || [];
                setProducts(productList);
                setTotal(result.data?.pagination.total || 0);
            } else {
                message.error(result.message || "Không thể tải danh sách sản phẩm")
            }
        } catch (error) {
            console.log("Lỗi tải danh sách sản phẩm", error);
            message.error("Không thể tải danh sách sản phẩm");
        } finally {
            setLoadingProduct(false);
        }
    }

    useEffect(() => {
        if (showProductModal) {
            setSelectedProductIds(orderProducts.map(op => op.productId));
            fetchProducts();
        } else {
            setPage(1);
        }
    }, [showProductModal]);

    useEffect(() => {
        fetchProducts();
    }, [productFilterType, productSearch]);

    const handlePageChangeProduct = (newPage: number, newLimit?: number) => {
        setPage(newPage);
        setLimit(newLimit || limit);
        fetchProducts();
    };

    const handleProductSearch = async (value: string) => {
        setProductSearch(value);
    }

    const handleProductFilterType = async (value: string) => {
        setProductFilterType(value);
    }

    const handleSelectProducts = (selected: Product[]) => {
        const newOrderProducts: OrderProduct[] = selected.map((p) => {
            const existing = orderProducts.find(op => op.productId === p.id);

            return {
                productId: p.id,
                productName: p.name,
                productCode: p.code,
                productPrice: p.price,
                productType: p.type,
                productStock: p.stock,
                productWeight: p.weight,
                quantity: existing ? existing.quantity : 1,
                price: p.price,
            };
        });

        setOrderProducts(newOrderProducts);
        console.log("newOrderProducts", newOrderProducts);
        setSelectedProductIds(selected.map(s => s.id));

        const totalValue = newOrderProducts.reduce(
            (sum, op) => sum + op.productPrice * op.quantity,
            0
        );

        const totalWeight = newOrderProducts.reduce(
            (sum, op) => sum + (op.productWeight || 0) * op.quantity,
            0
        );

        setOrderValue(totalValue);
        setWeight(totalWeight);
        setShowProductModal(false);
    };

    useEffect(() => {
        const initialQuantities: { [key: number]: number } = {};
        orderProducts.forEach((op, index) => {
            initialQuantities[index] = op.quantity;
        });
        setQuantityValues(initialQuantities);
    }, [orderProducts]);

    const handleQuantityChange = (value: number | null, index: number) => {
        if (value !== null && value > 0) {
            setQuantityValues(prev => ({
                ...prev,
                [index]: value
            }));

            const updated = orderProducts.map((op, i) =>
                i === index ? { ...op, quantity: value } : op
            );

            setOrderProducts(updated);

            const totalValue = updated.reduce(
                (sum, op) => sum + op.productPrice * op.quantity,
                0
            );
            const totalWeight = updated.reduce(
                (sum, op) => sum + (op.productWeight || 0) * op.quantity,
                0
            );
            setOrderValue(totalValue);
            setWeight(Number(totalWeight.toFixed(2)));
        }
    };

    const handleOrderInfoChange = (changedValues: any) => {
        if (changedValues.weight !== undefined) {
            setWeight(changedValues.weight);
        }

        if (changedValues.orderValue !== undefined) {
            setOrderValue(changedValues.orderValue);
        }

        if (changedValues.codAmount !== undefined) {
            setCodAmount(changedValues.codAmount);
        }

        if (changedValues.serviceType !== undefined) {
            const selected = serviceTypes?.find(s => s.id === changedValues.serviceType);
            setSelectedServiceType(selected || null);
        }
    };

    useEffect(() => {
        if (!(pickupType === "AT_OFFICE")) return;

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
            } catch (error) {
                console.log("Lỗi tải danh sách bưu cục", error);
                message.error("Không thể tải danh sách bưu cục");
                setLocalOffices([]);
            } finally {
                setLoadingOffice(false);
            }
        }
        fetchLocalOffices()
    }, [senderData.cityCode, senderData.wardCode, pickupType]);

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
                        message.warning("Rất tiếc, chúng tôi chưa phục vụ khu vực người nhận. Vui lòng chọn một thành phố khác");
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

    // Xử lý khi tạo đơn hàng thành công
    const handleOrderSuccess = () => {
        setShowSuccessModal(true);
    };

    const handleCreateOrder = async (status: "DRAFT" | "PENDING") => {
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

            // Kiểm tra khu vực phục vụ
            if (!isHasOfficeSender && !isHasOfficeRecipient) {
                message.warning("Rất tiếc, cả địa chỉ người gửi và người nhận đều nằm ngoài khu vực phục vụ. Vui lòng chọn khu vực khác.");
                return;
            }

            if (!isHasOfficeSender) {
                message.warning("Rất tiếc, địa chỉ người gửi hiện nằm ngoài khu vực phục vụ của chúng tôi. Vui lòng chọn khu vực khác.");
                return;
            }

            if (!isHasOfficeRecipient) {
                message.warning("Rất tiếc, địa chỉ người nhận hiện nằm ngoài khu vực phục vụ của chúng tôi. Vui lòng chọn khu vực khác.");
                return;
            }

            // Kiểm tra nếu có sản phẩm tươi sống => bắt buộc dùng dịch vụ hỏa tốc
            const hasFreshProduct = orderProducts.some(op => op.productType === "FRESH");
            if (hasFreshProduct && serviceTypes) {
                const fastService = serviceTypes.find(s => s.name.toLowerCase().includes("hỏa tốc"));
                if (!selectedServiceType || selectedServiceType.id !== fastService?.id) {
                    message.warning("Đơn hàng có sản phẩm tươi sống. Vui lòng chọn dịch vụ HỎA TỐC!");
                    setSelectedServiceType(fastService || null);
                    return;
                }
            }

            const orderData = {
                status,
                senderAddressId: selectedAddress?.id,
                recipientName: recipientData.name,
                recipientPhone: recipientData.phoneNumber,
                recipientCityCode: recipientData.cityCode,
                recipientWardCode: recipientData.wardCode,
                recipientDetail: recipientData.detail,
                pickupType: pickupType,
                weight,
                serviceTypeId: selectedServiceType?.id,
                cod: codAmount || 0,
                orderValue: orderValue || 0,
                payer: payer,
                notes: notes || "",
                promotionId: selectedPromotion?.id,
                fromOfficeId: selectedOffice?.id,
                discountAmount: discountAmount,
                shippingFee: shippingFee,
                orderProducts: orderProducts.map((op) => ({
                    productId: op.productId,
                    quantity: op.quantity,
                })),
            } as UserOrderRequest;

            const result = await orderApi.createUserOrder(orderData);
            if (result.success) {
                if (result.data) {
                    setSuccessTrackingNumber(result.data.trackingNumber);
                    setSuccessOrderId(result.data.orderId);
                    setSuccessStatus(orderData.status as "DRAFT" | "PENDING");
                    handleOrderSuccess();
                }
            } else {
                message.error(result.message || "Tạo đơn hàng thất bại")
            }
        } catch (error: any) {
            console.log("Lỗi tạo đơn hàng", error);
            message.error(error.message || "Tạo đơn hàng thất bại");
        } finally {
            setLoadingOrder(false);
        }
    };

    useEffect(() => {
        const fetchShippingFee = async () => {
            const hasEnoughData = senderData.cityCode && recipientData.cityCode &&
                weight && selectedServiceType && codAmount !== undefined && orderValue !== undefined;

            if (!hasEnoughData) {
                setShippingFee(0);
                setServiceFee(0);
                return;
            }

            try {
                const [result1, result2] = await Promise.all([
                    shippingFeeApi.calculateShippingFee({
                        weight,
                        serviceTypeId: selectedServiceType.id,
                        senderCodeCity: senderData.cityCode,
                        recipientCodeCity: recipientData.cityCode,
                    }),
                    shippingFeeApi.calculateTotalFeeUser({
                        weight,
                        serviceTypeId: selectedServiceType.id,
                        senderCodeCity: senderData.cityCode,
                        recipientCodeCity: recipientData.cityCode,
                        cod: codAmount,
                        orderValue: orderValue
                    })
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
    }, [senderData.cityCode, recipientData.cityCode, weight, selectedServiceType, codAmount, orderValue]);

    useEffect(() => {
        if (serviceFee === undefined || discountAmount === undefined) return;
        setTotalFee(Math.max(serviceFee - discountAmount, 0))
    }, [discountAmount, serviceFee]);

    // Tạo đơn mới sau khi tạo thành công 
    const handleResetForm = async () => {
        // Reset tất cả form
        senderInfo.resetFields();
        recipientInfo.resetFields();
        paymentCard.resetFields();
        fromOffice.resetFields();
        orderInfo.resetFields();

        // Reset state
        setOrderProducts([]);
        setSelectedProductIds([]);
        setQuantityValues({});
        setOrderValue(undefined);
        setWeight(undefined);
        setCodAmount(undefined);
        setTotalFee(undefined);
        setPayer("CUSTOMER");
        setDiscountAmount(undefined);
        setNotes("");
        setShippingFee(undefined);
        setPickupType(undefined);
        setSelectedOffice(null);
        setSelectedPromotion(null);

        setSelectedAddress(null);
        setSenderData(empty);
        setRecipientData(empty);

        await fetchAddresses();

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

    const orderColumns = [
        {
            title: "Mã SP",
            dataIndex: "productCode",
            key: "productCode",
            align: "center",
        },
        {
            title: "Tên SP",
            dataIndex: "productName",
            key: "productName",
            align: "left",
        },
        {
            title: "Khối lượng quy đổi (Kg)",
            dataIndex: "productWeight",
            key: "productWeight",
            align: "center",
            render: (weight: number) => weight !== undefined && weight !== null
                ? Number(weight).toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
                : '0.00',
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
            render: (value: number, record: OrderProduct, index: number) => {
                const currentValue = quantityValues[index] || value;

                const handleChange = (newValue: number | null) => {
                    const numValue = newValue || 0;

                    if (numValue > record.productStock) {
                        message.error(`Số lượng vượt quá tồn kho! Tồn kho hiện tại: ${record.productStock}`);
                        return;
                    }

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

                                    if (numValue > record.productStock) {
                                        return Promise.reject(
                                            new Error(`Số lượng tối đa hiện tại: ${record.productStock}`)
                                        );
                                    }

                                    return Promise.resolve();
                                },
                            },
                        ]}
                        initialValue={currentValue}
                        validateTrigger={['onChange', 'onBlur']}
                    >
                        <InputNumber
                            className="modal-custom-input-number fixed-width"
                            min={1}
                            max={record.productStock}
                            value={currentValue}
                            onChange={handleChange}
                            onBlur={(e) => {
                                const value = e.target.value;
                                if (value && Number(value) > record.productStock) {
                                    message.warning(`Số lượng không được vượt quá ${record.productStock}`);
                                }
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
            dataIndex: "productStock",
            key: "productStock",
            align: "center",
        },
        {
            key: "action",
            align: "center",
            render: (_: any, record: OrderProduct) => (
                <Tooltip title="Xoá sản phẩm">
                    <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => {
                            setOrderProducts((prev) => {
                                const updated = prev.filter(
                                    (op) => op.productId !== record.productId
                                );

                                setSelectedProductIds((prevIds) =>
                                    prevIds.filter(id => id !== record.productId)
                                );

                                const totalValue = updated.reduce(
                                    (sum, op) => sum + op.productPrice * op.quantity,
                                    0
                                );
                                const totalWeight = updated.reduce(
                                    (sum, op) => sum + (op.productWeight || 0) * op.quantity,
                                    0
                                );
                                setOrderValue(totalValue);
                                setWeight(totalWeight);

                                return updated;
                            });
                        }}
                    />
                </Tooltip>
            ),
        },
    ];

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
                                sender={senderData}
                                addresses={addresses}
                                existBankAccount={existBankAccount}
                                initialSelected={selectedAddress}
                                onSelectAddress={handleSelectedAddress}
                                onAdd={() => showModal("create")}
                                onEdit={(a) => showModal("edit", a)}
                                onDelete={(id) => handleDeleteAddress(id)}
                                onSetDefault={(id) => handleSetDefault(id)}
                            />

                            <RecipientInfo
                                form={recipientInfo}
                                recipient={recipientData}
                                disabled={!selectedAddress || !existBankAccount}
                                onChange={(values) => {
                                    if (selectedAddress === null) return;
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
                                weight={weight}
                                orderValue={orderValue}
                                orderProducts={orderProducts}
                                orderColumns={orderColumns}
                                serviceTypes={serviceTypes}
                                loading={loadingService}
                                disabled={!selectedAddress || !existBankAccount}
                                setSelectedServiceType={(service) => {
                                    if (selectedAddress === null) return;
                                    setSelectedServiceType(service);
                                }}
                                onOpenProductModal={() => {
                                    if (selectedAddress === null) return;
                                    setShowProductModal(true);
                                }}
                                onChangeOrderInfo={handleOrderInfoChange}
                            />

                            <PickupType
                                form={fromOffice}
                                selectedOffice={selectedOffice}
                                offices={localOffices}
                                disabled={!selectedAddress || !existBankAccount}
                                onChange={({ office, pickupType }) => {
                                    setPickupType(pickupType);

                                    if (pickupType === "AT_OFFICE") {
                                        setSelectedOffice(office);
                                    } else {
                                        setSelectedOffice(null);
                                    }
                                }}
                                loading={loadingOffice}
                            />

                            <PaymentCard
                                form={paymentCard}
                                payer={payer}
                                disabled={!selectedAddress || !existBankAccount}
                                onChangePayment={(changedValues) => {
                                    if (selectedAddress === null) return;
                                    setPayer(changedValues.payer);
                                }}
                            />

                            <NoteCard
                                notes={notes}
                                disabled={!selectedAddress || !existBankAccount}
                                onChange={(newNotes) => {
                                    if (selectedAddress === null) return;
                                    setNotes(newNotes);
                                }}
                            />
                        </div>
                    </div>
                </Col>

                {/* RIGHT SIDEBAR */}
                <Col xs={24} lg={6} className="rightSidebar">
                    <div>
                        {discountAmount !== undefined && totalFee != undefined && serviceFee !== undefined &&
                            <PromotionCard
                                discountAmount={discountAmount}
                                totalFee={totalFee}
                                serviceFee={serviceFee}
                                selectedPromotion={selectedPromotion}
                                setSelectedPromotion={setSelectedPromotion}
                                setShowPromoModal={handleOpenPromoModal}
                                disabled={!selectedAddress || !existBankAccount}
                            />
                        }
                    </div>

                    <Actions
                        onCreate={handleCreateOrder}
                        loading={loadingOrder}
                        disabled={!selectedAddress || !existBankAccount}
                    />
                </Col>
            </Row>

            <SelectProductModal
                open={showProductModal}
                products={products}
                page={page}
                limit={limit}
                total={total}
                selectedProductIds={selectedProductIds}
                setSelectedProductIds={setSelectedProductIds}
                loading={loadingProduct}
                onClose={() => setShowProductModal(false)}
                onSearch={handleProductSearch}
                filterType={productFilterType}
                onFilterTypeChange={handleProductFilterType}
                onSelectProducts={handleSelectProducts}
                onPageChange={handlePageChangeProduct}
            />

            {serviceFee !== undefined &&
                <SelectedPromoModal
                    open={showPromoModal}
                    onCancel={() => setShowPromoModal(false)}
                    promotions={promotions}
                    selectedPromotion={selectedPromotion}
                    setSelectedPromotion={setSelectedPromotion}
                    onSearch={handlePromotionSearch}
                    pageSize={limit}
                    currentPage={page}
                    serviceFee={serviceFee}
                    total={total}
                    loading={loadingPromotion}
                    onPageChange={handlePageChangePromotion}
                />
            }

            <AddressModal
                key={modalKeyAddress}
                open={modalAddEditAddress}
                mode={modalModeAddress}
                address={editingAddress || {
                    name: '',
                    phoneNumber: '',
                    detail: '',
                    wardCode: 0,
                    cityCode: 0,
                    isDefault: addresses.length === 0
                }}
                onOk={handleSaveAddress}
                onCancel={handleCancel}
                onAddressChange={handleAddressChange}
                form={form}
                total={addresses.length}
                loading={loadingAddress}
            />

            <SuccessOrderModal
                open={showSuccessModal}
                trackingNumber={successTrackingNumber}
                orderId={successOrderId}
                status={successStatus}
                onClose={() => setShowSuccessModal(false)}
                onCreateNew={handleResetForm}
            />
        </div>
    );
};

export default UserOrderCreate;