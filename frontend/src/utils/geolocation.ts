/**
 * Helper lấy vị trí GPS hiện tại của user.
 *
 * - Chỉ gọi 1 lần (không tracking liên tục).
 * - Timeout cứng 4 giây để không chặn luồng tải danh sách.
 * - Trả về `null` nếu user từ chối / timeout / trình duyệt không hỗ trợ /
 *   tọa độ không hợp lệ (0,0 / ngoài range).
 */

export interface CurrentPosition {
    latitude: number;
    longitude: number;
}

export function getCurrentPositionOnce(timeoutMs = 4000): Promise<CurrentPosition | null> {
    return new Promise((resolve) => {
        if (typeof navigator === "undefined" || !navigator.geolocation) {
            resolve(null);
            return;
        }

        const timer = setTimeout(() => {
            resolve(null);
        }, timeoutMs);

        navigator.geolocation.getCurrentPosition(
            (pos) => {
                clearTimeout(timer);
                const lat = pos?.coords?.latitude;
                const lng = pos?.coords?.longitude;
                if (
                    typeof lat !== "number" ||
                    typeof lng !== "number" ||
                    !Number.isFinite(lat) ||
                    !Number.isFinite(lng) ||
                    (lat === 0 && lng === 0) ||
                    lat < -90 || lat > 90 ||
                    lng < -180 || lng > 180
                ) {
                    resolve(null);
                    return;
                }
                resolve({latitude: lat, longitude: lng});
            },
            () => {
                clearTimeout(timer);
                resolve(null);
            },
            {
                enableHighAccuracy: false,
                maximumAge: 30_000,
                timeout: timeoutMs,
            }
        );
    });
}