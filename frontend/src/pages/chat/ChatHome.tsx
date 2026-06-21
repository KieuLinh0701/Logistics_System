import {useEffect, useRef, useState} from "react";
import {Button, Input, message} from "antd";
import {FileTextOutlined, RobotOutlined, SendOutlined,} from "@ant-design/icons";
import {chatStore} from "../../hooks/chatStore";
import supportApi from "../../api/supportApi";
import type {BotPreviewResponse, SupportTicket} from "../../types/support";

const { TextArea } = Input;

type Message = {
  id: string;
  type: "bot" | "user";
  content: string;
  actions?: { label: string; onClick: () => void }[];
};

type QuickReply = {
  key: string;
  label: string;
  icon: React.ReactNode;
};

type ChatHomeMode = "normal" | "create_ticket";

const quickReplies: QuickReply[] = [
  { key: "new_ticket", label: "Tạo yêu cầu hỗ trợ", icon: <FileTextOutlined /> },
  { key: "history", label: "Xem yêu cầu cũ", icon: <FileTextOutlined /> },
];

const greetingMessage: Message = {
  id: "greeting",
  type: "bot",
  content: "Xin chào! Bạn có thể nhập mã vận đơn, hỏi về COD hoặc tạo yêu cầu hỗ trợ.",
};

const ChatHome: React.FC = () => {
  const [mode, setMode] = useState<ChatHomeMode>("normal");
  const [messages, setMessages] = useState<Message[]>([greetingMessage]);
  const [inputValue, setInputValue] = useState("");
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<any>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, [mode]);

  const handleViewHistory = () => {
    chatStore.goTicketList();
  };

  const handleStartCreateTicket = () => {
    if (mode === "create_ticket") {
      return;
    }

    setMode("create_ticket");
    setInputValue("");

    const userMsg: Message = {
      id: `user-${Date.now()}`,
      type: "user",
      content: "Tạo yêu cầu hỗ trợ",
    };

    const botMsg: Message = {
      id: `bot-${Date.now()}`,
      type: "bot",
      content: "Bạn vui lòng mô tả vấn đề cần hỗ trợ, mình sẽ tạo ticket để CSKH tiếp nhận.",
      actions: [{ label: "Hủy", onClick: handleReset }],
    };

    setMessages((prev) => [...prev, userMsg, botMsg]);
  };

  const handleCreateTicket = async (content: string) => {
    if (!content.trim() || sending) return;
    if (mode !== "create_ticket") return;

    setSending(true);

    const userMsg: Message = {
      id: `user-${Date.now()}`,
      type: "user",
      content,
    };

    try {
      const res = await supportApi.createTicket({
        subject: content.substring(0, 80),
        initialMessage: content,
      });

      if (!res.success || !res.data) {
        message.error(res.message || "Không thể tạo yêu cầu hỗ trợ");
        setMessages((prev) => [...prev, userMsg]);
        setSending(false);
        return;
      }

      const ticket = res.data as SupportTicket;

      const botMsg: Message = {
        id: `bot-${Date.now()}`,
        type: "bot",
        content: `Đã tạo yêu cầu ${ticket.code}. CSKH sẽ tiếp nhận và phản hồi trong ticket.`,
        actions: [
          { label: "Xem chi tiết", onClick: () => handleViewTicket(ticket) },
          { label: "Xem yêu cầu cũ", onClick: handleViewHistory },
          { label: "Tiếp tục chat", onClick: handleReset },
        ],
      };

      setMessages((prev) => [...prev, userMsg, botMsg]);
      setInputValue("");
      setMode("normal");
    } catch {
      message.error("Không thể tạo yêu cầu hỗ trợ");
      setMessages((prev) => [...prev, userMsg]);
    } finally {
      setSending(false);
    }
  };

  const handleViewTicket = (ticket: SupportTicket) => {
    chatStore.selectTicket(ticket);
  };

  const handlePreviewBot = async (content: string) => {
    if (!content.trim() || sending) return;

    setSending(true);

    const userMsg: Message = {
      id: `user-${Date.now()}`,
      type: "user",
      content,
    };

    setMessages((prev) => [...prev, userMsg]);

    try {
      const res = await supportApi.previewBotMessage(content);

      if (!res.success || !res.data) {
        const fallbackMsg: Message = {
          id: `bot-${Date.now()}`,
          type: "bot",
          content: "Mình chưa xử lý được tin nhắn này. Bạn có thể tạo yêu cầu hỗ trợ để CSKH kiểm tra.",
          actions: [
            { label: "Tạo yêu cầu hỗ trợ", onClick: handleStartCreateTicket },
            { label: "Xem yêu cầu cũ", onClick: handleViewHistory },
          ],
        };
        setMessages((prev) => [...prev, fallbackMsg]);
        setInputValue("");
        setSending(false);
        return;
      }

      const preview = res.data as BotPreviewResponse;

      const actions: { label: string; onClick: () => void }[] = [];
      if (preview.suggestCreateTicket) {
        actions.push({ label: "Tạo yêu cầu hỗ trợ", onClick: handleStartCreateTicket });
      }
      if (preview.suggestViewTickets) {
        actions.push({ label: "Xem yêu cầu cũ", onClick: handleViewHistory });
      }
      // Không hiện "Quay lại" cho các câu trả lời bình thường

      const botMsg: Message = {
        id: `bot-${Date.now()}`,
        type: "bot",
        content: preview.reply,
        actions: actions.length > 0 ? actions : undefined,
      };

      setMessages((prev) => [...prev, botMsg]);
      setInputValue("");
    } catch {
      const fallbackMsg: Message = {
        id: `bot-${Date.now()}`,
        type: "bot",
        content: "Mình chưa xử lý được tin nhắn này. Bạn có thể tạo yêu cầu hỗ trợ để CSKH kiểm tra.",
        actions: [
          { label: "Tạo yêu cầu hỗ trợ", onClick: handleStartCreateTicket },
          { label: "Xem yêu cầu cũ", onClick: handleViewHistory },
        ],
      };
      setMessages((prev) => [...prev, fallbackMsg]);
      setInputValue("");
    } finally {
      setSending(false);
    }
  };

  const handleQuickReply = (key: string) => {
    if (key === "new_ticket") {
      handleStartCreateTicket();
    } else if (key === "history") {
      handleViewHistory();
    }
  };

  const handleSend = () => {
    const content = inputValue.trim();
    if (!content || sending) return;

    if (mode === "create_ticket") {
      void handleCreateTicket(content);
    } else {
      void handlePreviewBot(content);
    }
  };

  const handleReset = () => {
    setMode("normal");
    setMessages([greetingMessage]);
    setInputValue("");
  };

  const getPlaceholder = () => {
    if (mode === "create_ticket") {
      return "Mô tả vấn đề cần hỗ trợ...";
    }
    return "Nhập mã vận đơn hoặc câu hỏi cần hỗ trợ...";
  };

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        background: "#f5f7fa",
      }}
    >
      {/* Messages Area */}
      <div
        style={{
          flex: 1,
          overflowY: "auto",
          padding: "12px 12px 8px",
          display: "flex",
          flexDirection: "column",
          gap: 8,
        }}
      >
        {/* Bot greeting */}
        <div
          style={{
            display: "flex",
            alignItems: "flex-end",
            gap: 8,
            alignSelf: "flex-start",
            maxWidth: "85%",
          }}
        >
          <div
            style={{
              width: 28,
              height: 28,
              borderRadius: "50%",
              background: "#0284c7",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
            }}
          >
            <RobotOutlined style={{ fontSize: 14, color: "#fff" }} />
          </div>
          <div
            style={{
              padding: "8px 12px",
              background: "#fff",
              borderRadius: 16,
              borderBottomLeftRadius: 4,
              boxShadow: "0 1px 2px rgba(0,0,0,0.08)",
            }}
          >
            <div style={{ fontSize: 12, fontWeight: 500, color: "#0284c7", marginBottom: 2 }}>
              Trợ lý Logistics
            </div>
            <span style={{ fontSize: 13, color: "#262626", lineHeight: 1.4 }}>
              Xin chào! Bạn có thể nhập mã vận đơn, hỏi về COD hoặc tạo yêu cầu hỗ trợ.
            </span>
          </div>
        </div>

        {/* Quick reply buttons */}
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: 8,
            paddingLeft: 36,
            alignSelf: "flex-start",
          }}
        >
          {quickReplies.map((reply) => (
            <Button
              key={reply.key}
              size="small"
              icon={reply.icon}
              onClick={() => handleQuickReply(reply.key)}
              style={{
                borderRadius: 16,
                fontSize: 12,
                height: 32,
                padding: "0 12px",
                display: "flex",
                alignItems: "center",
                gap: 4,
              }}
            >
              {reply.label}
            </Button>
          ))}
        </div>

        {/* Chat messages */}
        {messages
          .filter((m) => m.id !== "greeting")
          .map((msg) => (
            <div key={msg.id}>
              <div
                style={{
                  display: "flex",
                  alignItems: "flex-end",
                  gap: 8,
                  flexDirection: msg.type === "user" ? "row-reverse" : "row",
                  alignSelf: msg.type === "user" ? "flex-end" : "flex-start",
                  maxWidth: "85%",
                  marginLeft: msg.type === "user" ? "auto" : 0,
                }}
              >
                {msg.type === "bot" && (
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: "50%",
                      background: "#0284c7",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0,
                    }}
                  >
                    <RobotOutlined style={{ fontSize: 14, color: "#fff" }} />
                  </div>
                )}

                <div
                  style={{
                    padding: "8px 12px",
                    background: msg.type === "user" ? "#0284c7" : "#fff",
                    borderRadius: 16,
                    borderBottomRightRadius: msg.type === "user" ? 4 : 16,
                    borderBottomLeftRadius: msg.type === "bot" ? 4 : 16,
                    boxShadow: "0 1px 2px rgba(0,0,0,0.08)",
                  }}
                >
                  <span
                    style={{
                      fontSize: 13,
                      color: msg.type === "user" ? "#fff" : "#262626",
                      lineHeight: 1.6,
                      whiteSpace: "pre-line",
                    }}
                  >
                    {msg.content}
                  </span>
                </div>
              </div>

              {/* Action buttons after bot message */}
              {msg.type === "bot" && msg.actions && (
                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: 6,
                    paddingLeft: 36,
                    marginTop: 6,
                    alignSelf: "flex-start",
                  }}
                >
                  {msg.actions.map((action, idx) => (
                    <Button
                      key={idx}
                      size="small"
                      onClick={action.onClick}
                      style={{
                        borderRadius: 12,
                        fontSize: 11,
                        height: 26,
                        padding: "0 10px",
                      }}
                    >
                      {action.label}
                    </Button>
                  ))}
                </div>
              )}
            </div>
          ))}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div
        style={{
          padding: "8px 12px 12px",
          background: "#fff",
          borderTop: "1px solid #f0f0f0",
          display: "flex",
          gap: 8,
          alignItems: "flex-end",
        }}
      >
        <TextArea
          ref={inputRef}
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder={getPlaceholder()}
          autoSize={{ minRows: 1, maxRows: 3 }}
          maxLength={2000}
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
          style={{ flex: 1, borderRadius: 20 }}
        />
        <Button
          type="primary"
          shape="circle"
          icon={<SendOutlined />}
          onClick={handleSend}
          disabled={!inputValue.trim() || sending}
          loading={sending}
        />
      </div>
    </div>
  );
};

export default ChatHome;
