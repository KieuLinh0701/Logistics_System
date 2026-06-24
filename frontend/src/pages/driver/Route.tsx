import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {Alert, Button, Card, Col, List, message, Row, Space, Spin, Statistic, Tag, Typography,} from "antd";
import {CompassOutlined, EnvironmentOutlined, ReloadOutlined} from "@ant-design/icons";
import {GoogleMap, MarkerF, PolylineF, useJsApiLoader} from "@react-google-maps/api";
import shipmentApi from "../../api/shipmentApi";
import type {DriverDeliveryStop, DriverRouteInfo} from "../../types/shipment";

const { Title, Text } = Typography;

const MAP_CONTAINER_STYLE = {
    width: "100%",
    height: "520px",
};

const DEFAULT_CENTER = {
    lat: 10.9804,
    lng: 106.6519,
};

const hasValidCoords = (lat?: number | null, lng?: number | null) =>
    lat != null &&
    lng != null &&
    !Number.isNaN(Number(lat)) &&
    !Number.isNaN(Number(lng));

const toNumber = (v: unknown): number | null => {
    if (v == null) return null;
    if (typeof v === "number") return v;
    if (typeof v === "string" && v.trim() !== "" && !isNaN(Number(v))) {
        return Number(v);
    }
    return null;
};

const CURRENT_POSITION_ICON = {
    url: "data:image/svg+xml;utf8,%3Csvg xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22 viewBox%3D%220 0 20 20%22%3E%3Ccircle cx%3D%2210%22 cy%3D%2210%22 r%3D%228%22 fill%3D%22%231890ff%22 stroke%3D%22white%22 stroke-width%3D%223%22%2F%3E%3C%2Fsvg%3E",
    scaledSize: { width: 22, height: 22 } as unknown as google.maps.Size,
    anchor: { x: 11, y: 11 } as unknown as google.maps.Point,
};

const DESTINATION_ICON = {
    url: "data:image/svg+xml;utf8,%3Csvg xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22 viewBox%3D%220 0 32 32%22%3E%3Cpath d%3D%22M16%202C9.92%202%205%206.6%205%2012.2c0%207.4%2010.36%2017.04%2010.8%2017.44a.8.8%200%200%200%201.12%200C17.36%2029.24%2028%2019.6%2028%2012.2%2028%206.6%2023.08%202%2016%202z%22 fill%3D%22%23FF4D4F%22 stroke%3D%22white%22 stroke-width%3D%221.5%22%2F%3E%3Cpath d%3D%22M12%2012h8v8h-3v-5h-2v5h-3z%22 fill%3D%22white%22%2F%3E%3C%2Fsvg%3E",
    scaledSize: { width: 40, height: 40 } as unknown as google.maps.Size,
    anchor: { x: 20, y: 36 } as unknown as google.maps.Point,
};

const DriverRoute: React.FC = () => {
    const [routeInfo, setRouteInfo] = useState<DriverRouteInfo | null>(null);
    const [deliveryStops, setDeliveryStops] = useState<DriverDeliveryStop[]>([]);
    const [loading, setLoading] = useState(false);
    const [currentPosition, setCurrentPosition] = useState<google.maps.LatLngLiteral | null>(null);
    const [realtimeDirections, setRealtimeDirections] = useState<google.maps.DirectionsResult | null>(null);
    const [directionsLoading, setDirectionsLoading] = useState(false);
    const [directionsRenderKey, setDirectionsRenderKey] = useState(0);

    const mapRef = useRef<google.maps.Map | null>(null);
    const mapCardRef = useRef<HTMLDivElement | null>(null);
    const directionsRequestSeqRef = useRef(0);
    const lastDirectionsQueryRef = useRef<{ origin: google.maps.LatLngLiteral; dest: google.maps.LatLngLiteral } | null>(null);
    const directionsCacheRef = useRef<Map<string, google.maps.DirectionsResult>>(new Map());

    const { isLoaded, loadError } = useJsApiLoader({
        id: "google-maps-script",
        googleMapsApiKey: (import.meta.env.VITE_GOOGLE_MAPS_KEY as string) || "",
    });

    const destination = useMemo<google.maps.LatLngLiteral | null>(() => {
        const toOffice = routeInfo?.toOffice;
        if (!toOffice) return null;
        const lat = toNumber((toOffice as any).latitude);
        const lng = toNumber((toOffice as any).longitude);
        if (lat == null || lng == null) return null;
        return { lat, lng };
    }, [routeInfo?.toOffice]);

    const fetchRouteData = useCallback(async () => {
        try {
            setLoading(true);
            const routeData = await shipmentApi.getDriverRoute();
            setRouteInfo(routeData.routeInfo);
            setDeliveryStops(routeData.deliveryStops || []);
        } catch (error: any) {
            const backendMsg =
                error?.response?.data?.message ||
                error?.response?.data ||
                error?.message ||
                "Lỗi khi tải dữ liệu lộ trình";
            message.error(backendMsg);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchRouteData();
    }, [fetchRouteData]);

    // Theo dõi vị trí realtime của tài xế
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
                    console.error("Không lấy được GPS tài xế", error);
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

    // Fit bounds khi có cả vị trí hiện tại & bưu cục đích
    useEffect(() => {
        if (!isLoaded || !mapRef.current) return;
        if (currentPosition && destination) {
            const bounds = new google.maps.LatLngBounds();
            bounds.extend(currentPosition);
            bounds.extend(destination);
            mapRef.current.fitBounds(bounds, 80);
        } else if (destination) {
            mapRef.current.panTo(destination);
            mapRef.current.setZoom(15);
        } else if (currentPosition) {
            mapRef.current.panTo(currentPosition);
            mapRef.current.setZoom(15);
        }
    }, [isLoaded, currentPosition, destination]);

    const requestDirectionsToDestination = useCallback(() => {
        if (!isLoaded || typeof google === "undefined") {
            message.warning("Bản đồ chưa sẵn sàng");
            return;
        }
        if (!currentPosition) {
            message.warning("Chưa có vị trí hiện tại của tài xế");
            return;
        }
        if (!destination) {
            message.warning("Bưu cục đích chưa có tọa độ GPS trên bản đồ");
            return;
        }
        if (directionsLoading) {
            return;
        }

        const origin = { lat: Number(currentPosition.lat), lng: Number(currentPosition.lng) };
        const dest = { lat: Number(destination.lat), lng: Number(destination.lng) };

        const roundedOrigin = {
            lat: Number(origin.lat.toFixed(5)),
            lng: Number(origin.lng.toFixed(5)),
        };
        const roundedDest = {
            lat: Number(dest.lat.toFixed(5)),
            lng: Number(dest.lng.toFixed(5)),
        };

        const last = lastDirectionsQueryRef.current;
        if (
            last &&
            Math.abs(last.origin.lat - roundedOrigin.lat) < 0.0002 &&
            Math.abs(last.origin.lng - roundedOrigin.lng) < 0.0002 &&
            Math.abs(last.dest.lat - roundedDest.lat) < 0.0002 &&
            Math.abs(last.dest.lng - roundedDest.lng) < 0.0002
        ) {
            return;
        }

        const cacheKey = `${roundedOrigin.lat},${roundedOrigin.lng}->${roundedDest.lat},${roundedDest.lng}`;
        const cached = directionsCacheRef.current.get(cacheKey);
        if (cached) {
            setRealtimeDirections(cached);
            lastDirectionsQueryRef.current = { origin: roundedOrigin, dest: roundedDest };
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
                destination: dest,
                travelMode: google.maps.TravelMode.DRIVING,
            },
            (result, status) => {
                if (requestSeq !== directionsRequestSeqRef.current) {
                    return;
                }
                setDirectionsLoading(false);

                if (status === google.maps.DirectionsStatus.OK && result) {
                    directionsCacheRef.current.set(cacheKey, result);
                    if (directionsCacheRef.current.size > 10) {
                        const firstKey = directionsCacheRef.current.keys().next().value;
                        if (firstKey) directionsCacheRef.current.delete(firstKey);
                    }
                    lastDirectionsQueryRef.current = { origin: roundedOrigin, dest: roundedDest };
                    setRealtimeDirections(result);
                    setDirectionsRenderKey((k) => k + 1);
                    return;
                }

                message.warning("Không thể lấy chỉ đường, vui lòng thử lại");
            }
        );
    }, [currentPosition, destination, directionsLoading, isLoaded]);

    const handleFollowMyLocation = useCallback(() => {
        if (!currentPosition) {
            message.warning("Chưa có vị trí hiện tại của tài xế");
            return;
        }
        if (!mapRef.current) {
            return;
        }
        if (destination) {
            const bounds = new google.maps.LatLngBounds();
            bounds.extend(currentPosition);
            bounds.extend(destination);
            mapRef.current.fitBounds(bounds, 80);
        } else {
            mapRef.current.panTo(currentPosition);
            mapRef.current.setZoom(15);
        }
    }, [currentPosition, destination]);

    const translateStatus = (status?: string) => {
        if (!status) return "";
        switch (status.toString().toUpperCase()) {
            case "PENDING":
                return "Chưa bắt đầu";
            case "IN_TRANSIT":
                return "Đang vận chuyển";
            case "COMPLETED":
                return "Hoàn tất";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return status;
        }
    };

    if (loading) {
        return (
            <div style={{ textAlign: "center", padding: "50px" }}>
                <Spin size="large" />
                <div style={{ marginTop: 16 }}>Đang tải dữ liệu lộ trình...</div>
            </div>
        );
    }

    if (!routeInfo) {
        return (
            <div style={{ padding: 24 }}>
                <Alert
                    message="Không có lộ trình vận chuyển"
                    description="Hiện tại bạn chưa có chuyến hàng đang hoạt động."
                    type="info"
                    showIcon
                />
            </div>
        );
    }

    return (
        <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
            <Title level={2} style={{ color: "#1C3D90", marginBottom: 24 }}>
                Lộ trình vận chuyển
            </Title>

            <Card style={{ marginBottom: 16 }}>
                <Row gutter={16}>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Mã chuyến"
                            value={routeInfo.code || `#${routeInfo.id}`}
                        />
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Số điểm dừng"
                            value={routeInfo.totalStops}
                        />
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Tổng đơn"
                            value={routeInfo.totalOrders}
                        />
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Statistic
                            title="Trạng thái"
                            value={translateStatus(routeInfo.status)}
                        />
                    </Col>
                </Row>
            </Card>

            <Card
                title="Bản đồ điều hướng"
                ref={mapCardRef}
                extra={
                    <Space>
                        <Button
                            size="small"
                            type="primary"
                            icon={<CompassOutlined />}
                            disabled={!isLoaded || !currentPosition || !destination}
                            loading={directionsLoading}
                            onClick={requestDirectionsToDestination}
                        >
                            Chỉ đường tới bưu cục đích
                        </Button>
                        <Button
                            size="small"
                            icon={<EnvironmentOutlined />}
                            onClick={handleFollowMyLocation}
                            disabled={!isLoaded || !currentPosition}
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
                            center={destination ?? currentPosition ?? DEFAULT_CENTER}
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
                                <MarkerF
                                    position={currentPosition}
                                    title="Vị trí hiện tại của tài xế"
                                    zIndex={10000}
                                    icon={CURRENT_POSITION_ICON}
                                />
                            )}

                            {destination && (
                                <MarkerF
                                    position={destination}
                                    title={routeInfo.toOffice?.name || "Bưu cục đích"}
                                    zIndex={9000}
                                    icon={DESTINATION_ICON}
                                />
                            )}

                            {realtimeDirections?.routes?.[0]?.overview_path && (
                                <PolylineF
                                    key={`driver-route-${directionsRenderKey}`}
                                    path={realtimeDirections.routes[0].overview_path}
                                    options={{
                                        strokeColor: "#1890ff",
                                        strokeOpacity: 0.95,
                                        strokeWeight: 6,
                                        zIndex: 6000,
                                    }}
                                />
                            )}
                        </GoogleMap>
                    )}
                </div>
            </Card>

            <Card
                title="Danh sách điểm dừng"
                style={{ marginTop: 16 }}
                extra={
                    <Button
                        size="small"
                        icon={<ReloadOutlined />}
                        onClick={fetchRouteData}
                    >
                        Tải lại
                    </Button>
                }
            >
                {deliveryStops.length === 0 ? (
                    <Alert type="info" message="Chuyến này chưa có điểm dừng nào" showIcon />
                ) : (
                    <List
                        dataSource={deliveryStops}
                        renderItem={(stop, index) => {
                            const sequenceNumber = (stop as any).stopSequence ?? index + 1;
                            return (
                                <List.Item>
                                    <List.Item.Meta
                                        avatar={
                                            <div
                                                style={{
                                                    width: 48,
                                                    height: 48,
                                                    minWidth: 48,
                                                    borderRadius: "50%",
                                                    background: "#1890ff",
                                                    color: "#fff",
                                                    display: "flex",
                                                    alignItems: "center",
                                                    justifyContent: "center",
                                                    fontWeight: "bold",
                                                    fontSize: 18,
                                                    flexShrink: 0,
                                                    boxShadow: "0 2px 6px rgba(24, 144, 255, 0.35)",
                                                }}
                                            >
                                                {sequenceNumber}
                                            </div>
                                        }
                                        title={
                                            <Space wrap>
                                                <Text strong>{stop.officeName}</Text>
                                                <Tag color="blue">{stop.orderCount} đơn</Tag>
                                                <Tag color="default">{translateStatus(stop.status)}</Tag>
                                            </Space>
                                        }
                                        description={
                                            <Space direction="vertical" size={4}>
                                                {stop.officeAddress && (
                                                    <Text type="secondary">
                                                        <EnvironmentOutlined /> {stop.officeAddress}
                                                    </Text>
                                                )}
                                                {stop.orders && stop.orders.length > 0 && (
                                                    <Text type="secondary" style={{ fontSize: 12 }}>
                                                        Mã đơn: {stop.orders.map((o) => o.trackingNumber).join(", ")}
                                                    </Text>
                                                )}
                                            </Space>
                                        }
                                    />
                                </List.Item>
                            );
                        }}
                    />
                )}
            </Card>
        </div>
    );
};

export default DriverRoute;
