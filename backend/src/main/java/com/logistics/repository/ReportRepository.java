package com.logistics.repository;

import com.logistics.dto.admin.AdminFinancialPoint;
import com.logistics.dto.admin.AdminShipperReportDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

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
            + "LEFT JOIN (SELECT CAST(o.created_at AS date) as day4, COUNT(*) as failed FROM orders o JOIN order_histories ohf ON ohf.order_id = o.id AND ohf.action = 'DELIVERY_FAILED_FINAL' WHERE o.created_at BETWEEN :start AND :end GROUP BY CAST(o.created_at AS date)) fd ON fd.day4 = dates.day "
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
    public List<Object[]> reportByOfficeDetailed(LocalDateTime start, LocalDateTime end) {
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
            + "LEFT JOIN (SELECT o.from_office_id as office_id, COUNT(DISTINCT o.id) as delivered FROM orders o JOIN order_histories oh ON oh.order_id = o.id AND oh.action = 'DELIVERED' WHERE o.created_at BETWEEN :start AND :end GROUP BY o.from_office_id) dv ON dv.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, COUNT(DISTINCT o.id) as failed FROM orders o JOIN order_histories ohf ON ohf.order_id = o.id AND ohf.action = 'DELIVERY_FAILED_FINAL' WHERE o.created_at BETWEEN :start AND :end GROUP BY o.from_office_id) fd ON fd.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, COUNT(DISTINCT o.id) as returned FROM orders o JOIN order_histories ohr ON ohr.order_id = o.id AND ohr.action = 'RETURNED' WHERE o.created_at BETWEEN :start AND :end GROUP BY o.from_office_id) rn ON rn.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, SUM(o.shipping_fee) as shipping_revenue FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY o.from_office_id) sf ON sf.office_id = o.id "
            + "LEFT JOIN (SELECT o.from_office_id as office_id, SUM(ps.system_amount) as total_cod FROM payment_submissions ps JOIN orders o ON o.id = ps.order_id WHERE ps.paid_at BETWEEN :start AND :end GROUP BY o.from_office_id) psum ON psum.office_id = o.id "
            + "LEFT JOIN (SELECT b.office_id as office_id, SUM(b.total_actual_amount) as total_submitted FROM payment_submission_batches b WHERE b.status = 'COMPLETED' AND b.created_at BETWEEN :start AND :end GROUP BY b.office_id) pb ON pb.office_id = o.id "
            + "LEFT JOIN (SELECT e.office_id, COUNT(*) as cnt_emp FROM employees e GROUP BY e.office_id) emp ON emp.office_id = o.id "
            + "LEFT JOIN (SELECT sa.office_id, COUNT(DISTINCT sa.shipper_id) as cnt_shipper FROM shipper_assignments sa GROUP BY sa.office_id) ship ON ship.office_id = o.id "
            + "ORDER BY o.name";

        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Object[]> reportByShipperDetailed(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT u.id as shipper_id, CONCAT(u.last_name, ' ', u.first_name) as shipper_name, u.phone_number, ofc.name as branch_name, "
            + "COALESCE(t.total_orders,0) as total_orders, COALESCE(dv.delivered,0) as delivered, COALESCE(fd.failed,0) as failed, COALESCE(rn.returned,0) as returned_orders, "
            + "COALESCE(sf.inprogress,0) as in_progress, "
            + "COALESCE(psum.total_cod,0) as cod_collected, COALESCE(pb.total_submitted,0) as cod_submitted_to_company "
            + "FROM users u "
            + "LEFT JOIN employees e ON e.user_id = u.id "
            + "LEFT JOIN offices ofc ON ofc.id = e.office_id "
            + "LEFT JOIN (SELECT o.shipper_id, COUNT(*) as total_orders FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY o.shipper_id) t ON t.shipper_id = u.id "
            + "LEFT JOIN (SELECT o.shipper_id, COUNT(DISTINCT o.id) as delivered FROM orders o JOIN order_histories oh ON oh.order_id = o.id AND oh.action = 'DELIVERED' WHERE o.created_at BETWEEN :start AND :end GROUP BY o.shipper_id) dv ON dv.shipper_id = u.id "
            + "LEFT JOIN (SELECT o.shipper_id, COUNT(DISTINCT o.id) as failed FROM orders o JOIN order_histories ohf ON ohf.order_id = o.id AND ohf.action = 'DELIVERY_FAILED_FINAL' WHERE o.created_at BETWEEN :start AND :end GROUP BY o.shipper_id) fd ON fd.shipper_id = u.id "
            + "LEFT JOIN (SELECT o.shipper_id, COUNT(DISTINCT o.id) as returned FROM orders o JOIN order_histories ohr ON ohr.order_id = o.id AND ohr.action = 'RETURNED' WHERE o.created_at BETWEEN :start AND :end GROUP BY o.shipper_id) rn ON rn.shipper_id = u.id "
            + "LEFT JOIN (SELECT o.shipper_id, SUM(CASE WHEN o.status = 'DELIVERING' THEN 1 ELSE 0 END) as inprogress FROM orders o WHERE o.created_at BETWEEN :start AND :end GROUP BY o.shipper_id) sf ON sf.shipper_id = u.id "
            + "LEFT JOIN (SELECT ps.shipper_id, SUM(ps.system_amount) as total_cod FROM payment_submissions ps WHERE ps.paid_at BETWEEN :start AND :end GROUP BY ps.shipper_id) psum ON psum.shipper_id = u.id "
            + "LEFT JOIN (SELECT b.shipper_id, SUM(b.total_actual_amount) as total_submitted FROM payment_submission_batches b WHERE b.status = 'COMPLETED' AND b.created_at BETWEEN :start AND :end GROUP BY b.shipper_id) pb ON pb.shipper_id = u.id "
            + "WHERE EXISTS (SELECT 1 FROM account_roles ar JOIN roles r ON r.id = ar.role_id WHERE ar.account_id = u.account_id AND r.name = 'SHIPPER') "
            + "ORDER BY total_orders DESC";

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
            r[2] == null ? 0L : ((Number) r[2]).longValue(),
            r[3] == null ? BigDecimal.ZERO : new BigDecimal(r[3].toString()),
            r[4] == null ? BigDecimal.ZERO : new BigDecimal(r[4].toString())
        )).toList();
        }

    @Transactional(readOnly = true)
    public Object[] overviewSummary(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT "
            + "(SELECT COUNT(*) FROM offices) as total_offices, "
            + "(SELECT COUNT(DISTINCT a.id) FROM accounts a JOIN account_roles ar ON ar.account_id = a.id JOIN roles r ON r.id = ar.role_id WHERE r.name = 'EMPLOYEE') as total_employees, "
            + "(SELECT COUNT(DISTINCT a.id) FROM accounts a JOIN account_roles ar ON ar.account_id = a.id JOIN roles r ON r.id = ar.role_id WHERE r.name = 'SHIPPER') as total_shippers, "
            + "(SELECT COUNT(*) FROM orders o WHERE o.created_at BETWEEN :start AND :end) as total_orders, "
            + "(SELECT COUNT(DISTINCT o.id) FROM orders o JOIN order_histories oh ON oh.order_id = o.id AND oh.action = 'DELIVERED' WHERE o.created_at BETWEEN :start AND :end) as delivered, "
            + "(SELECT COUNT(DISTINCT o.id) FROM orders o JOIN order_histories ohf ON ohf.order_id = o.id AND ohf.action = 'DELIVERY_FAILED_FINAL' WHERE o.created_at BETWEEN :start AND :end) as failed, "
            + "(SELECT COUNT(DISTINCT o.id) FROM orders o JOIN order_histories ohr ON ohr.order_id = o.id AND ohr.action = 'RETURNED' WHERE o.created_at BETWEEN :start AND :end) as returned, "
            + "COALESCE((SELECT SUM(o.shipping_fee) FROM orders o WHERE o.created_at BETWEEN :start AND :end),0) as shipping_revenue, "
            + "COALESCE((SELECT SUM(ps.system_amount) FROM payment_submissions ps WHERE ps.paid_at BETWEEN :start AND :end),0) as total_cod_collected, "
            + "COALESCE((SELECT SUM(st.amount) FROM settlement_transactions st WHERE st.type = 'SYSTEM_TO_SHOP' AND st.status = 'SUCCESS' AND st.paid_at BETWEEN :start AND :end),0) as cod_transferred "
            + "";

        Query q = em.createNativeQuery(sql);
        q.setParameter("start", start);
        q.setParameter("end", end);
        Object[] row = (Object[]) q.getSingleResult();
        return row;
    }
}
