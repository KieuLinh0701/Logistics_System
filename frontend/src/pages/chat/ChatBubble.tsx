import { CustomerServiceOutlined } from "@ant-design/icons";
import { Badge } from "antd";
import Draggable, { type DraggableData, type DraggableEvent } from "react-draggable";
import { useMemo, useState } from "react";
import type { ChatPosition } from "../../hooks/chatStore";

type Props = {
  unreadCount: number;
  position: ChatPosition;
  onOpen: () => void;
  onPositionChange: (position: ChatPosition) => void;
  draggableEnabled: boolean;
};

const ChatBubble: React.FC<Props> = ({ unreadCount, position, onOpen, onPositionChange, draggableEnabled }) => {
  const [hovered, setHovered] = useState(false);
  const [dragging, setDragging] = useState(false);

  const badgeValue = useMemo(() => {
    if (unreadCount <= 0) {
      return 0;
    }
    return unreadCount > 9 ? "9+" : unreadCount;
  }, [unreadCount]);

  const bubbleNode = (
    <div
      style={{
        width: 50,
        height: 50,
        borderRadius: "50%",
        background: "linear-gradient(135deg, #0ea5e9, #0284c7)",
        color: "#fff",
        boxShadow: unreadCount > 0 ? "0 12px 28px rgba(2, 132, 199, 0.55)" : "0 10px 24px rgba(0,0,0,0.24)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        userSelect: "none",
        transition: "transform 0.16s ease, box-shadow 0.2s ease",
        transform: hovered ? "scale(1.06)" : "scale(1)",
        animation: unreadCount > 0 ? "chatBubblePulse 1.4s ease-in-out infinite" : "none",
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onClick={() => {
        if (!dragging) {
          onOpen();
        }
      }}
    >
      <CustomerServiceOutlined style={{ fontSize: 23 }} />
      <style>
        {`@keyframes chatBubblePulse {
          0% { box-shadow: 0 8px 20px rgba(2,132,199,0.35); }
          50% { box-shadow: 0 14px 26px rgba(2,132,199,0.62); }
          100% { box-shadow: 0 8px 20px rgba(2,132,199,0.35); }
        }`}
      </style>
    </div>
  );

  if (!draggableEnabled) {
    return (
      <div style={{ position: "fixed", right: 16, bottom: 20, zIndex: 10000 }}>
        <Badge count={badgeValue}>{bubbleNode}</Badge>
      </div>
    );
  }

  const handleStop = (_event: DraggableEvent, data: DraggableData) => {
    onPositionChange({ x: data.x, y: data.y });
    window.setTimeout(() => setDragging(false), 0);
  };

  return (
    <Draggable
      position={position}
      onStart={() => setDragging(false)}
      onDrag={() => setDragging(true)}
      onStop={handleStop}
    >
      <div style={{ position: "fixed", left: 0, top: 0, zIndex: 10000 }}>
        <Badge count={badgeValue}>{bubbleNode}</Badge>
      </div>
    </Draggable>
  );
};

export default ChatBubble;
