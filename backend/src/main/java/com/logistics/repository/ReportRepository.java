package com.logistics.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.admin.AdminFinancialPoint;
import com.logistics.dto.admin.AdminShipperReportDto;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<AdminFinancialPoint> sumByDate(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT CAST(p.paid_at AS date) as d, SUM(p.system_amount) as s, SUM(p.actual_amount) as a "
                + "FROM payment_submissions p "
                + "WHERE p.status = 'MATCHED' "
                + "AND p.paid_at BETWEEN :start AND :end "
                + "GROUP BY CAST(p.paid_at AS date) "
                + "ORDER BY d";
        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new AdminFinancialPoint(
            ((Date) r[0]).toLocalDate(),
            r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString()),
            r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString())
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminShipperReportDto> reportByShipper(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT p.shipper_id, u.first_name, u.last_name, u.phone_number, COUNT(DISTINCT p.order_id) as cnt, "
                + "SUM(p.system_amount) as s, SUM(p.actual_amount) as a "
                + "FROM payment_submissions p "
                + "LEFT JOIN users u ON u.id = p.shipper_id "
                + "WHERE p.status = 'MATCHED' "
                + "AND p.paid_at BETWEEN :start AND :end "
                + "GROUP BY p.shipper_id, u.first_name, u.last_name, u.phone_number "
                + "ORDER BY cnt DESC";
        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> {
            Integer shipperId = r[0] == null ? null : ((Number) r[0]).intValue();
            String first = r[1] == null ? "" : r[1].toString();
            String last = r[2] == null ? "" : r[2].toString();
            String phone = r[3] == null ? "" : r[3].toString();
            Long cnt = r[4] == null ? 0L : ((Number) r[4]).longValue();
            BigDecimal s = r[5] == null ? BigDecimal.ZERO : new BigDecimal(r[5].toString());
            BigDecimal a = r[6] == null ? BigDecimal.ZERO : new BigDecimal(r[6].toString());
            return new AdminShipperReportDto(shipperId, last + " " + first, phone, cnt, s, a, s.subtract(a));
        }).toList();
    }

        @Transactional(readOnly = true)
        public List<AdminFinancialPoint> transferredByDate(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT CAST(b.created_at AS date) as d, SUM(b.total_system_amount) as s, SUM(b.total_actual_amount) as a "
            + "FROM payment_submission_batches b "
            + "WHERE b.status = 'COMPLETED' "
            + "AND b.created_at BETWEEN :start AND :end "
            + "GROUP BY CAST(b.created_at AS date) "
            + "ORDER BY d";
        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new AdminFinancialPoint(
            ((Date) r[0]).toLocalDate(),
            r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString()),
            r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString())
        )).toList();
        }

        @Transactional(readOnly = true)
        public List<AdminFinancialPoint> shippingFeeByDate(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT CAST(o.created_at AS date) as d, SUM(o.shipping_fee) as s, SUM(o.total_fee) as t "
            + "FROM orders o "
            + "WHERE o.created_at BETWEEN :start AND :end "
            + "GROUP BY CAST(o.created_at AS date) "
            + "ORDER BY d";
        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new AdminFinancialPoint(
            ((Date) r[0]).toLocalDate(),
            r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString()),
            r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString())
        )).toList();
        }

        @Transactional(readOnly = true)
        public List<Object[]> orderOperationSummary(LocalDateTime start, LocalDateTime end) {
        // Trả về các hàng: ngày, tổng đơn, số giao thành công, số thất bại, thời gian trung bình giao (giây), số đang trả, số đã trả
        String sql = "SELECT dates.day as day, COALESCE(t.total_orders,0) as total_orders, COALESCE(dv.delivered,0) as delivered, COALESCE(fd.failed,0) as failed, COALESCE(dv.avg_seconds,0) as avg_seconds, COALESCE(rt.returning,0) as returning, COALESCE(rn.returned,0) as returned "
            + "FROM (SELECT CAST(o.created_at AS date) as day FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) dates "
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day2, COUNT(*) as total_orders FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) t ON t.day2 = dates.day "
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day3, COUNT(*) as delivered, AVG(TIMESTAMPDIFF(SECOND, oh_pick.action_time, oh_del.action_time)) as avg_seconds FROM orders o "
            + "JOIN order_histories oh_pick ON oh_pick.order_id = o.id AND oh_pick.action = 'PICKED_UP' "
            + "JOIN order_histories oh_del ON oh_del.order_id = o.id AND oh_del.action = 'DELIVERED' "
            + "WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) dv ON dv.day3 = dates.day "
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day4, COUNT(*) as failed FROM orders o JOIN order_histories ohf ON ohf.order_id = o.id AND ohf.action = 'FAILED_DELIVERY' WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) fd ON fd.day4 = dates.day "
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day5, COUNT(*) as returning FROM orders o JOIN order_histories ohr ON ohr.order_id = o.id AND ohr.action = 'RETURNING' WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) rt ON rt.day5 = dates.day "
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day6, COUNT(*) as returned FROM orders o JOIN order_histories ohr2 ON ohr2.order_id = o.id AND ohr2.action = 'RETURNED' WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) rn ON rn.day6 = dates.day "
            + "ORDER BY dates.day";
        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows;
        }

        @Transactional(readOnly = true)
        public List<com.logistics.dto.admin.AdminOfficeReportDto> reportByOffice(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT o.id, o.name, COUNT(*) as cnt "
            + "FROM orders ord "
            + "LEFT JOIN offices o ON o.id = ord.from_office_id "
            + "WHERE ord.created_at BETWEEN :start AND :end "
            + "GROUP BY o.id, o.name "
            + "ORDER BY cnt DESC";
        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new com.logistics.dto.admin.AdminOfficeReportDto(
            r[0] == null ? null : ((Number) r[0]).intValue(),
            r[1] == null ? "" : r[1].toString(),
            r[2] == null ? 0L : ((Number) r[2]).longValue()
        )).toList();
        }

        @Transactional(readOnly = true)
        public List<com.logistics.dto.admin.AdminShopReportDto> reportByShop(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT u.id, CONCAT(u.last_name, ' ', u.first_name) as name, COUNT(*) as cnt, SUM(o.order_value) as total_value, SUM(o.shipping_fee) as total_fee "
            + "FROM orders o "
            + "LEFT JOIN users u ON u.id = o.user_id "
            + "WHERE o.created_at BETWEEN :start AND :end "
            + "GROUP BY u.id, u.last_name, u.first_name "
            + "ORDER BY cnt DESC";
        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new com.logistics.dto.admin.AdminShopReportDto(
            r[0] == null ? null : ((Number) r[0]).intValue(),
            ((r[1] == null) ? "" : r[1].toString()),
            r[2] == null ? 0L : ((Number) r[2]).longValue(),
            r[3] == null ? BigDecimal.ZERO : new BigDecimal(r[3].toString()),
            r[4] == null ? BigDecimal.ZERO : new BigDecimal(r[4].toString())
        )).toList();
        }
}
