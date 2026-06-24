package com.logistics.service.chat;

import com.logistics.dto.chat.BotPreviewResponse;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.SupportMessage;
import com.logistics.entity.SupportTicket;
import com.logistics.enums.*;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SupportMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SupportAssistantService {

    private static final String GREETING_MESSAGE = "Xin chào bạn 👋\n\nMình có thể hỗ trợ tra cứu đơn hàng, COD, lịch sử vận chuyển hoặc thông tin người giao hàng.\n\nBạn gửi mã vận đơn hoặc câu hỏi cần hỗ trợ nhé.";

    private static final String THANK_YOU_MESSAGE = "Cảm ơn bạn 😊 Nếu cần hỗ trợ thêm, bạn cứ nhắn cho mình nhé.";

    private static final String FALLBACK_HUMAN_MESSAGE = "Mình đã ghi nhận yêu cầu của bạn.\n\nNhân viên CSKH sẽ tiếp nhận và hỗ trợ bạn trong cuộc trò chuyện này trong thời gian sớm nhất.";

    private static final String ORDER_NOT_FOUND_MESSAGE = "Mình chưa tìm thấy vận đơn này hoặc bạn không có quyền xem thông tin của vận đơn đó. Bạn hãy kiểm tra lại mã vận đơn hoặc tạo yêu cầu hỗ trợ để CSKH kiểm tra.";

    private static final String ASK_TRACKING_FOR_ORDER_MESSAGE = "Bạn vui lòng nhập mã vận đơn để mình kiểm tra thông tin đơn hàng. Ví dụ: HCM006.";

    private static final String ASK_TRACKING_FOR_COD_MESSAGE = "Bạn vui lòng nhập mã vận đơn để mình kiểm tra thông tin COD. Ví dụ: HCM006.";

    private static final String UNKNOWN_MESSAGE = "Mình chưa hiểu rõ yêu cầu của bạn. Bạn có thể nhập mã vận đơn, hỏi về COD hoặc tạo yêu cầu hỗ trợ.";

    private static final String NO_HISTORY_MESSAGE = "Hiện đơn hàng này chưa có lịch sử vận chuyển.";

    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SupportIntentDetector supportIntentDetector;
    private final SupportBotMessageService supportBotMessageService;
    private final SupportOrderLookupService supportOrderLookupService;
    private final SupportMessageRepository supportMessageRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void handleAfterUserMessage(SupportTicket ticket, SupportMessage userMessage) {
        if (ticket == null || userMessage == null) {
            return;
        }

        if (userMessage.getSenderType() != SupportMessageSenderType.USER) {
            return;
        }

        // Bỏ qua nếu đã có tin nhắn của Manager/Admin trong 30 phút gần nhất
        Optional<SupportMessage> lastManagerOrAdminMessage = supportMessageRepository
                .findTopByTicketIdAndSenderTypeInOrderByCreatedAtDesc(ticket.getId(),
                        List.of(SupportMessageSenderType.MANAGER, SupportMessageSenderType.ADMIN));
        if (lastManagerOrAdminMessage.isPresent() && lastManagerOrAdminMessage.get().getCreatedAt() != null
                && lastManagerOrAdminMessage.get().getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(30))) {
            return;
        }

        SupportIntentDetector.Intent intent = supportIntentDetector.detect(userMessage.getMessage());
        if (intent == SupportIntentDetector.Intent.NONE) {
            return;
        }

        if (intent == SupportIntentDetector.Intent.FALLBACK_TO_HUMAN) {
            supportBotMessageService.createBotMessage(ticket.getId(), FALLBACK_HUMAN_MESSAGE);
            return;
        }

        if (intent == SupportIntentDetector.Intent.GREETING) {
            supportBotMessageService.createBotMessage(ticket.getId(), GREETING_MESSAGE);
            return;
        }

        if (intent == SupportIntentDetector.Intent.THANK_YOU) {
            supportBotMessageService.createBotMessage(ticket.getId(), THANK_YOU_MESSAGE);
            return;
        }

        Optional<Order> resolvedOrder = resolveOrderForMessage(ticket, userMessage.getMessage());
        if (resolvedOrder.isEmpty()) {
            supportBotMessageService.createBotMessage(ticket.getId(), ORDER_NOT_FOUND_MESSAGE);
            return;
        }

        Order order = resolvedOrder.get();
        String response = switch (intent) {
            case ORDER_STATUS -> buildOrderStatusText(order);
            case ORDER_DETAIL -> buildOrderDetailText(order);
            case ORDER_HISTORY -> buildOrderHistoryText(order);
            case COD_INFO -> buildCodInfoText(order);
            case SHIPPER_INFO -> buildShipperInfoText(order);
            default -> null;
        };

        if (response != null && !response.isBlank()) {
            supportBotMessageService.createBotMessage(ticket.getId(), response);
        }
    }

    private String buildOrderStatusText(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 Mã vận đơn: ").append(nullSafe(order.getTrackingNumber())).append('\n');
        sb.append("🚚 Trạng thái: ").append(mapOrderStatus(order.getStatus())).append('\n');
        sb.append("👤 Nhân viên giao hàng: ").append(resolveShipperLabel(order)).append('\n');
        sb.append("🏢 Bưu cục gốc: ").append(order.getFromOffice() != null ? order.getFromOffice().getName() : "Chưa có thông tin").append('\n');
        sb.append("🏢 Bưu cục đích: ").append(order.getToOffice() != null ? order.getToOffice().getName() : "Chưa có thông tin").append('\n');
        sb.append("💰 COD: ").append(formatCurrency(order.getCod()));
        return sb.toString();
    }

    private String buildOrderDetailText(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 Mã vận đơn: ").append(nullSafe(order.getTrackingNumber())).append('\n');
        sb.append("👤 Người nhận: ").append(nullSafe(order.getRecipientName())).append('\n');
        sb.append("📞 SĐT nhận hàng: ").append(maskPhone(order.getRecipientPhone())).append('\n');
        sb.append("📍 Địa chỉ nhận: ").append(nullSafe(order.getRecipientFullAddress())).append('\n');
        sb.append("⚖️ Khối lượng: ").append(formatWeight(order.getWeight())).append('\n');
        sb.append("💵 Phí vận chuyển: ").append(order.getShippingFee() != null ? formatCurrency(order.getShippingFee()) : "Chưa có thông tin").append('\n');
        sb.append("💰 COD: ").append(formatCurrency(order.getCod())).append('\n');
        sb.append("🚚 Trạng thái: ").append(mapOrderStatus(order.getStatus()));
        return sb.toString();
    }

    private String buildCodInfoText(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 Mã vận đơn: ").append(nullSafe(order.getTrackingNumber())).append('\n');
        sb.append("💰 Số tiền COD: ").append(formatCurrency(order.getCod())).append('\n');
        sb.append("📌 Trạng thái COD: ").append(mapCodStatus(order.getCodStatus())).append('\n');
        sb.append("💳 Trạng thái thanh toán: ").append(mapPaymentStatus(order.getPaymentStatus()));
        return sb.toString();
    }

    private String buildOrderHistoryText(Order order) {
        List<OrderHistory> histories = orderHistoryRepository.findByOrderIdOrderByActionTimeDesc(order.getId());
        if (histories == null || histories.isEmpty()) {
            return NO_HISTORY_MESSAGE;
        }

        int total = histories.size();
        int limit = Math.min(5, total);
        StringBuilder sb = new StringBuilder();
        sb.append("Lịch sử vận chuyển gần nhất");

        for (int i = 0; i < limit; i++) {
            OrderHistory history = histories.get(i);
            sb.append("\n\n").append(i + 1).append(". ");
            sb.append(formatHistoryTime(history.getActionTime())).append(" - ");
            sb.append(mapOrderHistoryAction(history.getAction()));
        }

        if (total > limit) {
            sb.append("\n\n... còn ").append(total - limit).append(" cập nhật cũ hơn");
        }

        return sb.toString();
    }

    private String buildShipperInfoText(Order order) {
        String shipperName = getShipperName(order);
        if (shipperName == null) {
            shipperName = "Chưa được phân công";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📦 Mã vận đơn: ").append(nullSafe(order.getTrackingNumber())).append('\n');
        sb.append("👤 Shipper: ").append(shipperName).append('\n');
        sb.append("📞 SĐT: ").append(formatShipperPhone(getShipperPhone(order))).append('\n');
        sb.append("🚗 Loại xe: ").append(getVehicleTypeLabel(order));
        return sb.toString();
    }

    private String getShipperName(Order order) {
        if (order == null || order.getEmployee() == null || order.getEmployee().getUser() == null) {
            return null;
        }
        return order.getEmployee().getUser().getFullName();
    }

    private String getShipperPhone(Order order) {
        if (order == null || order.getEmployee() == null || order.getEmployee().getUser() == null) {
            return null;
        }
        return order.getEmployee().getUser().getPhoneNumber();
    }

    private String getVehicleTypeLabel(Order order) {
        return "Chưa cập nhật";
    }

    private String formatShipperPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "Chưa cập nhật";
        }
        return phone.trim();
    }

    private String resolveShipperLabel(Order order) {
        String shipperName = getShipperName(order);
        return shipperName != null ? shipperName : "Chưa được phân công";
    }

    private String mapOrderStatus(OrderStatus status) {
        if (status == null) {
            return "Chưa có thông tin";
        }
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case PENDING -> "Chờ xử lý";
            case CONFIRMED -> "Đã xác nhận";
            case READY_FOR_PICKUP -> "Sẵn sàng lấy hàng";
            case PICKUP_RETRY -> "Chờ lấy lại";
            case PICKUP_FAILED_FINAL -> "Lấy hàng thất bại cuối cùng";
            case PICKING_UP -> "Đang lấy hàng";
            case PICKED_UP -> "Đã lấy hàng";
            case AT_ORIGIN_OFFICE -> "Tại bưu cục gốc";
            case IN_TRANSIT -> "Đang trung chuyển";
            case AT_DEST_OFFICE -> "Tại bưu cục đích";
            case DELIVERING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao thành công";
            case PARTIAL_DELIVERY -> "Giao một phần";
            case PARTIAL_RETURN -> "Hoàn một phần";
            case DELIVERY_RETRY -> "Chờ giao lại";
            case DELIVERY_FAILED_FINAL -> "Giao thất bại cuối cùng";
            case CANCELLED -> "Đã hủy";
            case RETURNING -> "Đang hoàn hàng";
            case RETURN_RETRY -> "Hoàn hàng lại";
            case RETURN_FAILED_FINAL -> "Hoàn hàng thất bại cuối cùng";
            case RETURNED -> "Đã hoàn hàng";
            default -> status.name();
        };
    }

    private String mapCodStatus(OrderCodStatus status) {
        if (status == null) {
            return "Chưa có thông tin";
        }
        return switch (status) {
            case NONE -> "Không có COD";
            case EXPECTED -> "Chờ thu COD";
            case PENDING -> "Shipper đang giữ tiền";
            case SUBMITTED -> "Đã đưa vào phiên nộp tiền";
            case RECEIVED -> "Đã đối soát COD";
            case TRANSFERRED -> "Đã chuyển tiền COD";
        };
    }

    private String mapPaymentStatus(OrderPaymentStatus status) {
        if (status == null) {
            return "Chưa có thông tin";
        }
        return switch (status) {
            case PAID -> "Đã thanh toán";
            case UNPAID -> "Chưa thanh toán";
            case REFUNDED -> "Đã hoàn tiền";
            default -> status.name();
        };
    }

    private String mapOrderHistoryAction(OrderHistoryActionType action) {
        if (action == null) {
            return "Chưa có thông tin";
        }
        return switch (action) {
            case PENDING -> "Chờ xử lý";
            case TRANSIT_TO_OFFICE -> "Đang chuyển về bưu cục";
            case READY_FOR_PICKUP -> "Sẵn sàng lấy hàng";
            case URGENT_PICKUP -> "Ưu tiên lấy hàng";
            case CONFIRMED -> "Đã xác nhận";
            case PICKING_UP -> "Đang lấy hàng";
            case PICKED_UP -> "Đã lấy hàng";
            case PICKUP_FAILED_FINAL -> "Lấy hàng thất bại cuối cùng";
            case IMPORTED -> "Đã nhập hàng";
            case EXPORTED -> "Đã xuất hàng";
            case AT_DEST_OFFICE -> "Tại bưu cục đích";
            case DELIVERING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao thành công";
            case PARTIAL_DELIVERY -> "Giao một phần";
            case PARTIAL_RETURN -> "Hoàn một phần";
            case DELIVERY_RETRY -> "Chờ giao lại";
            case DELIVERY_FAILED_FINAL -> "Giao thất bại cuối cùng";
            case RETURNING -> "Đang hoàn hàng";
            case RETURN_AT_ORIGIN_OFFICE -> "Hàng hoàn đã về bưu cục gốc";
            case RETURN_RETRY -> "Hoàn hàng lại";
            case RETURN_FAILED_FINAL -> "Hoàn hàng thất bại cuối cùng";
            case RETURNED -> "Đã hoàn hàng";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String formatHistoryTime(LocalDateTime time) {
        if (time == null) {
            return "Chưa có thông tin";
        }
        return time.format(HISTORY_TIME_FORMAT);
    }

    private Optional<Order> resolveOrderForMessage(SupportTicket ticket, String message) {
        String trackingNumber = supportOrderLookupService.extractTrackingNumber(message);
        if (trackingNumber != null) {
            return supportOrderLookupService.resolveByTrackingNumber(ticket.getCreatedByAccountId(), trackingNumber)
                    .map(order -> order);
        }

        if (ticket.getRelatedType() != null && "ORDER".equalsIgnoreCase(ticket.getRelatedType()) && ticket.getRelatedId() != null) {
            return orderRepository.findByIdAndUserId(ticket.getRelatedId(), ticket.getCreatedByAccountId());
        }

        return Optional.empty();
    }

    private String formatCurrency(Integer amount) {
        if (amount == null) {
            return "Chưa có thông tin";
        }
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        return format.format(amount) + "đ";
    }

    private String formatWeight(BigDecimal value) {
        if (value == null) {
            return "Chưa có thông tin";
        }
        return value.stripTrailingZeros().toPlainString() + " kg";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "N/A";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "***";
        }
        return "***" + trimmed.substring(trimmed.length() - 3);
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    public BotPreviewResponse previewMessage(String message) {
        // Nếu message blank
        if (message == null || message.isBlank()) {
            return new BotPreviewResponse("NONE",
                    "Bạn vui lòng nhập mã vận đơn hoặc nội dung cần hỗ trợ.",
                    false, false);
        }

        SupportIntentDetector.Intent intent = supportIntentDetector.detect(message);

        // Extract tracking number trước
        String trackingNumber = supportOrderLookupService.extractTrackingNumber(message);

        // Nếu có trackingNumber - thử tìm đơn
        if (trackingNumber != null) {
            Optional<Order> order = supportOrderLookupService.resolveByTrackingNumber(null, trackingNumber);
            if (order.isPresent()) {
                Order o = order.get();
                String reply;
                String intentName = intent.name();

                switch (intent) {
                    case COD_INFO:
                        reply = buildCodInfoText(o);
                        break;
                    case ORDER_DETAIL:
                        reply = buildOrderDetailText(o);
                        break;
                    case ORDER_HISTORY:
                        reply = buildOrderHistoryText(o);
                        break;
                    case SHIPPER_INFO:
                        reply = buildShipperInfoText(o);
                        break;
                    case NONE:
                    case ORDER_STATUS:
                    default:
                        reply = buildOrderStatusText(o);
                        if (intent == SupportIntentDetector.Intent.NONE) {
                            intentName = "ORDER_STATUS";
                        }
                        break;
                }

                return new BotPreviewResponse(intentName, reply, false, false);
            } else {
                // Không tìm thấy đơn
                return new BotPreviewResponse("ORDER_STATUS", ORDER_NOT_FOUND_MESSAGE, true, true);
            }
        }

        // E. Không có trackingNumber
        switch (intent) {
            case GREETING:
                return new BotPreviewResponse("GREETING", GREETING_MESSAGE, false, false);

            case THANK_YOU:
                return new BotPreviewResponse("THANK_YOU", THANK_YOU_MESSAGE, false, false);

            case COD_INFO:
                return new BotPreviewResponse("COD_INFO", ASK_TRACKING_FOR_COD_MESSAGE, false, false);

            case ORDER_STATUS:
            case ORDER_DETAIL:
            case ORDER_HISTORY:
            case SHIPPER_INFO:
                return new BotPreviewResponse(intent.name(), ASK_TRACKING_FOR_ORDER_MESSAGE, false, false);

            case FALLBACK_TO_HUMAN:
                return new BotPreviewResponse("FALLBACK_TO_HUMAN",
                        "Vấn đề này cần CSKH tiếp nhận. " + FALLBACK_HUMAN_MESSAGE,
                        true, false);

            case NONE:
            default:
                return new BotPreviewResponse("NONE", UNKNOWN_MESSAGE, true, true);
        }
    }
}
