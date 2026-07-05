import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState} from "react";
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
import orderApi from "../../../api/orderApi";
import {SHIPPER_ROUTE_REFRESH_EVENT} from "./deliveryRouteEvents";
import StopDetailModal from "./components/StopDetailModal";

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
        || st === "FINAL";
};

const isStopHiddenFromRoute = (stop: DeliveryStop) => {
    const rawOrderStatus = (stop.orderStatus || "").toString().toUpperCase();
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
    return st === "DELIVERY" || st === "PICKUP" || st === "RETURN_TO_OFFICE";
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

const DeliveryRoutePage: React.FC = () => {
    const navigate = useNavigate();
    const [routeInfo, setRouteInfo] = useState<RouteInfo | null>(null);
    const [allStops, setAllStops] = useState<DeliveryStop[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedStop, setSelectedStop] = useState<DeliveryStop | null>(null);
    const [detailModal, setDetailModal] = useState(false);
    const [currentPosition, setCurrentPosition] = useState<google.maps.LatLngLiteral | null>(null);
    const [realtimeDirections, setRealtimeDirections] = useState<google.maps.DirectionsResult | null>(null);
    const [directionsRenderKey, setDirectionsRenderKey] = useState(0);
    const [reOptimizing, setReOptimizing] = useState(false);

    const mapRef = useRef<google.maps.Map | null>(null);
    const mapCardRef = useRef<HTMLDivElement | null>(null);
    const lastNextStopIdRef = useRef<number | null>(null);
    const directionsRequestSeqRef = useRef(0);
    const lastDirectionsQueryRef = useRef<{ stopId: number; origin: google.maps.LatLngLiteral } | null>(null);
    const directionsCacheRef = useRef<Map<string, google.maps.DirectionsResult>>(new Map());
    const routeDataVersionRef = useRef<number>(0);

    const { isLoaded, loadError } = useJsApiLoader({
        id: "google-maps-script",
        googleMapsApiKey: (import.meta.env.VITE_GOOGLE_MAPS_KEY as string) || "",
    });

    const deliveryStops = useMemo(() => {
        return sortByStopSequence(allStops.filter(isDeliveryStop));
    }, [allStops]);

    const mapCenterAndBounds = useMemo(() => {
        const validStops = deliveryStops.filter(hasValidCoords);
        if (validStops.length === 0) {
            return { center: DEFAULT_CENTER, bounds: null, hasBounds: false };
        }
        const lats = validStops.map((s) => Number(s.latitude));
        const lngs = validStops.map((s) => Number(s.longitude));
        const minLat = Math.min(...lats);
        const maxLat = Math.max(...lats);
        const minLng = Math.min(...lngs);
        const maxLng = Math.max(...lngs);
        const centerLat = (minLat + maxLat) / 2;
        const centerLng = (minLng + maxLng) / 2;
        return {
            center: { lat: centerLat, lng: centerLng },
            bounds: { north: maxLat, south: minLat, east: maxLng, west: minLng },
            hasBounds: true,
        };
    }, [deliveryStops]);

    const displayTotalStops = deliveryStops.length;
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
            routeDataVersionRef.current += 1;
            const routeStops = (routeData.deliveryStops || []) as DeliveryStop[];
            const filteredRouteStops = routeStops.filter((s) => !isStopHiddenFromRoute(s));

            try {
                const ordersRes = await orderApi.getShipperOrders({ page: 1, limit: 200 });
                const shipperOrders = (ordersRes.orders || []) as any[];
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
        const map = mapRef.current;
        if (!map || !mapCenterAndBounds.hasBounds || !mapCenterAndBounds.bounds) return;
        const bounds = new google.maps.LatLngBounds(
            { lat: mapCenterAndBounds.bounds.south, lng: mapCenterAndBounds.bounds.west },
            { lat: mapCenterAndBounds.bounds.north, lng: mapCenterAndBounds.bounds.east }
        );
        map.fitBounds(bounds, 50);
    }, [deliveryStops, routeInfo, mapCenterAndBounds]);

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
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
        return () => navigator.geolocation.clearWatch(watchId);
    }, []);

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
            setDirectionsRenderKey((k) => k + 1);
            service.route(
                { origin, destination, travelMode: google.maps.TravelMode.DRIVING },
                (result, status) => {
                    if (requestSeq !== directionsRequestSeqRef.current) return;
                    if (status === google.maps.DirectionsStatus.OK && result) {
                        directionsCacheRef.current.set(cacheKey, result);
                        if (directionsCacheRef.current.size > 20) {
                            const firstKey = directionsCacheRef.current.keys().next().value;
                            if (firstKey) directionsCacheRef.current.delete(firstKey);
                        }
                        lastDirectionsQueryRef.current = { stopId: stop.id, origin: roundedOrigin };
                        setRealtimeDirections(result);
                        setDirectionsRenderKey((k) => k + 1);
                    } else {
                        console.warn("[DeliveryRoute] Directions API failed:", status);
                        message.warning("Không thể vẽ đường đi, vui lòng thử lại");
                    }
                }
            );
        },
        [currentPosition, isLoaded]
    );

    const handleStartRoute = async () => {
        if (!routeInfo) return;
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
        const isShipment = currentRoute.source === "SHIPMENT"
            || currentRoute.shipmentId != null;
        const payload: any = {
            currentLatitude: currentPosition.lat,
            currentLongitude: currentPosition.lng,
            includeRemainingStopsOnly: true,
            returnToOffice: true,
            reason: "MANUAL",
            // Truyền thời gian hiện tại để backend tính ETA đúng
            departureTime: new Date().toISOString(),
        };
        if (isShipment) {
            payload.shipmentId = currentRoute.shipmentId ?? currentRoute.id;
        } else {
            payload.routeId = currentRoute.id;
        }
        setReOptimizing(true);
        try {
            await orderApi.reOptimizeShipperRoute(payload);
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
            case "RETURN_AT_ORIGIN_OFFICE":
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
                return "Đang hoàn trả";
            case "RETURN_AT_ORIGIN_OFFICE":
                return "Đã hoàn về bưu cục gốc";
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

    const getStopTypeBadge = (stopType?: string) => {
        if (stopType === "PICKUP") {
            return <Tag color="purple" style={{ marginLeft: 4 }}>Lấy hàng</Tag>;
        }
        if (stopType === "DELIVERY") {
            return <Tag color="blue" style={{ marginLeft: 4 }}>Giao hàng</Tag>;
        }
        if (stopType === "RETURN_TO_OFFICE") {
            return <Tag color="orange" style={{ marginLeft: 4 }}>Hoàn trả</Tag>;
        }
        return null;
    };

    // Badge Cần thu COD: chỉ hiển thị nếu codAmount > 0
    const getCodBadge = (stop: DeliveryStop) => {
        const cod = stop.codAmount ?? 0;
        if (cod <= 0) return null;
        return <Tag color="green" style={{ marginLeft: 4 }}>Cần thu COD</Tag>;
    };

    // Badge Trễ ETA: chỉ hiển thị nếu etaTime đã qua giờ hiện tại (theo timezone Việt Nam)
    const getEtaWarningBadge = (stop: DeliveryStop) => {
        const etaTime = stop.etaTime;
        if (!etaTime) return null;

        try {
            const now = new Date();
            // Lấy giờ Việt Nam (UTC+7)
            const vietnamOffset = 7 * 60; // phút
            const localNow = new Date(now.getTime() + vietnamOffset * 60 * 1000);

            // Parse etaTime
            // Format có thể là HH:mm hoặc ISO datetime
            let etaDate: Date;
            if (etaTime.includes('T') || etaTime.includes('-')) {
                // ISO datetime format
                etaDate = new Date(etaTime);
            } else {
                // HH:mm format - ghép với ngày hôm nay
                const [hours, minutes] = etaTime.split(':').map(Number);
                const today = new Date(localNow);
                today.setHours(hours, minutes, 0, 0);
                etaDate = today;
            }

            // So sánh với giờ hiện tại (UTC+7)
            if (etaDate < localNow) {
                return <Tag color="red" style={{ marginLeft: 4 }}>Trễ ETA</Tag>;
            }
        } catch {
            // Invalid etaTime format
        }
        return null;
    };

    const isPickupStop = (stop: DeliveryStop) =>
        (stop.stopType || "").toString().toUpperCase() === "PICKUP";

    const isReturnStop = (stop: DeliveryStop) =>
        (stop.stopType || "").toString().toUpperCase() === "RETURN_TO_OFFICE";

    const getStopDisplayData = (stop: DeliveryStop) => {
        if (isPickupStop(stop)) {
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
        if (isReturnStop(stop)) {
            const rawOrderStatus = (stop.orderStatus || "").toString().toUpperCase();
            return {
                contactName: stop.senderName || stop.recipientName,
                contactPhone: stop.senderPhone || stop.recipientPhone,
                contactAddress: stop.senderAddress || stop.recipientAddress,
                typeBadge: <Tag color="orange">Hoàn trả</Tag>,
                statusBadge: (
                    <Tag color={getStatusColor(rawOrderStatus)}>{getStatusText(rawOrderStatus)}</Tag>
                ),
                showCod: false,
            };
        }
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
                            center={mapCenterAndBounds.center}
                            zoom={13}
                            options={{
                                gestureHandling: "greedy",
                                scrollwheel: true,
                            }}
                            onLoad={(map) => {
                                mapRef.current = map;
                                if (mapCenterAndBounds.hasBounds && mapCenterAndBounds.bounds) {
                                    const bounds = new google.maps.LatLngBounds(
                                        { lat: mapCenterAndBounds.bounds.south, lng: mapCenterAndBounds.bounds.west },
                                        { lat: mapCenterAndBounds.bounds.north, lng: mapCenterAndBounds.bounds.east }
                                    );
                                    map.fitBounds(bounds, 50);
                                }
                            }}
                        >
                            {currentPosition && (
                                <MarkerWithIcon
                                    position={currentPosition}
                                    title="Vị trí hiện tại của bạn"
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
                                        strokeOpacity: 0.35,
                                        strokeWeight: 4,
                                        zIndex: 1000,
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
                                        zIndex: 5000,
                                    }}
                                />
                            )}

                            {deliveryStops.map((stop) => {
                                if (!hasValidCoords(stop)) return null;
                                const isNext = nextStop?.id === stop.id;
                                const isPickup = isPickupStop(stop);
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

            <Card title={`Danh sách điểm xử lý (${displayTotalStops} điểm)`}>
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
                                            {getCodBadge(stop)}
                                            {getEtaWarningBadge(stop)}
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

            <StopDetailModal
                open={detailModal}
                stop={selectedStop}
                onClose={() => {
                    setDetailModal(false);
                    setSelectedStop(null);
                }}
                onFocusOnMap={handleFocusStopOnMap}
                getStatusColor={getStatusColor}
                getStatusText={getStatusText}
            />
        </div>
    );
};

// Tạo icon marker vị trí shipper
const createShipperMarkerIcon = (): google.maps.Icon => ({
    url:
        "data:image/svg+xml;charset=UTF-8," +
        encodeURIComponent(`
            <svg xmlns="http://www.w3.org/2000/svg" width="44" height="52" viewBox="0 0 44 52">
                <defs>
                    <filter id="shadow" x="-40%" y="-40%" width="180%" height="180%">
                        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#000000" flood-opacity="0.22"/>
                    </filter>
                </defs>
                <path
                    filter="url(#shadow)"
                    d="M22 1.5C11.2 1.5 2.5 10.2 2.5 21c0 14.2 19.5 29.5 19.5 29.5S41.5 35.2 41.5 21C41.5 10.2 32.8 1.5 22 1.5Z"
                    fill="#1890ff"
                    stroke="#ffffff"
                    stroke-width="3"
                />
                <g fill="none" stroke="#ffffff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="9" y="20" width="7" height="6" rx="1.2" fill="#ffffff" stroke="none"/>
                    <path d="M15.5 28H27.5L31 22"/>
                    <path d="M24 20L27.5 28"/>
                    <circle cx="24" cy="13" r="3" fill="#ffffff" stroke="none"/>
                    <path d="M23 17.5L20 22L24.5 24.5"/>
                    <path d="M25 18L29 22"/>
                    <path d="M30.5 21.5H34"/>
                    <circle cx="14" cy="31" r="4.2"/>
                    <circle cx="30" cy="31" r="4.2"/>
                    <path d="M7 25H11"/>
                    <path d="M5.5 29H9"/>
                </g>
            </svg>
        `),
    scaledSize: new google.maps.Size(44, 52),
    anchor: new google.maps.Point(22, 52),
});

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
            icon={
                isCurrentPosition && typeof google !== "undefined"
                    ? createShipperMarkerIcon()
                    : undefined
            }
        />
    );
};

export default DeliveryRoutePage;
