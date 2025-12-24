import React, { useState, useRef, useEffect } from "react";
import { Button, Card, message, Typography, Space, Upload, Alert, Spin } from "antd";
import { CameraOutlined, FileImageOutlined, StopOutlined, ScanOutlined } from "@ant-design/icons";
import { Html5Qrcode } from "html5-qrcode";
import orderApi from "../../api/orderApi";
import type { UploadFile } from "antd";

const { Title, Text } = Typography;

const BarcodeScanner: React.FC = () => {
  const [scanning, setScanning] = useState(false);
  const [scannedCode, setScannedCode] = useState<string | null>(null);
  const [processing, setProcessing] = useState(false);
  const [videoReady, setVideoReady] = useState(false);
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);

  useEffect(() => {
    return () => {
      if (scannerRef.current) {
        scannerRef.current.stop().catch(console.error);
      }
    };
  }, []);

  const handleStartCamera = async () => {
    try {
      setCameraError(null);
      setVideoReady(false);
      const scanner = new Html5Qrcode("qr-reader");
      scannerRef.current = scanner;

      await scanner.start(
        { facingMode: "environment" },
        {
          fps: 10,
          qrbox: { width: 300, height: 150 },
        },
        (decodedText) => {
          handleScanSuccess(decodedText);
        },
        () => {
          // Bỏ qua khi quét thất bại
        }
      );
      // Phát hiện khi html5-qrcode chèn phần tử video/canvas và khi khung hình đầu tiên sẵn sàng
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
          mo.observe(container, { childList: true, subtree: true });
        }
      }
      setScanning(true);
      message.info("Camera đã được bật. Hãy quét mã vạch.");
    } catch (error: any) {
      console.error("Camera error:", error);
      setCameraError("Không thể truy cập camera. Vui lòng kiểm tra quyền truy cập camera hoặc sử dụng chức năng tải ảnh lên.");
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
      } catch (error) {
        console.error("Stop camera error:", error);
      }
    }
  };

  const handleScanSuccess = async (trackingNumber: string) => {
    if (processing) return;

    setProcessing(true);
    setScannedCode(trackingNumber);

    // Dừng camera ngay khi quét thành công
    if (scannerRef.current && scanning) {
      await handleStopCamera();
    }

    try {
      // Lấy đơn hàng theo mã vận đơn (tracking number)
      const orderResponse = await orderApi.getShipperOrders({ 
        search: trackingNumber,
        page: 1,
        limit: 1 
      });

      if (!orderResponse.orders || orderResponse.orders.length === 0) {
        message.error(`Không tìm thấy đơn hàng với mã vận đơn: ${trackingNumber}`);
        setProcessing(false);
        return;
      }

      const order = orderResponse.orders[0];

      // Kiểm tra xem trạng thái đơn có phải là READY_FOR_PICKUP hay không
      if (order.status !== "READY_FOR_PICKUP") {
        message.warning(`Đơn hàng ${trackingNumber} không ở trạng thái "Sẵn sàng lấy hàng". Trạng thái hiện tại: ${getStatusText(order.status)}`);
        setProcessing(false);
        return;
      }

      // Cập nhật trạng thái sang PICKED_UP
      await orderApi.updateShipperDeliveryStatus(order.id, { status: "PICKED_UP" });
      message.success(`Đã cập nhật đơn hàng ${trackingNumber} sang trạng thái "Đã lấy hàng"`);
      
      // Đặt lại sau 2 giây
      setTimeout(() => {
        setScannedCode(null);
        setProcessing(false);
      }, 2000);

    } catch (error: any) {
      console.error("Update error:", error);
      message.error(error.message || "Không thể cập nhật trạng thái đơn hàng");
      setProcessing(false);
    }
  };

  const handleFileUpload = async (file: File) => {
    try {
      setProcessing(true);
      setCameraError(null);

      const scanner = new Html5Qrcode("qr-reader-file");
      const result = await scanner.scanFile(file, false);
      
      if (result) {
        await handleScanSuccess(result);
      } else {
        message.error("Không thể đọc mã vạch từ ảnh. Vui lòng thử ảnh khác.");
        setProcessing(false);
      }
    } catch (error: any) {
      console.error("File scan error:", error);
      message.error("Không thể đọc mã vạch từ ảnh. Vui lòng thử ảnh khác.");
      setProcessing(false);
    }
    return false; // Ngăn upload (không gửi file lên server qua component Upload)
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
      CANCELLED: "Đã hủy",
    };
    return statusMap[status] || status;
  };

  return (
    <div style={{ padding: 24, background: "#F9FAFB", minHeight: "100vh" }}>
      <Card style={{ maxWidth: 800, margin: "0 auto" }}>
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <div style={{ textAlign: "center" }}>
            <ScanOutlined style={{ fontSize: 48, color: "#1890ff", marginBottom: 16 }} />
            <Title level={2}>Quét mã vận đơn</Title>
            <Text type="secondary">
              Quét mã vạch trên phiếu vận đơn để tự động cập nhật trạng thái "Đã lấy hàng"
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
            />
          )}

          <div style={{ textAlign: "center" }}>
            <Space direction="vertical" size="middle" style={{ width: "100%" }}>
              <Space>
                {!scanning ? (
                  <Button
                    type="primary"
                    size="large"
                    icon={<CameraOutlined />}
                    onClick={handleStartCamera}
                    disabled={processing}
                  >
                    Bật Camera
                  </Button>
                ) : (
                  <Button
                    danger
                    size="large"
                    icon={<StopOutlined />}
                    onClick={handleStopCamera}
                  >
                    Tắt Camera
                  </Button>
                )}

                <Upload
                  accept="image/*"
                  showUploadList={false}
                  beforeUpload={handleFileUpload}
                  disabled={processing || scanning}
                >
                  <Button
                    size="large"
                    icon={<FileImageOutlined />}
                    disabled={processing || scanning}
                  >
                    Chọn ảnh từ thư mục
                  </Button>
                </Upload>
              </Space>

              {processing && (
                <div>
                  <Spin size="large" />
                  <div style={{ marginTop: 8 }}>
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
            <div id="qr-reader" style={{ width: "100%", height: "100%" }} />

            {/* Loading overlay shown until video/canvas ready */}
            {!videoReady && (
              <div style={{
                position: "absolute",
                inset: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#fff",
                background: "rgba(0,0,0,0.45)",
                zIndex: 5,
              }}>
                <div style={{ textAlign: 'center' }}>
                  <Spin size="large" />
                  <div style={{ marginTop: 8 }}>Đang kết nối camera...</div>
                </div>
              </div>
            )}

            {/* Overlay frame to help user aim the barcode */}
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

            {/* Ensure video/canvas fills container */}
            <style>{`#qr-reader video, #qr-reader canvas { width: 100% !important; height: 100% !important; object-fit: cover !important; background: black !important; }`}</style>
          </div>

          <div id="qr-reader-file" style={{ display: "none" }} />

          <div style={{ marginTop: 24, padding: 16, background: "#f0f2f5", borderRadius: 8 }}>
            <Title level={5}>Hướng dẫn sử dụng:</Title>
            <ul style={{ marginTop: 8, paddingLeft: 20 }}>
              <li>Nhấn <strong>"Bật Camera"</strong> để quét mã vạch trực tiếp từ camera</li>
              <li>Hoặc nhấn <strong>"Chọn ảnh từ thư mục"</strong> để tải lên ảnh chứa mã vạch</li>
              <li>Hướng mã vạch vào khung hình (nếu dùng camera)</li>
              <li>Hệ thống sẽ tự động cập nhật trạng thái đơn hàng sau khi quét thành công</li>
              <li>Chỉ các đơn hàng ở trạng thái <strong>"Sẵn sàng lấy hàng"</strong> mới được cập nhật</li>
            </ul>
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default BarcodeScanner;
