import React, {useEffect, useRef, useState} from "react";
import {Alert, Button, Card, message, Space, Spin, Typography, Upload} from "antd";
import {CameraOutlined, FileImageOutlined, ScanOutlined, StopOutlined} from "@ant-design/icons";
import {Html5Qrcode} from "html5-qrcode";
import orderApi from "../../../api/orderApi";
import shipmentApi from "../../../api/shipmentApi";

const { Title, Text } = Typography;

const BarcodeScannerPage: React.FC = () => {
  const [scanning, setScanning] = useState(false);
  const [scannedCode, setScannedCode] = useState<string | null>(null);
  const [videoReady, setVideoReady] = useState(false);
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);

  // Ref-based guard chống double-scan (state sẽ chậm 1 tick khiến callback lặp lại cùng mã có thể bypass)
  const processingRef = useRef(false);
  // Cache lệnh cooldown theo trackingNumber (debounce 5s) — ngăn cùng 1 mã scan lại liên tiếp
  const recentScansRef = useRef<Map<string, number>>(new Map());
  const SCAN_DEBOUNCE_MS = 5000;

  // Tìm (orderId, shipmentOrderId) trong các shipment active của shipper hiện tại
  // theo trackingNumber. shipmentOrderId dùng cho endpoint scan DELIVERY mới.
  const resolveShipmentOrderForTracking = async (
    trackingNumber: string
  ): Promise<{ shipmentId: number; orderId: number; stopType: string } | null> => {
    try {
      const res: any = await shipmentApi.listShipperActiveShipments();
      const shipments = res?.data?.data || res?.data || [];
      for (const sh of shipments) {
        // Chỉ chấp nhận shipment DELIVERY đang PENDING hoặc IN_TRANSIT — đây là
        // các chuyến mà shipper có thể đang quét QR lên xe (PENDING) hoặc đang
        // giao (IN_TRANSIT). Đơn thuộc chuyến đã kết thúc không resolve.
        const shStatus = sh?.status;
        if (shStatus !== "PENDING" && shStatus !== "IN_TRANSIT") continue;
        const shType = sh?.type;
        // Chuyến DELIVERY là target chính; chuyến PICKUP vẫn resolve được (luồng
        // markPickedUp cũ). Các loại khác bỏ qua.
        if (shType !== undefined && shType !== null
            && shType !== "DELIVERY" && shType !== "PICKUP") continue;

        const orders = sh?.orders || sh?.shipmentOrders || [];
        for (const so of orders) {
          const tn =
            so?.trackingNumber ||
            so?.order?.trackingNumber ||
            null;
          if (!tn || tn.toLowerCase() !== trackingNumber.toLowerCase()) continue;

          const stopType = so?.stopType || so?.order?.stopType || "DELIVERY";
          // DTO mới trả shipmentId + orderId phẳng. Fallback nested cho tương thích.
          const shipmentId = so?.shipmentId ?? sh?.id ?? so?.id?.shipmentId ?? null;
          const orderId = so?.orderId ?? so?.order?.id ?? so?.id?.orderId ?? null;
          if (shipmentId && orderId) {
            return { shipmentId, orderId, stopType };
          }
        }
      }
    } catch (err) {
      console.warn("[SCAN] resolveShipmentOrder failed", err);
    }
    return null;
  };

  const isCodeOnCooldown = (code: string): boolean => {
    const now = Date.now();
    const lastTs = recentScansRef.current.get(code);
    if (lastTs && now - lastTs < SCAN_DEBOUNCE_MS) {
      return true;
    }
    recentScansRef.current.set(code, now);
    // Dọn rác map cache
    if (recentScansRef.current.size > 200) {
      const cutoff = now - SCAN_DEBOUNCE_MS;
      for (const [k, ts] of recentScansRef.current.entries()) {
        if (ts < cutoff) recentScansRef.current.delete(k);
      }
    }
    return false;
  };

  useEffect(() => {
    return () => {
      if (scannerRef.current) {
        scannerRef.current.stop().catch(() => undefined);
      }
    };
  }, []);

  const handleStartCamera = async () => {
    try {
      setCameraError(null);
      setVideoReady(false);
      // Clear state success cũ trước khi bắt đầu session scan mới
      setScannedCode(null);

      const scanner = new Html5Qrcode("qr-reader");
      scannerRef.current = scanner;

      await scanner.start(
        {facingMode: "environment"},
        {fps: 10, qrbox: {width: 300, height: 150}},
        (decodedText) => {
          handleScanSuccess(decodedText).catch(() => undefined);
        },
        () => {
          /* ignore scan failure */
        }
      );

      const container = document.getElementById("qr-reader");
      if (container) {
        const checkRendered = () => {
          const video = container.querySelector("video") as HTMLVideoElement | null;
          const canvas = container.querySelector("canvas") as HTMLCanvasElement | null;
          if (video) {
            if (video.readyState >= 2) {
              setVideoReady(true);
              return true;
            }
            const onLoaded = () => {
              setVideoReady(true);
              video.removeEventListener("loadeddata", onLoaded);
            };
            video.addEventListener("loadeddata", onLoaded);
            return true;
          }
          if (canvas) {
            setVideoReady(true);
            return true;
          }
          return false;
        };

        if (!checkRendered()) {
          const mo = new MutationObserver(() => {
            if (checkRendered()) {
              mo.disconnect();
            }
          });
          mo.observe(container, {childList: true, subtree: true});
        }
      }
      setScanning(true);
      message.info("Camera đã được bật. Hãy quét mã vạch.");
    } catch (error: any) {
      setCameraError(
        "Không thể truy cập camera. Vui lòng kiểm tra quyền truy cập camera hoặc sử dụng chức năng tải ảnh lên."
      );
      message.error("Không thể khởi động camera");
    }
  };

  const handleStopCamera = async () => {
    if (scannerRef.current) {
      try {
        await scannerRef.current.stop();
        scannerRef.current = null;
        setScanning(false);
        setVideoReady(false);
        message.info("Camera đã được tắt");
      } catch {
        /* ignore stop error */
      }
    }
  };

  const getStatusText = (status: string): string => {
    const statusMap: Record<string, string> = {
      PENDING: "Chờ xử lý",
      READY_FOR_PICKUP: "Sẵn sàng lấy hàng",
      PICKED_UP: "Đã lấy hàng",
      IN_TRANSIT: "Đang vận chuyển",
      DELIVERING: "Đang giao hàng",
      DELIVERED: "Đã giao hàng",
      FAILED_DELIVERY: "Giao hàng thất bại",
      RETURNED: "Đã hoàn trả",
      RETURNING: "Đang hoàn trả",
      RETURN_READY_FOR_PICKUP: "Sẵn sàng lấy hàng hoàn",
      RETURN_PICKED_UP: "Đã lấy hàng hoàn lên xe",
      RETURN_AT_ORIGIN_OFFICE: "Đã hoàn về bưu cục gốc",
      RETURN_RETRY: "Hoàn lại",
      RETURN_FAILED_FINAL: "Hoàn thất bại",
      CANCELLED: "Đã hủy",
    };
    return statusMap[status] || status;
  };

  const handleScanSuccess = async (trackingNumber: string) => {
    const code = (trackingNumber || "").trim();
    if (!code) return;

    // 1. Guard đồng bộ bằng ref — chặn callback camera fps lặp liên tục / StrictMode
    if (processingRef.current) {
      console.warn("[SCAN] Đang xử lý mã khác, bỏ qua:", code);
      return;
    }

    // 2. Debounce theo trackingNumber — tránh scan cùng 1 mã nhiều lần trong 5s
    if (isCodeOnCooldown(code)) {
      console.warn("[SCAN] Mã này vừa được xử lý, bỏ qua:", code);
      return;
    }

    processingRef.current = true;
    // QUAN TRỌNG: KHÔNG setScannedCode ở đây. Chỉ set khi API thành công.
    // Clear success cũ khi bắt đầu xử lý mã mới
    setScannedCode(null);

    // Dừng camera ngay khi nhận diện được mã để tránh callback lặp
    if (scannerRef.current && scanning) {
      await handleStopCamera();
    }

    try {
      // Resolve shipmentOrder theo tracking trong danh sách shipment active
      // của shipper hiện tại. Bắt buộc phải resolve được — không fallback
      // sang API cũ.
      const resolved = await resolveShipmentOrderForTracking(code);
      if (!resolved) {
        message.error(
          `Không tìm thấy đơn ${code} trong chuyến giao đang chờ bắt đầu của bạn.`
        );
        setScannedCode(null);
        return;
      }

      if (resolved.stopType === "DELIVERY") {
        // Luồng giao từ bưu cục đích: endpoint scan mới.
        // URL dùng composite key (shipmentId, orderId) — body chỉ có trackingNumber optional.
        const scanResponse: any = await orderApi.scanDeliveryShipmentOrder(
          resolved.shipmentId,
          resolved.orderId,
          code
        );
        setScannedCode(code);
        const apiMessage =
          scanResponse?.message ?? `Đã xác nhận hàng lên xe cho mã ${code}`;
        message.success(apiMessage);
      } else {
        // Luồng PICKUP / RETURN_TO_OFFICE: giữ flow cũ markPickedUp.
        const pickedUpResponse = await orderApi.markShipperPickedUpByTrackingNumber(code);

        setScannedCode(code);
        const apiMessage =
          pickedUpResponse?.message ?? `Đã cập nhật đơn hàng ${code} sang trạng thái "Đã lấy hàng"`;
        message.success(apiMessage);
      }

      // Tự động ẩn success box sau 3s
      setTimeout(() => {
        setScannedCode(null);
      }, 3000);
    } catch (error: any) {
      const errorMessage =
        error?.response?.data?.message || error?.message || "Không thể cập nhật trạng thái đơn hàng";
      message.error(errorMessage);
      // Clear success cũ nếu có
      setScannedCode(null);
    } finally {
      // Reset guard SAU CÙNG để lần scan sau có thể vào
      processingRef.current = false;
    }
  };

  const handleFileUpload = async (file: File) => {
    try {
      setCameraError(null);
      const scanner = new Html5Qrcode("qr-reader-file");
      const result = await scanner.scanFile(file, false);
      if (result) {
        await handleScanSuccess(result);
      } else {
        message.error("Không thể đọc mã vạch từ ảnh. Vui lòng thử ảnh khác.");
      }
    } catch {
      message.error("Không thể đọc mã vạch từ ảnh. Vui lòng thử ảnh khác.");
    }
    return false;
  };

  const isProcessing = processingRef.current; // chỉ để disable UI; logic guard dùng ref

  return (
    <div style={{padding: 24, background: "#F9FAFB", minHeight: "100vh"}}>
      <Card style={{maxWidth: 800, margin: "0 auto"}}>
        <Space direction="vertical" size="large" style={{width: "100%"}}>
          <div style={{textAlign: "center"}}>
            <ScanOutlined style={{fontSize: 48, color: "#1890ff", marginBottom: 16}}/>
            <Title level={2}>Quét mã vận đơn</Title>
            <Text type="secondary">
              Quét mã vạch trên phiếu vận đơn để xác nhận lấy hàng (đơn giao) hoặc xác nhận đơn hoàn đã lên xe (đơn hoàn về bưu cục gốc)
            </Text>
          </div>

          {cameraError && (
            <Alert
              message="Lỗi camera"
              description={cameraError}
              type="warning"
              showIcon
              closable
              onClose={() => setCameraError(null)}
            />
          )}

          {scannedCode && (
            <Alert
              message="Đã quét thành công"
              description={`Mã vận đơn: ${scannedCode}`}
              type="success"
              showIcon
              closable
              onClose={() => setScannedCode(null)}
            />
          )}

          <div style={{textAlign: "center"}}>
            <Space direction="vertical" size="middle" style={{width: "100%"}}>
              <Space>
                {!scanning ? (
                  <Button
                    type="primary"
                    className="primary-button"
                    size="large"
                    icon={<CameraOutlined/>}
                    onClick={handleStartCamera}
                  >
                    Bật Camera
                  </Button>
                ) : (
                  <Button
                    danger
                    size="large"
                    icon={<StopOutlined/>}
                    onClick={handleStopCamera}
                  >
                    Tắt Camera
                  </Button>
                )}
                <Upload
                  accept="image/*"
                  showUploadList={false}
                  beforeUpload={handleFileUpload}
                  disabled={scanning}
                >
                  <Button
                    size="large"
                    className="filter-button"
                    icon={<FileImageOutlined/>}
                    disabled={scanning}
                  >
                    Chọn ảnh từ thư mục
                  </Button>
                </Upload>
              </Space>
              {processingRef.current && (
                <div>
                  <Spin size="large"/>
                  <div style={{marginTop: 8}}>
                    <Text>Đang xử lý...</Text>
                  </div>
                </div>
              )}
            </Space>
          </div>

          <div
            style={{
              position: "relative",
              width: "100%",
              height: scanning ? 360 : 0,
              display: scanning ? "block" : "none",
              borderRadius: 8,
              overflow: "hidden",
              background: videoReady ? undefined : "#000",
            }}
          >
            <div id="qr-reader" style={{width: "100%", height: "100%"}}/>
            {!videoReady && (
              <div
                style={{
                  position: "absolute",
                  inset: 0,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "#fff",
                  background: "rgba(0,0,0,0.45)",
                  zIndex: 5,
                }}
              >
                <div style={{textAlign: "center"}}>
                  <Spin size="large"/>
                  <div style={{marginTop: 8}}>Đang kết nối camera...</div>
                </div>
              </div>
            )}
            <div
              aria-hidden
              style={{
                position: "absolute",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                width: "60%",
                maxWidth: 460,
                height: 140,
                border: "3px dashed rgba(255,255,255,0.9)",
                borderRadius: 8,
                boxShadow: "0 0 0 9999px rgba(0,0,0,0.35)",
                pointerEvents: "none",
                zIndex: 4,
              }}
            />
            <style>{`#qr-reader video, #qr-reader canvas { width: 100% !important; height: 100% !important; object-fit: cover !important; background: black !important; }`}</style>
          </div>

          <div id="qr-reader-file" style={{display: "none"}}/>

          <div style={{marginTop: 24, padding: 16, background: "#f0f2f5", borderRadius: 8}}>
            <Title level={5}>Hướng dẫn sử dụng:</Title>
            <ul style={{marginTop: 8, paddingLeft: 20}}>
              <li>
                Nhấn <strong>"Bật Camera"</strong> để quét mã vạch trực tiếp từ camera
              </li>
              <li>
                Hoặc nhấn <strong>"Chọn ảnh từ thư mục"</strong> để tải lên ảnh chứa mã vạch
              </li>
              <li>Hướng mã vạch vào khung hình (nếu dùng camera)</li>
              <li>Hệ thống sẽ tự động cập nhật trạng thái đơn hàng sau khi quét thành công</li>
              <li>
                Đơn giao ở trạng thái <strong>"Sẵn sàng lấy hàng"</strong> sẽ chuyển sang <strong>"Đã lấy hàng"</strong>
              </li>
              <li>
                Đơn hoàn ở trạng thái <strong>"Sẵn sàng lấy hàng hoàn"</strong> sẽ chuyển sang <strong>"Đã lấy hàng hoàn lên xe"</strong>
              </li>
              <li>
                Sau khi quét thành công, vui lòng đợi vài giây trước khi quét mã tiếp theo
              </li>
            </ul>
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default BarcodeScannerPage;