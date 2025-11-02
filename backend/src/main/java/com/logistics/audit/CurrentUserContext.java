package com.logistics.audit;

public class CurrentUserContext {

    private static final ThreadLocal<Integer> currentUser = new ThreadLocal<>();

    public static void setUserId(Integer userId) {
        currentUser.set(userId);
    }

    public static Integer getUserId() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}