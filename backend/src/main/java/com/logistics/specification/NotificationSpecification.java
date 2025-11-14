package com.logistics.specification;

import com.logistics.entity.Notification;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class NotificationSpecification {

    public static Specification<Notification> unrestrictedNotification() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Notification> userId(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Notification> isRead(Boolean isRead) {
        return (root, query, cb) -> {
            if (isRead == null)
                return null;
            return cb.equal(root.get("isRead"), isRead);
        };
    }

    public static Specification<Notification> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return null;
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), likePattern),
                    cb.like(cb.lower(root.get("message")), likePattern));
        };
    }

    public static Specification<Notification> fetchCreator() {
        return (root, query, cb) -> {
            if (query != null && Notification.class.equals(query.getResultType())) {
                root.fetch("creator", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }
}