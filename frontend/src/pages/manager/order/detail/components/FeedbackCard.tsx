import React, { useState, useEffect } from 'react';
import { Card, Rate, Input, Button, message, Modal, Spin } from 'antd';
import { StarOutlined, SendOutlined } from '@ant-design/icons';
import axios from 'axios';

const { TextArea } = Input;

interface Feedback {
  id: number;
  rating: number;
  comment: string;
  serviceRating?: number;
  deliveryRating?: number;
  isAnonymous: boolean;
  createdAt: string;
}

interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
}

interface FeedbackCardProps {
  orderId: number;
  orderStatus: string;
  onFeedbackSubmitted?: () => void;
}

const FeedbackCard: React.FC<FeedbackCardProps> = ({ orderId, orderStatus, onFeedbackSubmitted }) => {
  const [loading, setLoading] = useState(false);
  const [existingFeedback, setExistingFeedback] = useState<Feedback | null>(null);
  const [rating, setRating] = useState(0);
  const [serviceRating, setServiceRating] = useState(0);
  const [deliveryRating, setDeliveryRating] = useState(0);
  const [comment, setComment] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    // Chỉ fetch feedback nếu đơn hàng đã được giao
    if (orderStatus === 'PENDING') {
      fetchFeedback();
    }
  }, [orderId, orderStatus]);

  const fetchFeedback = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get<ApiResponse<Feedback>>(`/api/user/orders/${orderId}/feedback`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.data.success && response.data.data) {
        const feedback = response.data.data;
        setExistingFeedback(feedback);
        setRating(feedback.rating);
        setServiceRating(feedback.serviceRating || 0);
        setDeliveryRating(feedback.deliveryRating || 0);
        setComment(feedback.comment || '');
        setIsAnonymous(feedback.isAnonymous);
      }
    } catch (error: any) {
      // Không hiển thị lỗi nếu chưa có feedback
      if (error.response?.status !== 404) {
        console.error('Error fetching feedback:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (rating === 0) {
      message.warning('Vui lòng chọn điểm đánh giá');
      return;
    }

    setIsSubmitting(true);
    try {
      const token = localStorage.getItem('token');
      const data = {
        rating,
        comment,
        serviceRating: serviceRating || undefined,
        deliveryRating: deliveryRating || undefined,
        isAnonymous,
      };

      let response;
      if (existingFeedback) {
        // Cập nhật feedback
        response = await axios.put<ApiResponse<Feedback>>(`/api/user/feedbacks/${existingFeedback.id}`, data, {
          headers: { Authorization: `Bearer ${token}` },
        });
      } else {
        // Tạo feedback mới
        response = await axios.post<ApiResponse<Feedback>>(`/api/user/orders/${orderId}/feedback`, data, {
          headers: { Authorization: `Bearer ${token}` },
        });
      }

      if (response.data.success) {
        message.success('Cảm ơn bạn đã đánh giá dịch vụ của chúng tôi!');
        if (response.data.data) {
          setExistingFeedback(response.data.data);
        }
        if (onFeedbackSubmitted) {
          onFeedbackSubmitted();
        }
        // Refresh to show the feedback
        fetchFeedback();
      } else {
        message.error(response.data.message || 'Gửi đánh giá thất bại');
      }
    } catch (error: any) {
      console.error('Error submitting feedback:', error);
      message.error(error.response?.data?.message || 'Lỗi khi gửi đánh giá');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Chỉ hiển thị card nếu đơn hàng đã được giao
  if (orderStatus !== 'delivered') {
    return null;
  }

  if (loading) {
    return (
      <Card title={<><StarOutlined /> Đánh giá dịch vụ</>} style={{ marginBottom: 16 }}>
        <Spin size="large" />
      </Card>
    );
  }

  return (
    <Card
      title={
        <span>
          <StarOutlined style={{ marginRight: 8, color: '#faad14' }} />
          {existingFeedback ? 'Đánh giá của bạn' : 'Đánh giá dịch vụ'}
        </span>
      }
      style={{ marginBottom: 16 }}
    >
      {existingFeedback ? (
        <div>
          <div style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 8 }}>
              <strong>Điểm đánh giá tổng thể:</strong>
            </div>
            <Rate disabled value={rating} />
          </div>

          {(serviceRating || deliveryRating) && (
            <div style={{ marginBottom: 16 }}>
              {serviceRating > 0 && (
                <div style={{ marginBottom: 8 }}>
                  <div style={{ marginBottom: 4 }}>
                    <strong>Chất lượng dịch vụ:</strong>
                  </div>
                  <Rate disabled value={serviceRating} />
                </div>
              )}
              {deliveryRating > 0 && (
                <div style={{ marginBottom: 8 }}>
                  <div style={{ marginBottom: 4 }}>
                    <strong>Thái độ nhân viên giao hàng:</strong>
                  </div>
                  <Rate disabled value={deliveryRating} />
                </div>
              )}
            </div>
          )}

          {comment && (
            <div style={{ marginBottom: 16 }}>
              <strong>Nhận xét:</strong>
              <p style={{ marginTop: 8, marginBottom: 0 }}>{comment}</p>
            </div>
          )}

          <Button type="link" onClick={() => setExistingFeedback(null)}>
            Chỉnh sửa đánh giá
          </Button>
        </div>
      ) : (
        <div>
          <div style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 8 }}>
              <strong>Đánh giá tổng thể *</strong>
            </div>
            <Rate value={rating} onChange={setRating} style={{ fontSize: 24 }} />
          </div>

          <div style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 8 }}>
              <strong>Chất lượng dịch vụ</strong>
            </div>
            <Rate value={serviceRating} onChange={setServiceRating} />
          </div>

          <div style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 8 }}>
              <strong>Thái độ nhân viên giao hàng</strong>
            </div>
            <Rate value={deliveryRating} onChange={setDeliveryRating} />
          </div>

          <div style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 8 }}>
              <strong>Nhận xét chi tiết</strong>
            </div>
            <TextArea
              rows={4}
              placeholder="Chia sẻ cảm nhận của bạn về dịch vụ..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              maxLength={500}
            />
            <div style={{ textAlign: 'right', fontSize: 12, color: '#999' }}>
              {comment.length}/500
            </div>
          </div>

          <div style={{ marginBottom: 16 }}>
            <label>
              <input
                type="checkbox"
                checked={isAnonymous}
                onChange={(e) => setIsAnonymous(e.target.checked)}
                style={{ marginRight: 8 }}
              />
              Đánh giá ẩn danh
            </label>
          </div>

          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSubmit}
            loading={isSubmitting}
            block
            style={{ backgroundColor: '#1C3D90', borderColor: '#1C3D90' }}
          >
            Gửi đánh giá
          </Button>
        </div>
      )}
    </Card>
  );
};

export default FeedbackCard;

