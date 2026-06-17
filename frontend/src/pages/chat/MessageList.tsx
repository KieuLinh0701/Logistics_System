import {Avatar, Tag, Typography} from "antd";
import {UserOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import {useEffect, useRef} from "react";
import type {SupportMessage} from "../../types/support";

const { Text } = Typography;

type Props = {
  messages: SupportMessage[];
  currentAccountId: number;
};

const isBotMessage = (item: SupportMessage) => {
  return item.isBotMessage === true || item.senderType === "BOT";
};

const MessageList: React.FC<Props> = ({ messages, currentAccountId }) => {
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    el.scrollTop = el.scrollHeight;
  }, [messages]);

  return (
    <div ref={containerRef} className="chat-modal-message-list" style={{ height: "100%", overflowY: "auto", padding: 12, paddingBottom: 32, background: "#fff" }}>
      {messages.map((item) => {
        const bot = isBotMessage(item);
        const isMine = item.senderAccountId === currentAccountId;
        const isSystem = item.messageType === "SYSTEM" && !bot;

        if (isSystem) {
          return (
            <div key={item.id} style={{ textAlign: "center", marginBottom: 8 }}>
              <Tag color="default">{item.message}</Tag>
            </div>
          );
        }

        const bubbleBackground = bot
          ? "linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%)"
          : isMine
            ? "#e6f4ff"
            : "#f5f5f5";

        const borderColor = bot ? "#91caff" : "transparent";
        const label = bot ? (item.senderLabel || "Trợ lý logistics") : item.senderLabel || item.senderName;

        return (
          <div
            key={item.id}
            style={{
              display: "flex",
              justifyContent: isMine && !bot ? "flex-end" : "flex-start",
              marginBottom: 8,
              alignItems: "flex-end",
            }}
          >
            {(!isMine || bot) ? (
              <div style={{ marginRight: 8 }}>
                <Avatar
                  src={bot ? undefined : (item.senderImage || undefined)}
                  icon={bot ? <span style={{ fontSize: 14 }}>🤖</span> : <UserOutlined />}
                  style={bot ? { background: "#1677ff" } : undefined}
                />
              </div>
            ) : null}

            <div
              style={{
                maxWidth: "80%",
                background: bubbleBackground,
                border: bot ? "1px solid #91caff" : "none",
                borderColor,
                borderRadius: 12,
                padding: "8px 10px",
                boxShadow: bot ? "0 1px 6px rgba(22,119,255,0.08)" : undefined,
              }}
            >
              {bot ? (
                <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 4 }}>
                  <Tag color="processing" style={{ marginInlineEnd: 0 }}>
                    Trợ lý logistics
                  </Tag>
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    Bot
                  </Text>
                </div>
              ) : null}

              {!bot && label ? (
                <div style={{ marginBottom: 4 }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {label}
                  </Text>
                </div>
              ) : null}

              <Text style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>{item.message}</Text>
              <div style={{ marginTop: 4, textAlign: "right" }}>
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {dayjs(item.createdAt).format("DD/MM HH:mm")}
                </Text>
              </div>
            </div>

            {isMine && !bot ? (
              <div style={{ marginLeft: 8 }}>
                <Avatar src={item.senderImage || undefined} icon={<UserOutlined />} />
              </div>
            ) : null}
          </div>
        );
      })}
      <div style={{ height: 10, flexShrink: 0 }} />
      
    </div>
  );
};

export default MessageList;
