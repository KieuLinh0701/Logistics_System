export interface AdminOrder {
  id: number;
  trackingNumber: string;
  senderName: string;
  recipientName: string;
  status: string;
  totalFee: number;
  createdAt: string;
}

