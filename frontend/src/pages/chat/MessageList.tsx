import { Tag, Typography, Avatar } from "antd";
import { UserOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useEffect, useRef } from "react";
import type { SupportMessage } from "../../types/support";

const { Text } = Typography;

type Props = {
  messages: SupportMessage[];
  currentAccountId: number;
};

const MessageList: React.FC<Props> = ({ messages, currentAccountId }) => {
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    el.scrollTop = el.scrollHeight;
  }, [messages]);

  return (
    <div ref={containerRef} className="chat-modal-message-list" style={{ height: "100%", overflowY: "auto", padding: 12, background: "#fff" }}>
      {messages.map((item) => {
        const isMine = item.senderAccountId === currentAccountId;
        const isSystem = item.messageType === "SYSTEM";

        if (isSystem) {
          return (
            <div key={item.id} style={{ textAlign: "center", marginBottom: 8 }}>
              <Tag color="default">{item.message}</Tag>
            </div>
          );
        }

        return (
          <div
            key={item.id}
            style={{
              display: "flex",
              justifyContent: isMine ? "flex-end" : "flex-start",
              marginBottom: 8,
              alignItems: "flex-end",
            }}
          >
            {!isMine ? (
              <div style={{ marginRight: 8 }}>
                <Avatar src={item.senderImage || undefined} icon={<UserOutlined />} />
              </div>
            ) : null}

            <div
              style={{
                maxWidth: "80%",
                background: isMine ? "#e6f4ff" : "#f5f5f5",
                borderRadius: 10,
                padding: "8px 10px",
              }}
            >
              <Text>{item.message}</Text>
              <div style={{ marginTop: 4, textAlign: "right" }}>
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {dayjs(item.createdAt).format("DD/MM HH:mm")}
                </Text>
              </div>
            </div>

            {isMine ? (
              <div style={{ marginLeft: 8 }}>
                <Avatar src={item.senderImage || undefined} icon={<UserOutlined />} />
              </div>
            ) : null}
          </div>
        );
      })}
      
    </div>
  );
};

export default MessageList;
