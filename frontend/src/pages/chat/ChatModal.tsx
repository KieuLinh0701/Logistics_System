import { CloseOutlined, CustomerServiceOutlined } from "@ant-design/icons";
import { Button, Typography } from "antd";
import MessageInput from "./MessageInput";
import MessageList from "./MessageList";
import type { SupportMessage } from "../../types/support";

const { Text } = Typography;

type Props = {
  open: boolean;
  messages: SupportMessage[];
  currentAccountId: number;
  sending: boolean;
  isMobile: boolean;
  onClose: () => void;
  onSend: (content: string) => Promise<void>;
};

const ChatModal: React.FC<Props> = ({
  open,
  messages,
  currentAccountId,
  sending,
  isMobile,
  onClose,
  onSend,
}) => {
  if (!open) {
    return null;
  }

  return (
    <div
      style={{
        position: "fixed",
        right: isMobile ? 0 : 20,
        bottom: isMobile ? 0 : 20,
        width: isMobile ? "100vw" : 380,
        height: isMobile ? "100vh" : 560,
        borderRadius: isMobile ? 0 : 14,
        overflow: "hidden",
        background: "#fff",
        boxShadow: "0 18px 46px rgba(0,0,0,0.22)",
        zIndex: 10001,
        display: "grid",
        gridTemplateRows: "56px 1fr auto",
        animation: "supportChatEnter 0.16s ease-out",
      }}
    >
      <style>
        {`@keyframes supportChatEnter {
          from { opacity: 0; transform: translateY(12px) scale(0.98); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }`}
      </style>

      <div
        style={{
          background: "linear-gradient(135deg, #0284c7, #0369a1)",
          color: "white",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "0 12px",
          gap: 8,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <CustomerServiceOutlined style={{ color: "white", fontSize: 18 }} />
          <Text style={{ color: "white", fontWeight: 600 }}>Hỗ trợ khách hàng</Text>
        </div>
        <Button
          type="text"
          icon={<CloseOutlined />}
          onClick={onClose}
          style={{ color: "white" }}
        />
      </div>

      <MessageList messages={messages} currentAccountId={currentAccountId} />

      <MessageInput onSend={onSend} sending={sending} />
    </div>
  );
};

export default ChatModal;
