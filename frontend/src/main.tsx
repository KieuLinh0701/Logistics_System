import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App.tsx';
import { ConfigProvider } from "antd";
import viVN from "antd/locale/vi_VN";

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      locale={viVN}
      getPopupContainer={() => document.body}
    >
      <App />
    </ConfigProvider>
  </StrictMode>,
);