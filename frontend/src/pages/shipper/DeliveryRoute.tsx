import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {
    Alert,
    Button,
    Card,
    Col,
    Divider,
    Flex,
    List,
    message,
    Modal,
    Progress,
    Row,
    Space,
    Spin,
    Statistic,
    Tag,
    Typography,
} from "antd";
import {
    CheckCircleOutlined,
    CompassOutlined,
    DollarOutlined,
    EnvironmentOutlined,
    ExclamationCircleFilled,
    EyeOutlined,
    NodeIndexOutlined,
    PauseCircleOutlined,
    PhoneOutlined,
    PlayCircleOutlined,
    ReloadOutlined,
} from "@ant-design/icons";
import {GoogleMap, MarkerF, PolylineF, useJsApiLoader} from "@react-google-maps/api";
import polyline from "@mapbox/polyline";
import {useNavigate} from "react-router-dom";
import orderApi from "../../api/orderApi";
import {SHIPPER_ROUTE_REFRESH_EVENT} from "./deliveryRouteEvents";

const { Title, Text } = Typography;

const MAP_CONTAINER_STYLE = {
    width: "100%",
    height: "650px",
};

const decodeEncodedPolyline = (encoded?: string): google.maps.LatLngLiteral[] => {
    if (!encoded) return [];
    try {
        return polyline.decode(encoded).map(([lat, lng]) => ({ lat, lng }));
    } catch {
        return [];
    }
};

const DEFAULT_CENTER = {
    lat: 10.9804,
    lng: 106.6519,
};

interface RouteInfo {
    id: number;
    name: string;
    startLocation: string;
    totalStops: number;
    completedStops: number;
    totalDistance: number;
    estimatedDuration: number;
    totalCOD: number;
    status: string;
    source?: string;
    shipmentId?: number;
    shipmentCode?: string;
    shipmentStatus?: string;
    encodedPolyline?: string;
    planCode?: string;
    fuelCost?: number;
    routeVersion?: number | null;
    reoptimizeReason?: string | null;
}

interface DeliveryStop {
    id: number;
    trackingNumber: string;
    recipientName: string;
    recipientPhone: string;
    recipientAddress: string;
    recipientFullAddress?: string;
    recipientLatitude?: number | null;
    recipientLongitude?: number | null;
    senderName?: string;
    senderPhone?: string;
    senderAddress?: string;
    senderFullAddress?: string;
    senderLatitude?: number | null;
    senderLongitude?: number | null;
    contactName?: string;
    contactPhone?: string;
    contactAddress?: string;
    codAmount: number;
    priority: string;
    serviceType: string;
    status: string;
    orderStatus?: string;
    stopSequence?: number;
    etaTime?: string;
    latitude?: number;
    longitude?: number;
    stopType?: string;
}


const isFinalDeliveryStatus = (status?: string) => {
    const st = (status || "").toString().toUpperCase();
    return st === "DELIVERED"
        || st === "FAILED_DELIVERY"
        || st === "COMPLETED"
        || st === "FAILED"
        // Backend có thể trả "final" cho đơn pickup fail final / delivery fail final / return fail final / cancelled / returned
        || st === "FINAL";
};

// Helper: quyết định 1 stop có nên ẨN khỏi lộ trình hay không.
// Dựa vào raw orderStatus (do backend set trong stop.orderStatus) thay vì mapped status
// vì backend map cả PICKED_UP lẫn DELIVERED đều = "completed" cho DELIVERY stop.
//   - PICKED_UP = đã lấy hàng -> vẫn hiển thị (shipper cần giao tiếp) - KHÔNG ẩn
//   - DELIVERED = đã giao -> ẩn khỏi route
//   - FAILED_DELIVERY / CANCELLED / RETURNED... = ẩn
//   - RETURNING / DELIVERING / PICKING_UP = đang xử lý -> hiển thị
const isStopHiddenFromRoute = (stop: DeliveryStop) => {
    const rawOrderStatus = (stop.orderStatus || "").toString().toUpperCase();

    // Trạng thái order "kết thúc vận chuyển" -> ẩn khỏi mọi route
    // Lưu ý: KHÔNG bao gồm PICKED_UP (đã lấy hàng nhưng vẫn phải giao).
    const TERMINAL = new Set([
        "DELIVERED",
        "FAILED_DELIVERY",
        "DELIVERY_FAILED_FINAL",
        "PICKUP_FAILED_FINAL",
        "RETURNED",
        "RETURN_FAILED_FINAL",
        "CANCELLED",
    ]);
    if (TERMINAL.has(rawOrderStatus)) return true;

    return false;
};

const isCompletedStop = (status?: string) => {
    const st = (status || "").toString().toUpperCase();
    return st === "COMPLETED" || st === "DELIVERED";
};

// Helper: lấy contact/address chính của stop theo stopType
const getStopContact = (stop: DeliveryStop) => {
    const isPickup = (stop.stopType || "").toUpperCase() === "PICKUP";
    if (isPickup) {
        return {
            name: stop.senderName || stop.recipientName || "",
            phone: stop.senderPhone || stop.recipientPhone || "",
            address: stop.senderAddress || stop.recipientAddress || "",
            latitude: stop.latitude ?? stop.senderLatitude ?? null,
            longitude: stop.longitude ?? stop.senderLongitude ?? null,
        };
    }
    return {
        name: stop.recipientName || stop.senderName || "",
        phone: stop.recipientPhone || stop.senderPhone || "",
        address: stop.recipientAddress || stop.senderAddress || "",
        latitude: stop.latitude ?? stop.recipientLatitude ?? null,
        longitude: stop.longitude ?? stop.recipientLongitude ?? null,
    };
};

const isDeliveryStop = (stop: DeliveryStop) => {
    const st = (stop.stopType || "").toString().toUpperCase();
    return st === "DELIVERY" || st === "PICKUP";
};

const sortByStopSequence = <T extends { stopSequence?: number }>(stops: T[]): T[] => {
    if (!stops.some((s) => s.stopSequence != null)) return stops;
    return [...stops].sort((a, b) => (a.stopSequence || 0) - (b.stopSequence || 0));
};

const hasValidCoords = (stop: DeliveryStop) =>
    stop.latitude != null &&
    stop.longitude != null &&
    !Number.isNaN(Number(stop.latitude)) &&
    !Number.isNaN(Number(stop.longitude));

const ShipperDeliveryRoute: React.FC = () => {
    const navigate = useNavigate();
    const [routeInfo, setRouteInfo] = useState<RouteInfo | null>(null);
    const [allStops, setAllStops] = useState<DeliveryStop[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedStop, setSelectedStop] = useState<DeliveryStop | null>(null);
    const [detailModal, setDetailModal] = useState(false);
    const [currentPosition, setCurrentPosition] = useState<google.maps.LatLngLiteral | null>(null);
    const [realtimeDirections, setRealtimeDirections] = useState<google.maps.DirectionsResult | null>(null);
    const [directionsLoading, setDirectionsLoading] = useState(false);
    const [directionsRenderKey, setDirectionsRenderKey] = useState(0);
    const [reOptimizing, setReOptimizing] = useState(false);

    const mapRef = useRef<google.maps.Map | null>(null);
    const mapCardRef = useRef<HTMLDivElement | null>(null);
    const lastNextStopIdRef = useRef<number | null>(null);
    const directionsRequestSeqRef = useRef(0);
    const lastDirectionsQueryRef = useRef<{ stopId: number; origin: google.maps.LatLngLiteral } | null>(null);
    const directionsCacheRef = useRef<Map<string, google.maps.DirectionsResult>>(new Map());

    // --- Google Maps loader (singleton, không re-load khi re-render) ---
    const { isLoaded, loadError } = useJsApiLoader({
        id: "google-maps-script",
        googleMapsApiKey: (import.meta.env.VITE_GOOGLE_MAPS_KEY as string) || "",
    });

    // deliveryStops = chỉ DELIVERY/PICKUP, không tính RETURN_TO_OFFICE
    const deliveryStops = useMemo(() => {
        return sortByStopSequence(allStops.filter(isDeliveryStop));
    }, [allStops]);


    const displayTotalStops = deliveryStops.length;
    // "Hoàn thành" = đã giao (DELIVERED) - đếm theo raw orderStatus, KHÔNG dùng mapped status
    // vì backend map cả PICKED_UP (đã lấy hàng, chưa giao) lẫn DELIVERED (đã giao) đều = "completed".
    const displayCompletedStops = useMemo(
        () => deliveryStops.filter((s) => (s.orderStatus || "").toUpperCase() === "DELIVERED").length,
        [deliveryStops]
    );

    const nextStop = useMemo(() => {
        for (const stop of deliveryStops) {
            if (hasValidCoords(stop)) return stop;
        }
        return null;
    }, [deliveryStops]);

    const aiBaselinePath = useMemo(() => {
        if (routeInfo?.source !== "AI") return [] as google.maps.LatLngLiteral[];
        return decodeEncodedPolyline(routeInfo?.encodedPolyline);
    }, [routeInfo?.source, routeInfo?.encodedPolyline]);

    // deliveryStops = chỉ DELIVERY/PICKUP, không tính RETURN_TO_OFFICE

    const fetchRouteData = useCallback(async () => {
        try {
            setLoading(true);

            const routeData = await orderApi.getShipperRoute();

            if (!routeData) {
                setRouteInfo(null);
                setAllStops([]);
                setLoading(false);
                return;
            }

            setRouteInfo(routeData.routeInfo);

            // Tách RETURN_TO_OFFICE + các stop đã kết thúc vận chuyển (DELIVERED/CANCELLED...) ra khỏi deliveryStops.
            // Lưu ý: KHÔNG ẩn stop "completed" thuộc PICKUP stop (PICKED_UP = đã lấy hàng nhưng vẫn phải giao).
            const routeStops = (routeData.deliveryStops || []) as DeliveryStop[];
            const filteredRouteStops = routeStops.filter((s) => !isStopHiddenFromRoute(s));

            try {
                const ordersRes = await orderApi.getShipperOrders({ page: 1, limit: 200 });
                const shipperOrders = (ordersRes.orders || []) as any[];

                // Order PICKED_UP (Đã lấy hàng) là trạng thái hợp lệ - KHÔNG ẩn.
                // Chỉ ẩn order đã DELIVERED hoặc FAILED_DELIVERY (xem isFinalDeliveryStatus).
                const visibleOrders = (shipperOrders || []).filter(
                    (o: any) => !isFinalDeliveryStatus(o?.status)
                );

                if (visibleOrders.length >= 0) {
                    const orderByTracking = new Map(
                        visibleOrders.map((o: any) => [o.trackingNumber, o])
                    );

                    const mergedStops = filteredRouteStops.map((stop) => {
                        const order = orderByTracking.get(stop.trackingNumber);
                        if (!order) return stop;
                        // Chỉ fill missing fields; KHÔNG override lat/lng backend đã set đúng theo stopType.
                        const merged: DeliveryStop = {
                            ...stop,
                            codAmount: stop.codAmount ?? order.cod ?? 0,
                            serviceType: stop.serviceType || order.serviceType?.name || order.serviceType || "",
                            priority: stop.priority || order.priority || "normal",
                        };
                        const isPickup = (stop.stopType || "").toUpperCase() === "PICKUP";
                        if (stop.latitude == null) {
                            merged.latitude = isPickup
                                ? (stop.senderLatitude ?? order.senderLatitude)
                                : (stop.recipientLatitude ?? order.recipientLatitude);
                        }
                        if (stop.longitude == null) {
                            merged.longitude = isPickup
                                ? (stop.senderLongitude ?? order.senderLongitude)
                                : (stop.recipientLongitude ?? order.recipientLongitude);
                        }
                        return merged;
                    });

                    const synced = mergedStops.filter((s): s is DeliveryStop => isDeliveryStop(s));
                    setAllStops(sortByStopSequence(synced));
                } else {
                    const filtered = filteredRouteStops.filter(isDeliveryStop);
                    setAllStops(sortByStopSequence(filtered));
                }
            } catch {
                const filtered = filteredRouteStops.filter(isDeliveryStop);
                setAllStops(sortByStopSequence(filtered));
            }
        } catch (error: any) {
            const backendMsg = error?.response?.data?.message
                || error?.response?.data
                || error?.message
                || "Lỗi khi tải dữ liệu lộ trình";
            message.error(backendMsg);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchRouteData();
    }, [fetchRouteData]);

    useEffect(() => {
        const onRefresh = () => {
            fetchRouteData();
        };
        window.addEventListener(SHIPPER_ROUTE_REFRESH_EVENT, onRefresh);
        return () => window.removeEventListener(SHIPPER_ROUTE_REFRESH_EVENT, onRefresh);
    }, [fetchRouteData]);

    useEffect(() => {
        if (!navigator.geolocation) {
            console.warn("Geolocation không được hỗ trợ");
            return;
        }

        const watchId = navigator.geolocation.watchPosition(
            (position) => {
                setCurrentPosition({
                    lat: position.coords.latitude,
                    lng: position.coords.longitude,
                });
            },
            (error) => {
                if (error.code === error.PERMISSION_DENIED) {
                    console.warn("GPS bị từ chối – bản đồ vẫn hoạt động mà không có vị trí realtime.");
                } else {
                    console.error("Không lấy được GPS shipper", error);
                }
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0,
            }
        );

        return () => navigator.geolocation.clearWatch(watchId);
    }, []);

    useEffect(() => {
        if (!nextStop) return;

        if (lastNextStopIdRef.current !== nextStop.id) {
            lastNextStopIdRef.current = nextStop.id;
            setRealtimeDirections(null);
            setDirectionsRenderKey((k) => k + 1);
        }
    }, [nextStop?.id]);

    const requestDirectionsToStop = useCallback(
        (stop: DeliveryStop) => {
            if (!isLoaded || typeof google === "undefined") {
                message.warning("Bản đồ chưa sẵn sàng");
                return;
            }

            if (!currentPosition) {
                message.warning("Chưa có vị trí hiện tại của shipper");
                return;
            }

            if (!hasValidCoords(stop)) {
                message.warning("Điểm giao chưa có tọa độ GPS trên bản đồ");
                return;
            }

            if (directionsLoading) {
                return;
            }

            const origin = {
                lat: Number(currentPosition.lat),
                lng: Number(currentPosition.lng),
            };
            const destination = {
                lat: Number(stop.latitude),
                lng: Number(stop.longitude),
            };

            const roundedOrigin = {
                lat: Number(origin.lat.toFixed(5)),
                lng: Number(origin.lng.toFixed(5)),
            };
            const roundedDestination = {
                lat: Number(destination.lat.toFixed(5)),
                lng: Number(destination.lng.toFixed(5)),
            };

            const lastQuery = lastDirectionsQueryRef.current;
            if (
                lastQuery &&
                lastQuery.stopId === stop.id &&
                Math.abs(lastQuery.origin.lat - roundedOrigin.lat) < 0.0002 &&
                Math.abs(lastQuery.origin.lng - roundedOrigin.lng) < 0.0002
            ) {
                return;
            }

            const cacheKey = `${stop.id}:${roundedOrigin.lat},${roundedOrigin.lng}->${roundedDestination.lat},${roundedDestination.lng}`;
            const cached = directionsCacheRef.current.get(cacheKey);
            if (cached) {
                setRealtimeDirections(cached);
                lastDirectionsQueryRef.current = { stopId: stop.id, origin: roundedOrigin };
                return;
            }

            const service = new google.maps.DirectionsService();
            const requestSeq = ++directionsRequestSeqRef.current;
            setDirectionsLoading(true);
            setRealtimeDirections(null);
            setDirectionsRenderKey((k) => k + 1);

            service.route(
                {
                    origin,
                    destination,
                    travelMode: google.maps.TravelMode.DRIVING,
                },
                (result, status) => {
                    if (requestSeq !== directionsRequestSeqRef.current) {
                        return;
                    }

                    setDirectionsLoading(false);

                    if (status === google.maps.DirectionsStatus.OK && result) {
                        directionsCacheRef.current.set(cacheKey, result);
                        if (directionsCacheRef.current.size > 20) {
                            const firstKey = directionsCacheRef.current.keys().next().value;
                            if (firstKey) directionsCacheRef.current.delete(firstKey);
                        }
                        lastDirectionsQueryRef.current = { stopId: stop.id, origin: roundedOrigin };

                        setRealtimeDirections(result);
                        setDirectionsRenderKey((k) => k + 1);
                        return;
                    }

                    message.warning("Không thể lấy chỉ đường, vui lòng thử lại");
                }
            );
        },
        [currentPosition, directionsLoading, isLoaded]
    );

    const handleStartRoute = async () => {
        if (!routeInfo) return;

        // Shipment-centric: nếu routeInfo.source === "SHIPMENT" thì dùng shipmentId.
        // Backend Phase 2 populate shipmentId/shipmentCode/shipmentStatus cho SHIPMENT source.
        const source = (routeInfo as any).source;
        const shipmentId = (routeInfo as any).shipmentId ?? routeInfo.id;
        const useShipmentEndpoint = source === "SHIPMENT" || (routeInfo as any).shipmentId != null;

        Modal.confirm({
            title: "Bắt đầu tuyến giao hàng",
            content: useShipmentEndpoint
                ? `Bắt đầu chuyến ${(routeInfo as any).shipmentCode ?? shipmentId}?`
                : "Bạn có chắc chắn muốn bắt đầu tuyến giao hàng này?",
            onOk: async () => {
                try {
                    if (useShipmentEndpoint) {
                        await orderApi.startShipperShipment(Number(shipmentId));
                    } else {
                        await orderApi.startShipperRoute(routeInfo.id);
                    }
                    setRouteInfo((prev) => (prev ? { ...prev, status: "in_progress" } : null));
                    message.success("Đã bắt đầu tuyến giao hàng");
                    mapCardRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
                } catch (err: any) {
                    const backendMsg = err?.response?.data?.message
                        || err?.response?.data
                        || err?.message
                        || "Không thể bắt đầu tuyến giao hàng";
                    message.error(backendMsg);
                }
            },
        });
    };

    // Đếm số stop đang ở trạng thái PICKED_UP (sẵn sàng bắt đầu giao).
    // Backend trả deliveryStops[].orderStatus là raw status từ orders.status
    // (còn deliveryStops[].status đã được map sang "in_progress"/"completed" cho UI).
    const pickedUpStopCount = useMemo(
        () => allStops.filter((s) => (s.orderStatus || "").toUpperCase() === "PICKED_UP").length,
        [allStops]
    );

    const isShipmentRoute =
        !!routeInfo &&
        ((routeInfo as any).source === "SHIPMENT" ||
            (routeInfo as any).shipmentId != null);
    const shipmentStatus = (routeInfo?.shipmentStatus || "").toString().toUpperCase();
    const routeStatus = (routeInfo?.status || "").toString().toUpperCase();
    // Fallback: nếu backend không populate shipmentStatus nhưng route đang in_progress
    // thì vẫn coi như shipment IN_TRANSIT.
    const isShipmentInTransit =
        shipmentStatus === "IN_TRANSIT" ||
        (!shipmentStatus && routeStatus === "in_progress");

    const canBulkStartDelivery =
        !!isShipmentRoute &&
        !!isShipmentInTransit &&
        pickedUpStopCount > 0;

    const handleStartDeliveryAll = async () => {
        if (!routeInfo) return;
        const shipmentId = Number((routeInfo as any).shipmentId ?? routeInfo.id);
        if (!shipmentId || Number.isNaN(shipmentId)) {
            message.error("Không xác định được shipmentId");
            return;
        }
        const shipmentCode = (routeInfo as any).shipmentCode ?? `#${shipmentId}`;

        Modal.confirm({
            title: (
                <div style={{ textAlign: "center" }}>
                    <ExclamationCircleFilled style={{ color: "#faad14", marginRight: 8 }} />
                    Bắt đầu giao tất cả
                </div>
            ),
            icon: <span style={{ display: "none" }} />,
            content: null,
            okText: "Xác nhận",
            cancelText: "Hủy",
            okButtonProps: { className: "primary-button", style: { borderRadius: 8 } },
            cancelButtonProps: { style: { borderRadius: 8 } },
            onOk: async () => {
                try {
                    setLoading(true);
                    const res = await orderApi.startDeliveryAll(shipmentId);
                    const updated = res?.updatedCount ?? 0;
                    const skipped = res?.skippedCount ?? 0;
                    message.success(
                        `Đã chuyển ${updated} đơn sang Đang giao hàng${
                            skipped > 0 ? ` (bỏ qua ${skipped} đơn không ở PICKED_UP)` : ""
                        }`
                    );
                    // Reload route/orders để cập nhật status
                    await fetchRouteData();
                } catch (err: any) {
                    const backendMsg =
                        err?.response?.data?.message ||
                        err?.response?.data ||
                        err?.message ||
                        "Không thể bắt đầu giao tất cả";
                    message.error(backendMsg);
                } finally {
                    setLoading(false);
                }
            },
        });
    };

    const handleFocusStopOnMap = (stop: DeliveryStop) => {
        if (!hasValidCoords(stop)) {
            message.warning("Điểm giao chưa có tọa độ GPS trên bản đồ");
            return;
        }
        const pos = { lat: Number(stop.latitude), lng: Number(stop.longitude) };
        mapRef.current?.panTo(pos);
        mapRef.current?.setZoom(16);
        mapCardRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
        const contact = getStopContact(stop);
        message.info(`Đã đưa bản đồ đến ${contact.name || stop.trackingNumber}`);
    };

    const handleViewStopDetail = (stop: DeliveryStop) => {
        setSelectedStop(stop);
        setDetailModal(true);
    };

    const handleReOptimize = async () => {
        if (!currentPosition) {
            message.warning("Chưa có vị trí GPS hiện tại. Hãy bật định vị.");
            return;
        }

        const currentRoute = routeInfo;
        if (!currentRoute?.id) {
            message.error("Không tìm thấy tuyến hiện tại để tái tối ưu");
            return;
        }

        // Shipment-centric: routeInfo.id là shipmentId khi source === "SHIPMENT".
        // Backend dispatch theo shipmentId để gọi reOptimizeShipmentRoute() mới.
        const isShipment = currentRoute.source === "SHIPMENT"
            || currentRoute.shipmentId != null;

        const payload: any = {
            currentLatitude: currentPosition.lat,
            currentLongitude: currentPosition.lng,
            includeRemainingStopsOnly: true,
            returnToOffice: true,
            reason: "MANUAL",
        };
        if (isShipment) {
            payload.shipmentId = currentRoute.shipmentId ?? currentRoute.id;
        } else {
            payload.routeId = currentRoute.id;
        }

        setReOptimizing(true);
        try {
            const response = await orderApi.reOptimizeShipperRoute(payload);
            message.success("Đã tối ưu lại tuyến!");
            await fetchRouteData();
        } catch (err: any) {
            const msg =
                err?.response?.data?.message ||
                err?.response?.data?.error ||
                err?.response?.data?.errors?.[0] ||
                "Tối ưu lại tuyến thất bại";
            message.error(msg);
        } finally {
            setReOptimizing(false);
        }
    };

    const getStatusColor = (status: string) => {
        const s = (status || "").toString().toUpperCase();
        switch (s) {
            case "PENDING":
            case "READY_FOR_PICKUP":
                return "default";
            case "IN_PROGRESS":
            case "IN_TRANSIT":
            case "DELIVERING":
            case "PICKED_UP":
                return "processing";
            case "COMPLETED":
            case "DELIVERED":
                return "success";
            case "FAILED":
            case "FAILED_DELIVERY":
            case "RETURNED":
            case "RETURN_FAILED_FINAL":
            case "CANCELLED":
                return "error";
            case "RETURNING":
            case "RETURN_RETRY":
                return "warning";
            default:
                return "default";
        }
    };

    const getStatusText = (status: string) => {
        const s = (status || "").toString().toUpperCase();
        switch (s) {
            case "PENDING":
                return "Chờ giao";
            case "READY_FOR_PICKUP":
                return "Sẵn sàng lấy hàng";
            case "PICKED_UP":
                return "Đã lấy hàng";
            case "IN_PROGRESS":
            case "IN_TRANSIT":
            case "DELIVERING":
                return "Đang giao";
            case "DELIVERED":
                return "Đã giao";
            case "COMPLETED":
                return "Hoàn thành";
            case "FAILED":
            case "FAILED_DELIVERY":
                return "Giao hàng thất bại";
            case "RETURNED":
                return "Đã hoàn trả";
            case "RETURNING":
                return "Đang hoàn";
            case "RETURN_RETRY":
                return "Hoàn lại";
            case "RETURN_FAILED_FINAL":
                return "Hoàn thất bại";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return status.replaceAll("_", " ");
        }
    };

    const getPriorityColor = (priority: string) => {
        return priority === "urgent" ? "red" : "default";
    };

    const getPriorityText = (priority: string) => {
        return priority === "urgent" ? "Ưu tiên" : "Bình thường";
    };

    const getStopTypeBadge = (stopType?: string) => {
        if (stopType === "PICKUP") {
            return <Tag color="purple" style={{ marginLeft: 4 }}>Lấy hàng</Tag>;
        }
        if (stopType === "DELIVERY") {
            return <Tag color="blue" style={{ marginLeft: 4 }}>Giao hàng</Tag>;
        }
        return null;
    };

    const isPickupStop = (stop: DeliveryStop) =>
        (stop.stopType || "").toString().toUpperCase() === "PICKUP";

    const getStopDisplayData = (stop: DeliveryStop) => {
        if (isPickupStop(stop)) {
            // PICKUP stop: status đã map từ raw (PICKED_UP -> "completed" mapped), dùng logic riêng
            const statusUpper = (stop.status || "").toString().toUpperCase();
            return {
                contactName: stop.senderName || stop.recipientName,
                contactPhone: stop.senderPhone || stop.recipientPhone,
                contactAddress: stop.senderAddress || stop.recipientAddress,
                typeBadge: <Tag color="purple">Lấy hàng</Tag>,
                statusBadge: (
                    <Tag color={getStatusColor(stop.status)}>
                        {statusUpper === "READY_FOR_PICKUP" || statusUpper === "PENDING"
                            ? "Chờ lấy"
                            : statusUpper === "PICKED_UP" || statusUpper === "COMPLETED"
                            ? "Đã lấy"
                            : statusUpper === "FAILED" || statusUpper === "FAILED_PICKUP"
                            ? "Lấy thất bại"
                            : getStatusText(stop.status)}
                    </Tag>
                ),
                showCod: false,
            };
        }
        // DELIVERY stop: PHẢI dùng raw orderStatus vì backend map cả PICKED_UP lẫn DELIVERED đều = "completed".
        //   Nếu dùng stop.status (mapped) thì PICKED_UP sẽ hiển thị "Hoàn thành" - sai.
        const rawOrderStatus = (stop.orderStatus || "").toString().toUpperCase();
        return {
            contactName: stop.recipientName,
            contactPhone: stop.recipientPhone,
            contactAddress: stop.recipientAddress,
            typeBadge: <Tag color="blue">Giao hàng</Tag>,
            statusBadge: (
                <Tag color={getStatusColor(rawOrderStatus)}>{getStatusText(rawOrderStatus)}</Tag>
            ),
            showCod: true,
        };
    };

    const getReoptimizeReasonText = (reason?: string | null) => {
        const r = (reason || "").toUpperCase();
        switch (r) {
            case "MANUAL": return "Thủ công";
            case "AUTO": return "Tự động";
            case "AI_OPTIMIZE": return "AI tối ưu";
            case "SCHEDULE": return "Theo lịch trình";
            case "DRIVER_REQUEST": return "Tài xế yêu cầu";
            default: return reason || "Không rõ";
        }
    };

    if (loading) {
        return (
            <div style={{ textAlign: "center", padding: "50px" }}>
                <Spin size="large" />
                <div style={{ marginTop: "16px" }}>Đang tải dữ liệu lộ trình...</div>
            </div>
        );
    }

    if (!routeInfo || deliveryStops.length === 0) {
        return (
            <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
                <Alert message="Không có lộ trình vận chuyển hôm nay" type="info" showIcon />
            </div>
        );
    }

    const completionRate =
        displayTotalStops > 0 ? (displayCompletedStops / displayTotalStops) * 100 : 0;

    return (
        <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
            <Title level={2} style={{ color: "#1C3D90", marginBottom: 24 }}>
                Lộ trình vận chuyển {routeInfo.source === "AI" && <Tag color="blue">Đã tối ưu</Tag>}
            </Title>

            <Card style={{ marginBottom: 24 }}>
                <Row gutter={16}>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Tổng điểm dừng"
                            value={displayTotalStops}
                            prefix={<NodeIndexOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Đã hoàn thành"
                            value={displayCompletedStops}
                            prefix={<CheckCircleOutlined />}
                        />
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Tổng COD thu hộ"
                            value={routeInfo.totalCOD}
                            prefix={<DollarOutlined />}
                            formatter={(value) => `${value?.toLocaleString()}đ`}
                        />
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Thời gian ước tính"
                            value={routeInfo.estimatedDuration}
                            suffix="phút"
                        />
                    </Col>
                </Row>

                <Divider />

                <Progress
                    percent={Math.round(completionRate)}
                    status="active"
                    format={(pct) => `${displayCompletedStops}/${displayTotalStops} điểm`}
                />

                {routeInfo.routeVersion && routeInfo.routeVersion > 1 && (
                    <Alert
                        style={{ marginTop: 16 }}
                        type="info"
                        showIcon
                        message={
                            <span>
                                Tuyến đã được tái tối ưu lần {routeInfo.routeVersion}
                                {routeInfo.reoptimizeReason && ` - Lý do: ${getReoptimizeReasonText(routeInfo.reoptimizeReason)}`}
                            </span>
                        }
                    />
                )}

                {nextStop && (() => {
                    const isPickup = (nextStop.stopType || "").toUpperCase() === "PICKUP";
                    const label = isPickup ? "Điểm lấy hàng tiếp theo" : "Điểm giao tiếp theo";
                    const contact = getStopContact(nextStop);
                    return (
                        <Alert
                            style={{ marginTop: 16 }}
                            type="info"
                            showIcon
                            message={
                                <span>
                                    {label} #{nextStop.stopSequence}:{" "}
                                    <strong>{nextStop.trackingNumber}</strong> — {contact.name}
                                </span>
                            }
                        />
                    );
                })()}

                <Flex style={{ marginTop: 16, width: "100%" }} justify="space-between" align="center" gap="middle" wrap="wrap">
                    <Space wrap>
                        {currentPosition && (
                            <Button
                                type="default"
                                className="filter-button"
                                icon={<ReloadOutlined spin={false} />}
                                onClick={handleReOptimize}
                                disabled={reOptimizing}
                            >
                                {reOptimizing ? "Đang tối ưu..." : "Tối ưu lại tuyến"}
                            </Button>
                        )}
                        {routeInfo.status === "in_progress" && (
                            <Button className="filter-button" icon={<PauseCircleOutlined />}>Tạm dừng</Button>
                        )}
                    </Space>
                    <Space wrap>
                        {routeInfo.status === "not_started" && (
                            <Button type="primary" className="primary-button" icon={<PlayCircleOutlined />} onClick={handleStartRoute}>
                                Bắt đầu tuyến
                            </Button>
                        )}
                        {routeInfo.status === "in_progress" && (
                            <Button type="primary" className="primary-button" icon={<CompassOutlined />} onClick={() => navigate("/shipper/orders")}>
                                Xem đơn hàng
                            </Button>
                        )}
                        {canBulkStartDelivery && (
                            <Button
                                type="primary"
                                className="primary-button"
                                icon={<PlayCircleOutlined />}
                                loading={loading}
                                onClick={handleStartDeliveryAll}
                            >
                                Bắt đầu giao tất cả ({pickedUpStopCount})
                            </Button>
                        )}
                    </Space>
                </Flex>
            </Card>

            <Card
                title="Bản đồ điều hướng realtime"
                style={{ marginBottom: 24 }}
                ref={mapCardRef}
                extra={
                    <Space>
                        {nextStop && <Tag color="orange">Điểm tiếp theo: #{nextStop.stopSequence}</Tag>}
                        <Button
                            size="small"
                            type="primary"
                            className="primary-button"
                            disabled={!isLoaded || !currentPosition || !nextStop || !hasValidCoords(nextStop)}
                            onClick={() => {
                                if (!nextStop) return;
                                requestDirectionsToStop(nextStop);
                            }}
                        >
                            Chỉ đường tới điểm tiếp theo
                        </Button>
                        <Button
                            size="small"
                            className="filter-button"
                            icon={<EnvironmentOutlined />}
                            onClick={() => {
                                if (!currentPosition) {
                                    message.warning("Chưa có vị trí hiện tại của shipper");
                                    return;
                                }

                                mapRef.current?.panTo(currentPosition);
                                mapRef.current?.setZoom(15);
                            }}
                        >
                            Theo dõi vị trí
                        </Button>
                    </Space>
                }
            >
                <div style={{ width: "100%", position: "relative" }}>
                    {loadError && (
                        <Alert
                            type="error"
                            message="Không thể tải Google Maps. Kiểm tra API key."
                            style={{ padding: 16 }}
                        />
                    )}

                    {!isLoaded && !loadError && (
                        <div
                            style={{
                                position: "absolute",
                                inset: 0,
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                background: "rgba(255,255,255,0.75)",
                                zIndex: 2,
                                minHeight: MAP_CONTAINER_STYLE.height,
                            }}
                        >
                            <Spin tip="Đang tải bản đồ..." />
                        </div>
                    )}

                    {isLoaded && !loadError && (
                        <GoogleMap
                            mapContainerStyle={MAP_CONTAINER_STYLE}
                            center={DEFAULT_CENTER}
                            zoom={13}
                            options={{
                                gestureHandling: "greedy",
                                scrollwheel: true,
                            }}
                            onLoad={(map) => {
                                mapRef.current = map;
                            }}
                        >
                            {currentPosition && (
                                <MarkerWithIcon
                                    position={currentPosition}
                                    title="Vị trí hiện tại"
                                    zIndex={10000}
                                    isCurrentPosition
                                />
                            )}

                            {aiBaselinePath.length > 0 && (
                                <PolylineF
                                    key={`ai-baseline-${routeInfo?.id ?? "route"}`}
                                    path={aiBaselinePath}
                                    options={{
                                        strokeColor: "#8c8c8c",
                                        strokeOpacity: 0.45,
                                        strokeWeight: 5,
                                        zIndex: 3000,
                                    }}
                                />
                            )}

                            {realtimeDirections?.routes?.[0]?.overview_path && (
                                <PolylineF
                                    key={`route-polyline-${directionsRenderKey}`}
                                    path={realtimeDirections.routes[0].overview_path}
                                    options={{
                                        strokeColor: "#1890ff",
                                        strokeOpacity: 0.95,
                                        strokeWeight: 6,
                                        zIndex: 6000,
                                    }}
                                />
                            )}

                            {deliveryStops.map((stop) => {
                                if (!hasValidCoords(stop)) return null;

                                const isNext = nextStop?.id === stop.id;
                                const isPickup = isPickupStop(stop);
                                const markerColor = isPickup ? "#722ed1" : "#1890ff";

                                return (
                                    <MarkerF
                                        key={stop.id}
                                        position={{
                                            lat: Number(stop.latitude),
                                            lng: Number(stop.longitude),
                                        }}
                                        label={
                                            stop.stopSequence != null
                                                ? {
                                                      text: String(stop.stopSequence),
                                                      color: "#fff",
                                                      fontWeight: "bold",
                                                  }
                                                : undefined
                                        }
                                        title={
                                            isNext
                                                ? `Điểm tiếp theo: ${isPickup ? stop.senderName || stop.recipientName : stop.recipientName}`
                                                : (isPickup ? stop.senderName || stop.recipientName : stop.recipientName)
                                        }
                                        zIndex={isNext ? 9999 : 1000}
                                    />
                                );
                            })}
                        </GoogleMap>
                    )}
                </div>
            </Card>

            <Card title={`Danh sách điểm giao hàng (${displayTotalStops} điểm, theo thứ tự AI)`}>
                <List
                    dataSource={deliveryStops}
                    renderItem={(stop) => {
                        const isNext = nextStop?.id === stop.id;
                        return (
                            <List.Item
                                actions={[
                                    <Button
                                        className="filter-button"
                                        icon={<EnvironmentOutlined />}
                                        onClick={() => {
                                            requestDirectionsToStop(stop);
                                            handleFocusStopOnMap(stop);
                                        }}
                                    >
                                        Chỉ đường
                                    </Button>,
                                    <Button className="filter-button" icon={<EyeOutlined />} onClick={() => handleViewStopDetail(stop)}>
                                        Chi tiết
                                    </Button>,
                                    <Button type="link" onClick={() => navigate(`/shipper/orders/${stop.id}`)}>
                                        Xem đơn
                                    </Button>,
                                ]}
                            >
                                <List.Item.Meta
                                    avatar={
                                        <div
                                            style={{
                                                width: 40,
                                                height: 40,
                                                borderRadius: "50%",
                                                background: "#1890ff",
                                                color: "#fff",
                                                display: "flex",
                                                alignItems: "center",
                                                justifyContent: "center",
                                                fontWeight: "bold",
                                            }}
                                        >
                                            {stop.stopSequence ?? "—"}
                                        </div>
                                    }
                                    title={
                                        <Space>
                                            {isNext && <Tag color="orange">Tiếp theo</Tag>}
                                            <Text strong>{stop.trackingNumber}</Text>
                                            {getStopTypeBadge(stop.stopType)}
                                            <Tag color={getPriorityColor(stop.priority)}>{getPriorityText(stop.priority)}</Tag>
                                            {(() => {
                                                const d = getStopDisplayData(stop);
                                                return d.statusBadge;
                                            })()}
                                        </Space>
                                    }
                                    description={
                                        <Space direction="vertical" size={4}>
                                            {(() => {
                                                const d = getStopDisplayData(stop);
                                                return (
                                                    <>
                                                        <Text>
                                                            <PhoneOutlined /> {d.contactPhone} - {d.contactName}
                                                        </Text>
                                                        <Text type="secondary">
                                                            <EnvironmentOutlined /> {d.contactAddress}
                                                        </Text>
                                                        <Text type="secondary">
                                                            {stop.etaTime ? `ETA: ${stop.etaTime}` : "ETA: Chưa có"}
                                                        </Text>
                                                        {d.showCod && (stop.codAmount ?? 0) > 0 && (
                                                            <Text>
                                                                <DollarOutlined /> COD thu hộ: {stop.codAmount.toLocaleString()}đ
                                                            </Text>
                                                        )}
                                                    </>
                                                );
                                            })()}
                                        </Space>
                                    }
                                />
                            </List.Item>
                        );
                    }}
                />
            </Card>

            <Modal
                title={selectedStop?.stopType === "PICKUP" ? "Chi tiết điểm lấy hàng" : "Chi tiết điểm giao hàng"}
                open={detailModal}
                onCancel={() => {
                    setDetailModal(false);
                    setSelectedStop(null);
                }}
                footer={null}
                width={600}
            >
                {selectedStop && (() => {
                    const isPickup = selectedStop.stopType === "PICKUP";
                    const name = isPickup
                        ? (selectedStop.senderName || selectedStop.recipientName)
                        : selectedStop.recipientName;
                    const phone = isPickup
                        ? (selectedStop.senderPhone || selectedStop.recipientPhone)
                        : selectedStop.recipientPhone;
                    const address = isPickup
                        ? (selectedStop.senderAddress || selectedStop.recipientAddress)
                        : selectedStop.recipientAddress;
                    return (
                        <Space direction="vertical" size="large" style={{ width: "100%" }}>
                            <div>
                                <Text strong>Mã đơn hàng: </Text>
                                <Text>{selectedStop.trackingNumber}</Text>
                            </div>
                            <div>
                                <Text strong>{isPickup ? "Người gửi: " : "Người nhận: "}</Text>
                                <Text>{name}</Text>
                            </div>
                            <div>
                                <Text strong>SĐT: </Text>
                                <Text>{phone}</Text>
                            </div>
                            <div>
                                <Text strong>Địa chỉ: </Text>
                                <Text>{address}</Text>
                            </div>
                            <div>
                                <Text strong>COD: </Text>
                                <Text style={{ color: "#f50" }}>{selectedStop.codAmount.toLocaleString()}đ</Text>
                            </div>
                            <div>
                                <Text strong>Dịch vụ: </Text>
                                <Text>{selectedStop.serviceType}</Text>
                            </div>
                            <div>
                                <Text strong>Trạng thái: </Text>
                                {(() => {
                                    // Dùng raw orderStatus (cho DELIVERY stop) thay vì mapped status,
                                    // tránh PICKED_UP hiển thị "Hoàn thành".
                                    const isPickup = (selectedStop.stopType || "").toUpperCase() === "PICKUP";
                                    const statusKey = isPickup
                                        ? selectedStop.status
                                        : (selectedStop.orderStatus || selectedStop.status);
                                    return <Tag color={getStatusColor(statusKey)}>{getStatusText(statusKey)}</Tag>;
                                })()}
                            </div>
                            <Button
                                type="primary"
                                className="primary-button"
                                block
                                icon={<EnvironmentOutlined />}
                                onClick={() => handleFocusStopOnMap(selectedStop)}
                            >
                                Xem trên bản đồ
                            </Button>
                        </Space>
                    );
                })()}
            </Modal>
        </div>
    );
};

const CURRENT_POSITION_ICON = {
    url: "data:image/svg+xml;utf8,%3Csvg xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22 viewBox%3D%220 0 20 20%22%3E%3Ccircle cx%3D%2210%22 cy%3D%2210%22 r%3D%228%22 fill%3D%22%231890ff%22 stroke%3D%22white%22 stroke-width%3D%223%22%2F%3E%3C%2Fsvg%3E",
    scaledSize: { width: 20, height: 20 } as unknown as google.maps.Size,
    anchor: { x: 10, y: 10 } as unknown as google.maps.Point,
};

const MarkerWithIcon: React.FC<{
    position: google.maps.LatLngLiteral;
    title: string;
    zIndex: number;
    isCurrentPosition: boolean;
}> = ({ position, title, zIndex, isCurrentPosition }) => {
    return (
        <MarkerF
            position={position}
            title={title}
            zIndex={zIndex}
            icon={isCurrentPosition ? CURRENT_POSITION_ICON : undefined}
        />
    );
};

export default ShipperDeliveryRoute;
