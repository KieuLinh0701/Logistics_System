package com.logistics.repository;

import com.logistics.dto.admin.AdminFinancialPoint;
import com.logistics.dto.admin.AdminShipperReportDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Slf4j
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
            toLocalDate(r[0]),
            safeBigDecimal(r[1]),
            safeBigDecimal(r[2])
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
            Long cnt = safeLong(r[4]);
            BigDecimal s = safeBigDecimal(r[5]);
            BigDecimal a = safeBigDecimal(r[6]);
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
            toLocalDate(r[0]),
            safeBigDecimal(r[1]),
            safeBigDecimal(r[2])
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
            toLocalDate(r[0]),
            safeBigDecimal(r[1]),
            safeBigDecimal(r[2])
        )).toList();
        }

        @Transactional(readOnly = true)
        public List<Object[]> orderOperationSummary(LocalDateTime start, LocalDateTime end) {
        // Use orders.status for main counts, order_histories only for avg_seconds calculation
        // This ensures demo data without order_histories still shows correct counts
        String sql = "SELECT dates.day as day, "
            + "COALESCE(t.total_orders,0) as total_orders, "
            + "COALESCE(dv.delivered,0) + COALESCE(dv2.delivered_status,0) as delivered, "
            + "COALESCE(fd.failed,0) + COALESCE(fd2.failed_status,0) as failed, "
            + "COALESCE(dv.avg_seconds,0) as avg_seconds, "
            + "COALESCE(rt.returning,0) + COALESCE(partial.partial,0) as returning, "
            + "COALESCE(rn.returned,0) as returned "
            + "FROM (SELECT CAST(o.created_at AS date) as day FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) dates "
            // Total orders by date
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day2, COUNT(*) as total_orders FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) t ON t.day2 = dates.day "
            // Delivered: from order_histories + from orders.status as fallback
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day3, COUNT(DISTINCT o.id) as delivered, AVG(TIMESTAMPDIFF(SECOND, oh_pick.action_time, oh_del.action_time)) as avg_seconds FROM orders o "
            + "JOIN order_histories oh_pick ON oh_pick.order_id = o.id AND oh_pick.action = 'PICKED_UP' "
            + "JOIN order_histories oh_del ON oh_del.order_id = o.id AND oh_del.action = 'DELIVERED' "
            + "WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) dv ON dv.day3 = dates.day "
            + "LEFT JOIN (SELECT CAST(created_at AS date) as day_status, COUNT(*) as delivered_status FROM orders WHERE created_at BETWEEN :start AND :end AND status = 'DELIVERED' GROUP BY CAST(created_at AS date)) dv2 ON dv2.day_status = dates.day "
            // Failed: from order_histories + from orders.status as fallback
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day4, COUNT(DISTINCT o.id) as failed FROM orders o JOIN order_histories ohf ON ohf.order_id = o.id AND ohf.action = 'DELIVERY_FAILED_FINAL' WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) fd ON fd.day4 = dates.day "
            + "LEFT JOIN (SELECT CAST(created_at AS date) as day_fail, COUNT(*) as failed_status FROM orders WHERE created_at BETWEEN :start AND :end AND status IN ('DELIVERY_FAILED_FINAL', 'RETURN_FAILED_FINAL', 'PICKUP_FAILED_FINAL') GROUP BY CAST(created_at AS date)) fd2 ON fd2.day_fail = dates.day "
            // Returning: from order_histories + from orders.status
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day5, COUNT(DISTINCT o.id) as returning FROM orders o JOIN order_histories ohr ON ohr.order_id = o.id AND ohr.action = 'RETURNING' WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) rt ON rt.day5 = dates.day "
            // Returned: from order_histories + from orders.status
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day6, COUNT(DISTINCT o.id) as returned FROM orders o JOIN order_histories ohr2 ON ohr2.order_id = o.id AND ohr2.action = 'RETURNED' WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) rn ON rn.day6 = dates.day "
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
            safeLong(r[2])
        )).toList();
        }

    @Transactional(readOnly = true)
    public List<Object[]> reportByOfficeDetailed(LocalDateTime start, LocalDateTime end) {
        // Note: shipper_assignments table does NOT have office_id column, so we use employees table instead
        String sql = "SELECT o.id as office_id, o.name as office_name, "
            + "COALESCE(t.total_orders,0) as total_orders, "
            + "COALESCE(dv.delivered,0) as delivered, "
            + "COALESCE(fd.failed,0) as failed, "
            + "COALESCE(rn.returned,0) as returned_orders, "
            + "COALESCE((COALESCE(dv.delivered,0)),0) as dummy, "
            + "COALESCE(sf.shipping_revenue,0) as shipping_revenue, "
            + "COALESCE(psum.total_cod,0) as total_cod_collected, "
            + "COALESCE(pb.total_submitted,0) as cod_submitted_to_company, "
            + "COALESCE(emp.cnt_emp,0) as total_employees, "
            + "COALESCE(ship.cnt_shipper,0) as total_shippers "
            + "FROM offices o "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, COUNT(*) as total_orders FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY o.from_office_id) t ON t.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, COUNT(DISTINCT o.id) as delivered FROM orders o WHERE o.created_at BETWEEN :start AND :end AND o.status = 'DELIVERED' GROUP BY o.from_office_id) dv ON dv.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, COUNT(DISTINCT o.id) as failed FROM orders o WHERE o.created_at BETWEEN :start AND :end AND o.status IN ('DELIVERY_FAILED_FINAL', 'RETURN_FAILED_FINAL', 'PICKUP_FAILED_FINAL') GROUP BY o.from_office_id) fd ON fd.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, COUNT(DISTINCT o.id) as returned FROM orders o WHERE o.created_at BETWEEN :start AND :end AND o.status = 'RETURNED' GROUP BY o.from_office_id) rn ON rn.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, SUM(o.shipping_fee) as shipping_revenue FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY o.from_office_id) sf ON sf.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, SUM(ps.system_amount) as total_cod FROM payment_submissions ps JOIN orders o ON o.id = ps.order_id WHERE ps.paid_at BETWEEN :start AND :end GROUP BY o.from_office_id) psum ON psum.office_id = o.id "
            + "LEFT JOIN (SELECT b.office_id as office_id, SUM(b.total_actual_amount) as total_submitted FROM payment_submission_batches b WHERE b.status = 'COMPLETED' AND b.created_at BETWEEN :start AND :end GROUP BY b.office_id) pb ON pb.office_id = o.id "
            + "LEFT JOIN (SELECT e.office_id, COUNT(*) as cnt_emp FROM employees e GROUP BY e.office_id) emp ON emp.office_id = o.id "
            + "LEFT JOIN (SELECT e.office_id, COUNT(DISTINCT u.id) as cnt_shipper FROM employees e JOIN users u ON u.id = e.user_id JOIN account_roles ar ON ar.account_id = u.account_id JOIN roles r ON r.id = ar.role_id WHERE r.name = 'SHIPPER' GROUP BY e.office_id) ship ON ship.office_id = o.id "
            + "ORDER BY o.name";

        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows;
    }

    /**
     * Báo cáo chi tiết theo shipper.
     * Sử dụng orders.employee_id làm nguồn đếm chính để đảm bảo dữ liệu chính xác.
     * KHÔNG dùng payment_submissions hoặc order_histories làm nguồn đếm.
     */
    @Transactional(readOnly = true)
    public List<Object[]> reportByShipperDetailed(LocalDateTime start, LocalDateTime end) {
        String sql = """
            SELECT
                e.id AS employee_id,
                u.id AS user_id,
                CONCAT(u.last_name, ' ', u.first_name) AS shipper_name,
                u.phone_number,
                ofc.id AS office_id,
                ofc.name AS office_name,
                COUNT(DISTINCT o.id) AS total_orders,
                COUNT(DISTINCT CASE WHEN o.status = 'DELIVERED' THEN o.id END) AS delivered,
                COUNT(DISTINCT CASE WHEN o.status IN ('FAILED_DELIVERY', 'DELIVERY_FAILED_FINAL', 'PICKUP_FAILED_FINAL') THEN o.id END) AS failed,
                COUNT(DISTINCT CASE WHEN o.status IN ('RETURNING', 'RETURNED') THEN o.id END) AS return_count,
                (
                    COUNT(DISTINCT o.id)
                    - COUNT(DISTINCT CASE WHEN o.status = 'DELIVERED' THEN o.id END)
                    - COUNT(DISTINCT CASE WHEN o.status IN ('FAILED_DELIVERY', 'DELIVERY_FAILED_FINAL', 'PICKUP_FAILED_FINAL') THEN o.id END)
                    - COUNT(DISTINCT CASE WHEN o.status IN ('RETURNING', 'RETURNED') THEN o.id END)
                ) AS processing,
                CASE
                    WHEN COUNT(DISTINCT o.id) = 0 THEN 0.0
                    ELSE ROUND(
                        COUNT(DISTINCT CASE WHEN o.status = 'DELIVERED' THEN o.id END) * 100.0
                        / COUNT(DISTINCT o.id),
                        2
                    )
                END AS success_rate,
                COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN COALESCE(o.cod, 0) ELSE 0 END), 0) AS cod_collected
            FROM employees e
            JOIN users u ON u.id = e.user_id
            JOIN offices ofc ON ofc.id = e.office_id
            JOIN account_roles ar ON ar.id = e.account_role_id
            JOIN roles r ON r.id = ar.role_id
            LEFT JOIN orders o ON o.employee_id = e.id AND o.created_at BETWEEN :start AND :end
            WHERE UPPER(r.name) = 'SHIPPER'
            GROUP BY
                e.id, u.id, u.first_name, u.last_name, u.phone_number, ofc.id, ofc.name
            ORDER BY total_orders DESC
            """;

        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Object[]> financeReportByDay(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT CAST(d.day AS date) as day, "
            + "COALESCE(r.shipping_revenue,0) as shipping_revenue, "
            + "COALESCE(ps.total_cod,0) as cod_collected, "
            + "COALESCE(pb.total_submitted,0) as cod_submitted, "
            + "COALESCE(st.total_transferred,0) as cod_transferred_to_shop "
            + "FROM (SELECT CAST(o.created_at AS date) as day FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) d "
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day2, SUM(o.shipping_fee) as shipping_revenue FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) r ON r.day2 = d.day "
            + "LEFT JOIN (SELECT CAST(ps.paid_at AS date) as day3, SUM(ps.system_amount) as total_cod FROM payment_submissions ps WHERE ps.paid_at BETWEEN :start AND :end GROUP BY CAST(ps.paid_at AS date)) ps ON ps.day3 = d.day "
            + "LEFT JOIN (SELECT CAST(b.created_at AS date) as day4, SUM(b.total_actual_amount) as total_submitted FROM payment_submission_batches b WHERE b.status = 'COMPLETED' AND b.created_at BETWEEN :start AND :end GROUP BY CAST(b.created_at AS date)) pb ON pb.day4 = d.day "
            + "LEFT JOIN (SELECT CAST(st.paid_at AS date) as day5, SUM(st.amount) as total_transferred FROM settlement_transactions st WHERE st.type = 'SYSTEM_TO_SHOP' AND st.status = 'SUCCESS' AND st.paid_at BETWEEN :start AND :end GROUP BY CAST(st.paid_at AS date)) st ON st.day5 = d.day "
            + "ORDER BY d.day";

        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Object[]> financeReportByBranch(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT o.id as office_id, o.name as office_name, COALESCE(SUM(ord.shipping_fee),0) as shipping_revenue, COALESCE(SUM(ps.total_cod),0) as cod_collected, COALESCE(SUM(pb.total_submitted),0) as cod_submitted, COALESCE(SUM(st.total_transferred),0) as cod_transferred_to_shop "
            + "FROM offices o "
            + "LEFT JOIN orders ord ON ord.from_office_id = o.id AND ord.created_at BETWEEN :start AND :end "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, SUM(ps.system_amount) as total_cod FROM payment_submissions ps JOIN orders o ON o.id = ps.order_id WHERE ps.paid_at BETWEEN :start AND :end GROUP BY o.from_office_id) ps ON ps.office_id = o.id "
            + "LEFT JOIN (SELECT b.office_id as office_id, SUM(b.total_actual_amount) as total_submitted FROM payment_submission_batches b WHERE b.status = 'COMPLETED' AND b.created_at BETWEEN :start AND :end GROUP BY b.office_id) pb ON pb.office_id = o.id "
            + "LEFT JOIN (SELECT ord.from_office_id as office_id, SUM(st.amount) as total_transferred FROM settlement_transactions st JOIN settlement_batches sb ON sb.id = st.settlement_batch_id JOIN orders ord ON ord.settlement_batch_id = sb.id WHERE st.type = 'SYSTEM_TO_SHOP' AND st.status = 'SUCCESS' AND st.paid_at BETWEEN :start AND :end GROUP BY ord.from_office_id) st ON st.office_id = o.id "
            + "GROUP BY o.id, o.name ORDER BY shipping_revenue DESC";

        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows;
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
            safeLong(r[2]),
            safeBigDecimal(r[3]),
            safeBigDecimal(r[4])
        )).toList();
        }

    @Transactional(readOnly = true)
    public Object[] overviewSummary(LocalDateTime start, LocalDateTime end) {
        // Use orders.status as primary source for counts, fallback to order_histories only if needed
        String sql = "SELECT "
            + "(SELECT COUNT(*) FROM offices) as total_offices, "
            + "(SELECT COUNT(DISTINCT a.id) FROM accounts a JOIN account_roles ar ON ar.account_id = a.id JOIN roles r ON r.id = ar.role_id WHERE r.name = 'EMPLOYEE') as total_employees, "
            + "(SELECT COUNT(DISTINCT a.id) FROM accounts a JOIN account_roles ar ON ar.account_id = a.id JOIN roles r ON r.id = ar.role_id WHERE r.name = 'SHIPPER') as total_shippers, "
            // Total orders by created_at date range
            + "(SELECT COUNT(*) FROM orders o WHERE o.created_at BETWEEN :start AND :end) as total_orders, "
            // Delivered: use orders.status as primary, fallback to order_histories
            + "(SELECT COUNT(DISTINCT o.id) FROM orders o WHERE o.created_at BETWEEN :start AND :end AND o.status = 'DELIVERED') as delivered, "
            // Failed: combine all failure statuses
            + "(SELECT COUNT(DISTINCT o.id) FROM orders o WHERE o.created_at BETWEEN :start AND :end AND o.status IN ('DELIVERY_FAILED_FINAL', 'RETURN_FAILED_FINAL', 'PICKUP_FAILED_FINAL')) as failed, "
            // Returned: use orders.status
            + "(SELECT COUNT(DISTINCT o.id) FROM orders o WHERE o.created_at BETWEEN :start AND :end AND o.status = 'RETURNED') as returned, "
            + "COALESCE((SELECT SUM(o.shipping_fee) FROM orders o WHERE o.created_at BETWEEN :start AND :end),0) as shipping_revenue, "
            + "COALESCE((SELECT SUM(ps.system_amount) FROM payment_submissions ps WHERE ps.paid_at BETWEEN :start AND :end),0) as total_cod_collected, "
            + "COALESCE((SELECT SUM(st.amount) FROM settlement_transactions st WHERE st.type = 'SYSTEM_TO_SHOP' AND st.status = 'SUCCESS' AND st.paid_at BETWEEN :start AND :end),0) as cod_transferred "
            + "";

        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        try {
            Object[] row = (Object[]) q.getSingleResult();
            if (row == null) {
                return new Object[10];
            }
            return row;
        } catch (jakarta.persistence.NoResultException ex) {
            return new Object[10];
        } catch (RuntimeException ex) {
            // Some drivers may return List instead of single result; fall back to list path
            try {
                List<?> list = q.getResultList();
                if (list == null || list.isEmpty()) {
                    return new Object[10];
                }
                Object first = list.get(0);
                if (first instanceof Object[] arr) {
                    return arr;
                }
                // Single scalar row -> wrap to length-10 array, first value at index 0
                Object[] arr = new Object[10];
                arr[0] = first;
                return arr;
            } catch (RuntimeException inner) {
                throw inner;
            }
        }
    }

    // ----- Helpers -----

    public static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate d) {
            return d;
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.time.LocalDateTime dt) {
            return dt.toLocalDate();
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    public static Long safeLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static Integer safeInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static BigDecimal safeBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static double nzDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
