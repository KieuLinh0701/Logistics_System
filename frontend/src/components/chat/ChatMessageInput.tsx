import { Button, Input, Modal, Spin, message } from "antd";
import { SendOutlined, PictureOutlined, CameraOutlined } from "@ant-design/icons";
import { useEffect, useRef, useState } from "react";
import "./ChatMessageInput.css";

const { TextArea } = Input;

export interface ChatMessageInputProps {
  onSend: (content: string) => Promise<void>;
  onUploadImage?: (file: File) => Promise<void>;
  sending?: boolean;
  placeholder?: string;
  disabled?: boolean;
}

const ChatMessageInput: React.FC<ChatMessageInputProps> = ({
  onSend,
  onUploadImage,
  sending = false,
  placeholder = "Nhập tin nhắn...",
  disabled = false,
}) => {
  const [value, setValue] = useState("");
  const [uploadingImage, setUploadingImage] = useState(false);
  const [cameraModalOpen, setCameraModalOpen] = useState(false);
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (videoRef.current && cameraStream) {
      videoRef.current.srcObject = cameraStream;
    }
  }, [cameraStream]);

  const stopCameraStream = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((track) => track.stop());
      setCameraStream(null);
    }
  };

  const handleOpenCamera = async () => {
    if (disabled || uploadingImage) return;

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
      });
      setCameraStream(stream);
      setCameraModalOpen(true);
    } catch {
      message.error("Không thể mở camera. Vui lòng kiểm tra quyền truy cập camera.");
    }
  };

  const handleCloseCameraModal = () => {
    stopCameraStream();
    setCameraModalOpen(false);
  };

  const handleCapture = async () => {
    if (!videoRef.current || !canvasRef.current || !onUploadImage) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;

    if (video.videoWidth === 0 || video.videoHeight === 0) {
      message.error("Không thể chụp ảnh. Camera chưa sẵn sàng.");
      return;
    }

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.drawImage(video, 0, 0);

    canvas.toBlob(
      (blob) => {
        if (!blob) {
          message.error("Không thể chụp ảnh");
          return;
        }

        const file = new File([blob], `camera_${Date.now()}.jpg`, { type: "image/jpeg" });

        handleCloseCameraModal();

        setUploadingImage(true);
        onUploadImage(file)
          .catch(() => {
            message.error("Gửi ảnh thất bại");
          })
          .finally(() => {
            setUploadingImage(false);
          });
      },
      "image/jpeg",
      0.9
    );
  };

  const handleSend = async () => {
    const content = value.trim();
    if (!content || disabled) {
      return;
    }

    await onSend(content);
    setValue("");
  };

  const handleSelectImage = () => {
    if (disabled || uploadingImage) return;
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    e.target.value = "";

    console.log("[ChatMessageInput] File selected:", {
      name: file.name,
      type: file.type,
      size: file.size,
    });

    if (!onUploadImage) {
      message.error("Tính năng gửi ảnh chưa khả dụng");
      return;
    }

    const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
    if (!allowedTypes.includes(file.type)) {
      message.error("Chỉ chấp nhận file ảnh JPEG, PNG hoặc WebP");
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      message.error("Kích thước ảnh vượt quá 5MB");
      return;
    }

    setUploadingImage(true);
    try {
      await onUploadImage(file);
    } catch {
      message.error("Gửi ảnh thất bại");
    } finally {
      setUploadingImage(false);
    }
  };

  const isLoading = sending || uploadingImage;
  const isDisabled = disabled || isLoading;

  return (
    <>
      <div className="chat-message-input-wrapper">
        <div className="chat-message-input">
          {onUploadImage && (
            <>
              <button
                type="button"
                className="chat-media-btn"
                aria-label="Chọn ảnh"
                disabled={isDisabled}
                onClick={handleSelectImage}
              >
                <PictureOutlined />
              </button>

              <button
                type="button"
                className="chat-media-btn"
                aria-label="Chụp ảnh"
                disabled={isDisabled}
                onClick={handleOpenCamera}
              >
                <CameraOutlined />
              </button>

              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                style={{ display: "none" }}
                onChange={handleFileChange}
              />
            </>
          )}

          <TextArea
            className="chat-text-input"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            autoSize={{ minRows: 2, maxRows: 4 }}
            maxLength={4000}
            placeholder={placeholder}
            disabled={isDisabled}
            onPressEnter={(event) => {
              if (!event.shiftKey) {
                event.preventDefault();
                if (!isDisabled && value.trim()) {
                  void handleSend();
                }
              }
            }}
          />

          <Button
            type="primary"
            shape="circle"
            icon={isLoading ? <Spin size="small" /> : <SendOutlined />}
            loading={sending}
            disabled={isDisabled || !value.trim()}
            onClick={() => void handleSend()}
            className="chat-send-btn"
          />
        </div>
      </div>

      <Modal
        title="Chụp ảnh"
        open={cameraModalOpen}
        onCancel={handleCloseCameraModal}
        footer={null}
        width={420}
        centered
      >
        <div style={{ textAlign: "center" }}>
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            style={{
              width: "100%",
              maxHeight: 320,
              borderRadius: 8,
              background: "#000",
            }}
          />
          <canvas ref={canvasRef} style={{ display: "none" }} />

          <div style={{ marginTop: 16, display: "flex", gap: 12, justifyContent: "center" }}>
            <Button onClick={handleCloseCameraModal}>Hủy</Button>
            <Button type="primary" onClick={handleCapture}>
              Chụp ảnh
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
};

export default ChatMessageInput;
