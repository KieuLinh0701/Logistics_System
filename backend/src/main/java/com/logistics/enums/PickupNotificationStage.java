package com.logistics.enums;

public enum PickupNotificationStage {
    NONE,       // Chưa ping lần nào
    STAGE_1,    // Đã ping sau 15p (ward + city match)
    STAGE_2,    // Đã ping sau 60p (toàn bộ city)
    URGENT      // Sau 90p, chuyển vào trang Manager xử lý
}