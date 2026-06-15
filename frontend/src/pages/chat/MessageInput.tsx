import { Button, Input } from "antd";
import { SendOutlined } from "@ant-design/icons";
import { useState } from "react";

const { TextArea } = Input;

type Props = {
  onSend: (content: string) => Promise<void>;
  sending: boolean;
};

const MessageInput: React.FC<Props> = ({ onSend, sending }) => {
  const [value, setValue] = useState("");

  const handleSend = async () => {
    const content = value.trim();
    if (!content) {
      return;
    }

    await onSend(content);
    setValue("");
  };

  return (
    <div style={{ padding: 12, borderTop: "1px solid #f0f0f0", background: "#fafafa" }}>
      <div style={{ display: "flex", gap: 8, alignItems: "flex-end", flexWrap: "nowrap" }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <TextArea
            value={value}
            onChange={(e) => setValue(e.target.value)}
            autoSize={{ minRows: 2, maxRows: 4 }}
            maxLength={4000}
            placeholder="Nhập nội dung cần hỗ trợ..."
            onPressEnter={(event) => {
              if (!event.shiftKey) {
                event.preventDefault();
                void handleSend();
              }
            }}
          />
        </div>

        <div style={{ display: "flex", alignItems: "center" }}>
          <Button
            type="primary"
            shape="circle"
            icon={<SendOutlined />}
            loading={sending}
            disabled={sending || !value.trim()}
            onClick={() => void handleSend()}
          />
        </div>
      </div>
    </div>
  );
};

export default MessageInput;
