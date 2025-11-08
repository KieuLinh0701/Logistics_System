import React, { useState, useRef } from "react";
import { Input, Row, Col } from "antd";
import type { InputRef } from "antd";
import "./OtpInput.css"; 

interface OtpInputProps {
  length: number;
  value?: string;
  onChange: (value: string) => void;
}

const OtpInput: React.FC<OtpInputProps> = ({ length, value = "", onChange }) => {
  const [values, setValues] = useState<string[]>(Array.from({ length }, (_, i) => value[i] || ""));
  const inputsRef = useRef<Array<InputRef | null>>([]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>, idx: number) => {
    const val = e.target.value.replace(/\D/g, "");
    if (!val) return;

    const newValues = [...values];
    newValues[idx] = val[0];
    setValues(newValues);
    onChange(newValues.join(""));

    if (idx < length - 1) inputsRef.current[idx + 1]?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, idx: number) => {
    if (e.key === "Backspace") {
      const newValues = [...values];
      newValues[idx] = "";
      setValues(newValues);
      onChange(newValues.join(""));
      if (idx > 0) inputsRef.current[idx - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    const paste = e.clipboardData.getData("text").replace(/\D/g, "");
    if (!paste) return;

    const newValues = [...values];
    for (let i = 0; i < length; i++) {
      newValues[i] = paste[i] || "";
    }
    setValues(newValues);
    onChange(newValues.join(""));

    const nextIdx = paste.length >= length ? length - 1 : paste.length;
    inputsRef.current[nextIdx]?.focus();
  };

  return (
    <Row justify="center" gutter={12} style={{ width: "100%" }}>
      {Array.from({ length }).map((_, idx) => (
        <Col key={idx}>
          <Input
            ref={(el) => { inputsRef.current[idx] = el; }}
            value={values[idx]}
            onChange={(e) => handleChange(e, idx)}
            onKeyDown={(e) => handleKeyDown(e, idx)}
            onPaste={handlePaste}
            maxLength={1}
            size="large"
            prefix={undefined}
            className="form-input otp-input"
          />
        </Col>
      ))}
    </Row>
  );
};

export default OtpInput;