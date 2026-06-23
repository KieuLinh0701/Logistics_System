import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {
    Alert,
    Button,
    Card,
    Col,
    Divider,
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
    EyeOutlined,
    NodeIndexOutlined,
    PauseCircleOutlined,
    PhoneOutlined,
    PlayCircleOutlined,
} from "@ant-design/icons";
import {GoogleMap, LoadScript, MarkerF, PolylineF} from "@react-google-maps/api";
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
  encodedPolyline?: string;
  planCode?: string;
  fuelCost?: number;
}

interface DeliveryStop {
  id: number;
  trackingNumber: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  codAmount: number;
  priority: string;
  serviceType: string;
  status: string;
  stopSequence?: number;
  etaTime?: string;
  latitude?: number;
  longitude?: number;
}


const isFinalDeliveryStatus = (status?: string) => {
  const st = (status || "").toString().toUpperCase();
  return st === "DELIVERED" || st === "FAILED_DELIVERY" || st === "COMPLETED" || st === "FAILED";
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
  const [deliveryStops, setDeliveryStops] = useState<DeliveryStop[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedStop, setSelectedStop] = useState<DeliveryStop | null>(null);
  const [detailModal, setDetailModal] = useState(false);
  const [currentPosition, setCurrentPosition] = useState<google.maps.LatLngLiteral | null>(null);
  const [realtimeDirections, setRealtimeDirections] = useState<google.maps.DirectionsResult | null>(null);
  const [isMapLoaded, setIsMapLoaded] = useState(false);
  const [directionsLoading, setDirectionsLoading] = useState(false);
  const [directionsRenderKey, setDirectionsRenderKey] = useState(0);

  const mapRef = useRef<google.maps.Map | null>(null);
  const mapCardRef = useRef<HTMLDivElement | null>(null);
  const lastNextStopIdRef = useRef<number | null>(null);
  const directionsRequestSeqRef = useRef(0);
  const lastDirectionsQueryRef = useRef<{ stopId: number; origin: google.maps.LatLngLiteral } | null>(null);
  const directionsCacheRef = useRef<Map<string, google.maps.DirectionsResult>>(new Map());

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
      setRouteInfo(routeData.routeInfo);
      const routeStops = (routeData.deliveryStops || []) as DeliveryStop[];
      const filteredRouteStops = routeStops.filter((s) => !isFinalDeliveryStatus(s.status));

      try {
        const ordersRes = await orderApi.getShipperOrders({ page: 1, limit: 200 });
        const shipperOrders = (ordersRes.orders || []) as any[];

        const visibleOrders = (shipperOrders || []).filter((o: any) => !isFinalDeliveryStatus(o?.status));

        if (visibleOrders.length > 0) {
          const routeByTracking = new Map(filteredRouteStops.map((s) => [s.trackingNumber, s]));
          const synced = visibleOrders
            .map((o: any) => {
              const tracking = o.trackingNumber;
              if (routeByTracking.has(tracking)) return routeByTracking.get(tracking);
              return {
                id: o.id,
                trackingNumber: o.trackingNumber,
                recipientName: o.recipientName,
                recipientPhone: o.recipientPhone,
                recipientAddress:
                  typeof o.recipientAddress === "string"
                    ? o.recipientAddress
                    : (o.recipientAddress?.fullAddress ?? ""),
                codAmount: o.cod || 0,
                priority: o.priority || "normal",
                serviceType: o.serviceType?.name || o.serviceType || "",
                status: o.status || "READY_FOR_PICKUP",
                latitude: o.latitude ?? o.recipientLatitude,
                longitude: o.longitude ?? o.recipientLongitude,
              } as DeliveryStop;
            })
            .filter((s): s is DeliveryStop => !!s && !isFinalDeliveryStatus(s.status));

          setDeliveryStops(sortByStopSequence(synced));
        } else {
          setDeliveryStops(sortByStopSequence(filteredRouteStops));
        }
      } catch (err) {
        console.warn("Could not fetch shipper orders for comparison", err);
        setDeliveryStops(sortByStopSequence(filteredRouteStops));
      }
    } catch (error) {
      console.error("Error fetching route data:", error);
      message.error("Lỗi khi tải dữ liệu lộ trình");
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
        console.error("Không lấy được GPS shipper", error);
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
      if (!isMapLoaded || typeof google === "undefined") {
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
    [currentPosition, directionsLoading, isMapLoaded]
  );

  const handleStartRoute = async () => {
    if (!routeInfo) return;

    Modal.confirm({
      title: "Bắt đầu tuyến giao hàng",
      content: "Bạn có chắc chắn muốn bắt đầu tuyến giao hàng này?",
      onOk: async () => {
        try {
          await orderApi.startShipperRoute(routeInfo.id);
          setRouteInfo((prev) => (prev ? { ...prev, status: "in_progress" } : null));
          message.success("Đã bắt đầu tuyến giao hàng");
          mapCardRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
        } catch {
          message.error("Không thể bắt đầu tuyến giao hàng");
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
    message.info(`Đã đưa bản đồ đến ${stop.recipientName}`);
  };

  const handleViewStopDetail = (stop: DeliveryStop) => {
    setSelectedStop(stop);
    setDetailModal(true);
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
      case "PARTIAL_DELIVERY":
        return "Giao 1 phần";
      case "PARTIAL_RETURN":
        return "Trả 1 phần";
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
        <Alert message="Không có lộ trình giao hàng hôm nay" type="info" showIcon />
      </div>
    );
  }

  const completionRate =
    routeInfo.totalStops > 0 ? (routeInfo.completedStops / routeInfo.totalStops) * 100 : 0;

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Title level={2} style={{ color: "#1C3D90", marginBottom: 24 }}>
        Lộ trình giao hàng {routeInfo.source === "AI" && <Tag color="blue">Đã tối ưu</Tag>}
      </Title>

      <Card style={{ marginBottom: 24 }}>
        <Row gutter={16}>
          <Col xs={24} sm={12} lg={6}>AI đã tối ưu
            <Statistic title="Tổng điểm dừng" value={routeInfo.totalStops} prefix={<NodeIndexOutlined />} />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Statistic title="Đã hoàn thành" value={routeInfo.completedStops} prefix={<CheckCircleOutlined />} />
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
            <Statistic title="Thời gian ước tính" value={routeInfo.estimatedDuration} suffix="phút" />
          </Col>
        </Row>

        <Divider />

        <Progress percent={Math.round(completionRate)} status="active" />

        {nextStop && (
          <Alert
            style={{ marginTop: 16 }}
            type="info"
            showIcon
            message={
              <span>
                Điểm giao tiếp theo #{nextStop.stopSequence}:{" "}
                <strong>{nextStop.trackingNumber}</strong> — {nextStop.recipientName}
              </span>
            }
          />
        )}

        <Space style={{ marginTop: 16 }}>
          {routeInfo.status === "not_started" && (
            <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleStartRoute}>
              Bắt đầu tuyến
            </Button>
          )}
          {routeInfo.status === "in_progress" && (
            <>
              <Button icon={<PauseCircleOutlined />}>Tạm dừng</Button>
              <Button type="primary" icon={<CompassOutlined />} onClick={() => navigate("/shipper/orders")}>
                Xem đơn hàng
              </Button>
              <Button
                icon={<EnvironmentOutlined />}
                onClick={() => mapCardRef.current?.scrollIntoView({ behavior: "smooth", block: "start" })}
              >
                Xem bản đồ tuyến
              </Button>
            </>
          )}
        </Space>
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
              disabled={!isMapLoaded || !currentPosition || !nextStop || !hasValidCoords(nextStop)}
              onClick={() => {
                if (!nextStop) return;
                requestDirectionsToStop(nextStop);
              }}
            >
              Chỉ đường tới điểm tiếp theo
            </Button>
            <Button
              size="small"
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
          {!isMapLoaded && (
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
          <LoadScript
            googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_KEY as string}
            onLoad={() => setIsMapLoaded(true)}
          >
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
              {isMapLoaded && (
                <>
                  {currentPosition && (
                    <MarkerF
                      position={currentPosition}
                      icon={{
                        path: google.maps.SymbolPath.CIRCLE,
                        scale: 10,
                        fillColor: "#1890ff",
                        fillOpacity: 1,
                        strokeColor: "#ffffff",
                        strokeWeight: 3,
                      }}
                      title="Vị trí hiện tại"
                      zIndex={10000}
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
                        title={isNext ? `Điểm tiếp theo: ${stop.recipientName}` : stop.recipientName}
                        zIndex={isNext ? 9999 : 1000}
                      />
                    );
                  })}
                </>
              )}
            </GoogleMap>
          </LoadScript>
        </div>
      </Card>

      <Card title="Danh sách điểm giao hàng (theo thứ tự AI)">
        <List
          dataSource={deliveryStops}
          renderItem={(stop) => {
            const isNext = nextStop?.id === stop.id;
            return (
              <List.Item
                actions={[
                  <Button
                    icon={<EnvironmentOutlined />}
                    onClick={() => {
                      requestDirectionsToStop(stop);
                      handleFocusStopOnMap(stop);
                    }}
                  >
                    Chỉ đường
                  </Button>,
                  <Button icon={<EyeOutlined />} onClick={() => handleViewStopDetail(stop)}>
                    Chi tiết
                  </Button>,
                  <Button type="link" onClick={() => navigate(`/orders/${stop.id}`)}>
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
                      <Tag color={getPriorityColor(stop.priority)}>{getPriorityText(stop.priority)}</Tag>
                      <Tag color={getStatusColor(stop.status)}>{getStatusText(stop.status)}</Tag>
                    </Space>
                  }
                  description={
                    <Space direction="vertical" size={4}>
                      <Text>
                        <PhoneOutlined /> {stop.recipientPhone} - {stop.recipientName}
                      </Text>
                      <Text type="secondary">
                        <EnvironmentOutlined /> {stop.recipientAddress}
                      </Text>
                      {stop.etaTime && <Text type="secondary">ETA: {stop.etaTime}</Text>}
                      {stop.codAmount > 0 && (
                        <Text>
                          <DollarOutlined /> COD thu hộ: {stop.codAmount.toLocaleString()}đ
                        </Text>
                      )}
                    </Space>
                  }
                />
              </List.Item>
            );
          }}
        />
      </Card>

      <Modal
        title="Chi tiết điểm giao hàng"
        open={detailModal}
        onCancel={() => {
          setDetailModal(false);
          setSelectedStop(null);
        }}
        footer={null}
        width={600}
      >
        {selectedStop && (
          <Space direction="vertical" size="large" style={{ width: "100%" }}>
            <div>
              <Text strong>Mã đơn hàng: </Text>
              <Text>{selectedStop.trackingNumber}</Text>
            </div>
            <div>
              <Text strong>Người nhận: </Text>
              <Text>{selectedStop.recipientName}</Text>
            </div>
            <div>
              <Text strong>SĐT: </Text>
              <Text>{selectedStop.recipientPhone}</Text>
            </div>
            <div>
              <Text strong>Địa chỉ: </Text>
              <Text>{selectedStop.recipientAddress}</Text>
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
              <Tag color={getStatusColor(selectedStop.status)}>{getStatusText(selectedStop.status)}</Tag>
            </div>
            <Button
              type="primary"
              block
              icon={<EnvironmentOutlined />}
              onClick={() => handleFocusStopOnMap(selectedStop)}
            >
              Xem trên bản đồ
            </Button>
          </Space>
        )}
      </Modal>
    </div>
  );
};

export default ShipperDeliveryRoute;
