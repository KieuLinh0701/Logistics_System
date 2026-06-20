package com.logistics.service.user;

import com.logistics.dto.user.dashboard.UserRevenueStatsDTO;
import com.logistics.dto.user.settlement.UserSettlementBatchListDto;
import com.logistics.dto.user.settlement.UserSettlementOrderDto;
import com.logistics.dto.user.settlement.UserSettlementSummaryResponse;
import com.logistics.dto.user.settlement.UserSettlementTransactionDto;
import com.logistics.entity.Order;
import com.logistics.entity.SettlementBatch;
import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.SettlementStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.SettlementBatchErrorCode;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.SettlementBatchMapper;
import com.logistics.mapper.SettlementTransactionMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.SettlementTransactionRepository;
import com.logistics.request.SearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.OrderSpecification;
import com.logistics.specification.SettlementBatchSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.logistics.utils.OrderUtils.*;
import static com.logistics.utils.SettlementBatchUtils.translateSettlementBatchStatus;
import static com.logistics.utils.SettlementTransactionUtils.translateSettlementTransactionStatus;
import static com.logistics.utils.SettlementTransactionUtils.translateSettlementTransactionType;

@Service
@RequiredArgsConstructor
public class SettlementBatchUserService {

    private final SettlementBatchRepository batchRepository;

    private final SettlementTransactionRepository transactionRepository;

    private final OrderRepository orderRepository;

    private final UserSettlementScheduleUserService scheduleUserService;

    private final UserUserService userService;

    public UserSettlementSummaryResponse getSummary(Integer userId) {
        Integer shopId = userService.getShopId(userId);

        List<SettlementBatch> batches = batchRepository.findByShop_Id(shopId);

        BigDecimal received = batches.stream()
                .filter(b -> b.getStatus() == SettlementStatus.COMPLETED
                        && b.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(SettlementBatch::getBalanceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = batches.stream()
                .filter(b -> b.getStatus() != SettlementStatus.COMPLETED
                        && b.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(SettlementBatch::getBalanceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debt = batches.stream()
                .filter(b -> b.getStatus() != SettlementStatus.COMPLETED
                        && b.getBalanceAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(b -> b.getBalanceAmount().abs().subtract(b.getPaidAmount()))
                .filter(r -> r.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new UserSettlementSummaryResponse(received, pending, debt);
    }

    public ListResponse<UserSettlementBatchListDto> list(
            Integer userId, SearchRequest request) {
        Integer shopId = userService.getShopId(userId);

        Sort sort = buildSort(request.getSort());
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), sort);
        Page<SettlementBatch> pageData = getSettlementBatchs(shopId, request, pageable);

        List<UserSettlementBatchListDto> list = pageData.getContent()
                .stream()
                .map(SettlementBatchMapper::toListDtos)
                .toList();

        Pagination pagination = new Pagination(
                (int) pageData.getTotalElements(),
                request.getPage(),
                request.getLimit(),
                pageData.getTotalPages());

        ListResponse<UserSettlementBatchListDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    private Page<SettlementBatch> getSettlementBatchs(
            Integer userId,
            SearchRequest request,
            Pageable pageable) {

        Specification<SettlementBatch> spec = SettlementBatchSpecification.unrestricted()
                .and(SettlementBatchSpecification.userId(userId))
                .and(SettlementBatchSpecification.search(request.getSearch()))
                .and(SettlementBatchSpecification.status(request.getStatus()))
                .and(SettlementBatchSpecification.balanceType(request.getType()))
                .and(SettlementBatchSpecification.createdAtBetween(
                        parseDate(request.getStartDate()),
                        parseDate(request.getEndDate())));

        return batchRepository.findAll(spec, pageable);
    }

    private Sort buildSort(String sort) {
        if (sort == null)
            return Sort.unsorted();
        return switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "balance_high" -> Sort.by("balanceAmount").descending();
            case "balance_low" -> Sort.by("balanceAmount").ascending();
            default -> Sort.unsorted();
        };
    }

    private LocalDateTime parseDate(String s) {
        return (s != null && !s.isBlank()) ? LocalDateTime.parse(s) : null;
    }

    public ListResponse<UserSettlementOrderDto> getOrdersBySettlementBatchId(
            int userId,
            Integer batchId,
            SearchRequest request) {
        Integer shopId = userService.getShopId(userId);

        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        String status = request.getStatus();
        String sort = request.getSort();
        String cod = request.getCod();
        String payer = request.getPayer();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.settlementBatchId(batchId))
                .and(OrderSpecification.userId(shopId))
                .and(OrderSpecification.settlementSearch(search))
                .and(OrderSpecification.payer(payer))
                .and(OrderSpecification.status(status))
                .and(OrderSpecification.cod(cod))
                .and(OrderSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "cod_high" -> Sort.by("cod").descending();
            case "cod_low" -> Sort.by("cod").ascending();
            case "fee_high" -> Sort.by("totalFee").descending();
            case "fee_low" -> Sort.by("totalFee").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<Order> pageData = orderRepository.findAll(spec, pageable);

        List<UserSettlementOrderDto> list = pageData.getContent()
                .stream()
                .map(OrderMapper::toUserSettlementOrderDto)
                .toList();

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<UserSettlementOrderDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public List<UserSettlementTransactionDto> getSettlementTransactionsBySettlementBatchId(
            Integer userId, Integer batchId) {
        Integer shopId = userService.getShopId(userId);

        Sort sort = Sort.by(Sort.Direction.DESC, "paidAt");

        batchRepository
                .findByIdAndShop_Id(batchId, shopId)
                .orElseThrow(() -> new AppException(SettlementBatchErrorCode.SETTLEMENT_BATCH_ACCESS_DENIED));

        List<SettlementTransaction> transactions = transactionRepository.findBySettlementBatchId(batchId, sort);

        return transactions.stream()
                .map(SettlementTransactionMapper::toUserSettlementTransactionDto)
                .toList();
    }

    public UserSettlementBatchListDto getBySettlementBatchId(Integer userId, Integer batchId) {
        Integer shopId = userService.getShopId(userId);

        SettlementBatch batch = batchRepository.findByIdAndShop_Id(batchId, shopId)
                .orElse(null);

        if (batch == null) {
            throw  new AppException(SettlementBatchErrorCode.SETTLEMENT_BATCH_NOT_FOUND);
        }
        return SettlementBatchMapper.toListDtos(batch);
    }

    public UserRevenueStatsDTO getUserRevenueStats(Integer userId) {

        BigDecimal received = batchRepository.sumReceivedByUser(userId);

        BigDecimal nextSettlement = orderRepository.sumPendingCODNow(
                userId, List.of(OrderStatus.DELIVERED, OrderStatus.RETURNED));

        BigDecimal pendingDebt = calculatePendingDebt(userId);

        String nextSettlementDate = scheduleUserService.getNextSettlementDate(userId);

        return new UserRevenueStatsDTO(
                received,
                nextSettlement,
                pendingDebt,
                nextSettlementDate);
    }

    public BigDecimal calculatePendingDebt(Integer userId) {
        List<SettlementBatch> batches = batchRepository.findDebtBatchesByUser(userId);

        BigDecimal totalDebt = BigDecimal.ZERO;

        for (SettlementBatch b : batches) {
            BigDecimal originalDebt = b.getBalanceAmount().abs();

            if (b.getStatus() == SettlementStatus.PENDING
                    || b.getStatus() == SettlementStatus.FAILED) {

                totalDebt = totalDebt.add(originalDebt);
            }
        }

        return totalDebt;
    }

    public byte[] export(Integer userId, SearchRequest request) {
        Integer shopId = userService.getShopId(userId);

        Sort sort = buildSort(request.getSort());
        List<SettlementBatch> batches = getSettlementBatchs(shopId, request, Pageable.unpaged(sort))
                .getContent();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("SettlementBatches");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Mã phiên",
                    "Trạng thái",
                    "Hình thức",
                    "Số tiền",
                    "Thời gian đối soát",
                    "Thời gian cập nhật"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (SettlementBatch b : batches) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(b.getCode() != null ? b.getCode() : "");
                row.createCell(1).setCellValue(translateSettlementBatchStatus(b.getStatus()));

                String type = (b.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) ? "Hòa"
                        : (b.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) ? "Shop trả hệ thống"
                        : "Hệ thống trả shop";
                row.createCell(2).setCellValue(type);

                row.createCell(3).setCellValue(b.getBalanceAmount().doubleValue());
                row.createCell(4).setCellValue(b.getCreatedAt() != null ? b.getCreatedAt().format(dtf) : "N/A");
                row.createCell(5).setCellValue(b.getUpdatedAt() != null ? b.getUpdatedAt().format(dtf) : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public byte[] exportById(Integer userId, Integer settlementBatchId) {
        Integer shopId = userService.getShopId(userId);

        // Lấy thông tin batch
        batchRepository
                .findByIdAndShop_Id(settlementBatchId, shopId)
                .orElseThrow(() -> new AppException(SettlementBatchErrorCode.SETTLEMENT_BATCH_ACCESS_DENIED));

        // Lấy toàn bộ đơn hàng của batch (không filter)
        Specification<Order> orderSpec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.settlementBatchId(settlementBatchId))
                .and(OrderSpecification.userId(shopId));

        List<Order> orders = orderRepository.findAll(orderSpec, Sort.by("createdAt").descending());

        // Lấy toàn bộ giao dịch thanh toán
        List<SettlementTransaction> transactions = transactionRepository
                .findBySettlementBatchId(settlementBatchId, Sort.by(Sort.Direction.DESC, "paidAt"));

        try (Workbook workbook = new XSSFWorkbook()) {

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
            DateTimeFormatter dtfDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // ── Sheet 1: Danh sách đơn hàng ──
            Sheet orderSheet = workbook.createSheet("Orders");

            String[] orderHeaders = {
                    "Mã đơn hàng",
                    "Trạng thái",
                    "Ngày giao / hoàn",
                    "Người thanh toán",
                    "COD (chưa phí)",
                    "Phí dịch vụ",
                    "Trạng thái thanh toán"
            };

            Row orderHeaderRow = orderSheet.createRow(0);
            for (int i = 0; i < orderHeaders.length; i++) {
                Cell cell = orderHeaderRow.createCell(i);
                cell.setCellValue(orderHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int orderRowIdx = 1;
            for (Order o : orders) {
                Row row = orderSheet.createRow(orderRowIdx++);

                row.createCell(0).setCellValue(o.getTrackingNumber() != null ? o.getTrackingNumber() : "");
                row.createCell(1).setCellValue(translateOrderStatus(o.getStatus()));
                row.createCell(2).setCellValue(o.getDeliveredAt() != null ? o.getDeliveredAt().format(dtfDate) : "N/A");
                row.createCell(3).setCellValue(translateOrderPayerType(o.getPayer()));
                row.createCell(4).setCellValue(o.getCod() != null ? o.getCod().doubleValue() : 0);
                row.createCell(5).setCellValue(o.getTotalFee() != null ? o.getTotalFee().doubleValue() : 0);
                row.createCell(6).setCellValue(translateOrderPaymentStatus(o.getPaymentStatus()));
            }

            for (int i = 0; i < orderHeaders.length; i++) {
                orderSheet.autoSizeColumn(i);
            }

            // ── Sheet 2: Hóa đơn thanh toán ──
            Sheet txSheet = workbook.createSheet("Transactions");

            String[] txHeaders = {
                    "Mã giao dịch",
                    "Loại",
                    "Số tiền",
                    "Trạng thái",
                    "Ngân hàng",
                    "Tên tài khoản",
                    "Số tài khoản",
                    "Thời gian tạo",
                    "Thời gian thanh toán"
            };

            Row txHeaderRow = txSheet.createRow(0);
            for (int i = 0; i < txHeaders.length; i++) {
                Cell cell = txHeaderRow.createCell(i);
                cell.setCellValue(txHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int txRowIdx = 1;
            for (SettlementTransaction tx : transactions) {
                Row row = txSheet.createRow(txRowIdx++);

                row.createCell(0).setCellValue(tx.getCode() != null ? tx.getCode() : "");
                row.createCell(1).setCellValue(translateSettlementTransactionType(tx.getType()));
                row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
                row.createCell(3).setCellValue(translateSettlementTransactionStatus(tx.getStatus()));
                row.createCell(4).setCellValue(tx.getBankName() != null ? tx.getBankName() : "N/A");
                row.createCell(5).setCellValue(tx.getAccountName() != null ? tx.getAccountName() : "N/A");
                row.createCell(6).setCellValue(tx.getAccountNumber() != null ? tx.getAccountNumber() : "N/A");
                row.createCell(7).setCellValue(tx.getCreatedAt() != null ? tx.getCreatedAt().format(dtf) : "N/A");
                row.createCell(8).setCellValue(tx.getPaidAt() != null ? tx.getPaidAt().format(dtf) : "N/A");
            }

            for (int i = 0; i < txHeaders.length; i++) {
                txSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }
}