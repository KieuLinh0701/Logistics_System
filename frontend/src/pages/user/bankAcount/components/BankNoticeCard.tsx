import React from 'react';
import { Card } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

const BankNoticeCard: React.FC = () => {
    return (
        <Card
            style={{
                backgroundColor: '#FFF7E6', 
                border: '1px solid #FFE58F',
                borderRadius: 8,
                marginBottom: 16,
            }}
        >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#AD8B00' }}>
                <ExclamationCircleOutlined style={{ fontSize: 20 }} />
                <span>
                    <strong>Chú ý:</strong> Tài khoản ngân hàng này dùng để nhận tiền COD sau mỗi phiên đối soát.
                    Tài khoản mặc định sẽ là tài khoản nhận chính. Chỉ được thêm tối đa 5 tài khoản.
                </span>
            </div>
        </Card>
    );
};

export default BankNoticeCard;