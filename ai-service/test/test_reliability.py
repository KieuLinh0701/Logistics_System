# -*- coding: utf-8 -*-
"""
BỘ TEST MINH CHỨNG ĐỘ TIN CẬY THUẬT TOÁN — AI Service (Logistics_System)
=========================================================================
Đặt tại: ai-service/tests/test_reliability.py
Chạy từ gốc ai-service/:  python -m pytest tests/test_reliability.py -v -s

TRẠNG THÁI: đã nối vào các hàm/class thật (assign_orders_to_shippers,
solve_route_for_shipper, GoogleMapsService). Các dòng còn TODO là những
chỗ mình đoán theo tài liệu kỹ thuật vì chưa thấy code thật — BẮT BUỘC
đối chiếu lại với chữ ký hàm/tên tham số/tên trường thật trong repo
trước khi coi kết quả chạy là bằng chứng hợp lệ.
"""

import itertools
import math
import random
import time
import statistics
import pytest

from app.services.area_matcher import assign_orders_to_shippers
from app.services.ortools_optimizer import solve_route_for_shipper
from app.services.google_maps_service import GoogleMapsService
from app.services.route_optimizer_service import RouteOptimizerService
from app.main import app
from fastapi.testclient import TestClient


# =============================================================================
# TIỆN ÍCH DÙNG CHUNG — đây là hàm HELPER để sinh dữ liệu mẫu cho test,
# KHÔNG phải code của hệ thống thật, giữ nguyên không cần sửa.
# =============================================================================

def haversine_km(a, b):
    """Khoảng cách đường chim bay giữa 2 điểm (lat, lng), đơn vị km."""
    R = 6371.0
    lat1, lon1 = math.radians(a[0]), math.radians(a[1])
    lat2, lon2 = math.radians(b[0]), math.radians(b[1])
    dlat, dlon = lat2 - lat1, lon2 - lon1
    h = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 2 * R * math.asin(math.sqrt(h))


def random_points(n, seed, center=(10.8, 106.6), spread=0.05):
    """Sinh N điểm ngẫu nhiên quanh 1 tâm (mặc định khu vực TP.HCM)."""
    rnd = random.Random(seed)
    return [
        (center[0] + rnd.uniform(-spread, spread), center[1] + rnd.uniform(-spread, spread))
        for _ in range(n)
    ]


def build_duration_matrix_haversine(points, speed_kmh=30):
    """Chỉ dùng để TẠO DỮ LIỆU MẪU (ma trận thời gian) cho các test không
    liên quan tới logic fallback — ví dụ TC-04 (optimality gap), TC-09
    (hiệu năng). KHÔNG dùng hàm này để test nhóm Fallback (TC-05..07),
    vì nó không hề biết gì về api_key / lỗi API — đó là logic nằm trong
    GoogleMapsService thật, phải gọi thẳng class đó."""
    n = len(points)
    matrix = [[0] * n for _ in range(n)]
    for i in range(n):
        for j in range(n):
            if i != j:
                km = haversine_km(points[i], points[j])
                matrix[i][j] = km / speed_kmh * 3600
    return matrix


def route_cost(matrix, order):
    """Tổng chi phí (giây) của 1 thứ tự ghé thăm, xuất phát và không quay lại điểm 0."""
    return sum(matrix[order[i]][order[i + 1]] for i in range(len(order) - 1))


def brute_force_optimal(matrix):
    """Lời giải TỐI ƯU TUYỆT ĐỐI thật sự — thử toàn bộ hoán vị.
    Điểm 0 = kho xuất phát, cố định ở đầu. Chỉ dùng được với N nhỏ (<=8-9)."""
    n = len(matrix)
    best = None
    for perm in itertools.permutations(range(1, n)):
        order = [0] + list(perm)
        c = route_cost(matrix, order)
        if best is None or c < best[0]:
            best = (c, order)
    return best  # (cost, order)


# =============================================================================
# NHÓM A — RÀNG BUỘC NGHIỆP VỤ CỨNG (area_matcher.py)
# =============================================================================

class TestHardConstraints:

    def test_TC01_shipper_khong_vuot_capacity(self):
        """1 shipper capacity=3, 5 đơn cùng khu vực -> chỉ nhận đúng 3 đơn."""
        # TODO: đối chiếu format thật của "shipper" và "order" — repo có thể
        # dùng Pydantic model chứ không phải dict thuần như dưới đây.
        shipper = {"id": "S1", "capacity": 3, "assigned": []}
        orders = [{"id": f"O{i}", "ward": "P.Ben_Nghe"} for i in range(5)]

        assigned, unassigned = assign_orders_to_shippers(orders, [shipper])

        assert len(assigned["S1"]) == 3, "Shipper không được nhận vượt quá capacity"
        assert len(unassigned) == 2
        assert all(u["reason"] == "CAPACITY_EXCEEDED" for u in unassigned)

    def test_TC02_don_khong_dung_khu_vuc_bi_loai(self):
        """Đơn không thuộc khu vực phụ trách của bất kỳ shipper nào -> unassigned NO_MATCHING_AREA."""
        shipper = {"id": "S1", "capacity": 10, "wards": {"P.Ben_Nghe"}, "assigned": []}
        orders = [{"id": "O1", "ward": "P.Tan_Dinh"}]  # ngoài khu vực

        assigned, unassigned = assign_orders_to_shippers(orders, [shipper])

        assert len(assigned["S1"]) == 0
        assert unassigned[0]["reason"] == "NO_MATCHING_AREA"

    def test_TC03_diem_khong_vuot_max_weight(self):
        """Tổng tải trọng các đơn gán cho 1 shipper không được vượt max_weight_kg."""
        shipper = {"id": "S1", "capacity": 10, "max_weight_kg": 20, "assigned": []}
        orders = [{"id": "O1", "weight_kg": 15}, {"id": "O2", "weight_kg": 10}]

        assigned, unassigned = assign_orders_to_shippers(orders, [shipper])
        total_weight = sum(o["weight_kg"] for o in assigned["S1"])

        assert total_weight <= shipper["max_weight_kg"]
        assert any(u["reason"] == "WEIGHT_CAPACITY_EXCEEDED" for u in unassigned)


# =============================================================================
# NHÓM B — OPTIMALITY GAP THỰC NGHIỆM (ortools_optimizer.py) — QUAN TRỌNG NHẤT
# =============================================================================

class TestOptimalityGap:
    """So sánh lời giải OR-Tools (Guided Local Search, time_limit=8s — đúng cấu
    hình thật trong settings.py) với lời giải brute-force (tối ưu tuyệt đối
    thật sự) trên bộ dữ liệu nhỏ."""

    @pytest.mark.parametrize("n_points", [5, 6, 7, 8])
    def test_TC04_gap_trung_binh_theo_N(self, n_points):
        n_runs = 20
        gaps = []
        solve_times = []

        for seed in range(n_runs):
            points = random_points(n_points, seed=seed)
            matrix = build_duration_matrix_haversine(points)

            optimal_cost, _ = brute_force_optimal(matrix)

            t0 = time.perf_counter()
            # TODO: kiểm tra chữ ký thật — có thể cần thêm capacity=..., depot_index=...
            ortools_cost, _ = solve_route_for_shipper(matrix, time_limit_seconds=8)
            solve_times.append(time.perf_counter() - t0)

            gap = 0.0 if optimal_cost == 0 else (ortools_cost - optimal_cost) / optimal_cost * 100
            gaps.append(gap)

            assert ortools_cost >= optimal_cost - 1e-6, \
                "OR-Tools không được cho lời giải rẻ hơn lời giải tối ưu tuyệt đối"

        avg_gap = statistics.mean(gaps)
        max_gap = max(gaps)
        avg_time = statistics.mean(solve_times)

        print(f"\n[N={n_points}] gap_trung_binh={avg_gap:.2f}%  gap_max={max_gap:.2f}%  "
              f"thoi_gian_giai_tb={avg_time*1000:.1f}ms  (so voi brute-force tren {n_runs} lan chay)")

        assert avg_gap < 5.0, "Gap trung bình vượt ngưỡng chấp nhận được cho bộ dữ liệu nhỏ"


# =============================================================================
# NHÓM C — FALLBACK 3 LỚP (google_maps_service.py)
# Lưu ý quan trọng: đây là logic nằm trong GoogleMapsService thật, KHÔNG
# dùng build_duration_matrix_haversine() ở trên để test nhóm này.
# =============================================================================

class TestFallback:

    def test_TC05_khong_co_api_key_dung_haversine(self):
        """Không cấu hình GOOGLE_MAPS_API_KEY -> tự động dùng Haversine, không lỗi."""
        points = random_points(4, seed=1)
        service = GoogleMapsService(api_key=None)  # TODO: đúng tên tham số __init__ thật
        matrix, source = service.build_duration_matrix(points)

        assert source == "HAVERSINE_FALLBACK"  # TODO: đúng giá trị enum/string thật trả về
        assert len(matrix) == 4 and all(len(row) == 4 for row in matrix)

    def test_TC06_api_loi_toan_phan_fallback_toan_bo(self, monkeypatch):
        """Google API raise lỗi (timeout/quota) -> toàn bộ ma trận fallback Haversine,
        service KHÔNG được crash / trả 500."""
        points = random_points(4, seed=2)
        service = GoogleMapsService(api_key="fake-valid-key")

        # TODO: patch đúng chỗ code thật gọi ra ngoài, ví dụ nếu bên trong
        # service có self.client.distance_matrix(...):
        def _raise(*args, **kwargs):
            raise TimeoutError("giả lập Google API timeout")
        monkeypatch.setattr(service.client, "distance_matrix", _raise)

        matrix, source = service.build_duration_matrix(points)

        assert source == "HAVERSINE_FALLBACK"
        assert matrix is not None

    def test_TC07_loi_mot_phan_dung_mixed(self, monkeypatch):
        """Một vài cặp điểm cụ thể lỗi (status != OK) -> chỉ fallback đúng
        các cặp đó, giữ nguyên kết quả Google cho các cặp còn lại."""
        points = random_points(4, seed=3)
        service = GoogleMapsService(api_key="fake-valid-key")

        # TODO: mock response trả về có 1 phần tử status != "OK" đúng format
        # thật của googlemaps.distance_matrix (cần xem code thật để mock đúng)

        matrix, source = service.build_duration_matrix(points)
        assert source == "MIXED_WITH_FALLBACK"


# =============================================================================
# NHÓM D — ETA CỘNG DỒN THEO CHẶNG (không phụ thuộc code thật, kiểm tra khái niệm)
# =============================================================================

class TestETA:

    def test_TC08_leg_based_chinh_xac_hon_chia_deu(self):
        leg_durations_sec = [60, 60, 1200]
        total = sum(leg_durations_sec)
        n_stops = len(leg_durations_sec)

        leg_based_eta = []
        running = 0
        for d in leg_durations_sec:
            running += d
            leg_based_eta.append(running)

        naive_eta = [total / n_stops * (i + 1) for i in range(n_stops)]

        assert leg_based_eta[0] == 60
        assert abs(naive_eta[0] - leg_based_eta[0]) > 300


# =============================================================================
# NHÓM E — HIỆU NĂNG THỜI GIAN THỰC
# =============================================================================

class TestPerformance:

    @pytest.mark.parametrize("n_points", [10, 20, 30, 50])
    def test_TC09_thoi_gian_phan_hoi_trong_nguong(self, n_points):
        points = random_points(n_points, seed=42)
        matrix = build_duration_matrix_haversine(points)

        t0 = time.perf_counter()
        solve_route_for_shipper(matrix, time_limit_seconds=8)
        elapsed = time.perf_counter() - t0

        print(f"\n[N={n_points}] thoi gian giai = {elapsed:.2f}s")
        assert elapsed <= 9.0


# =============================================================================
# NHÓM F — END-TO-END QUA API THẬT
# =============================================================================

class TestEndToEnd:

    def test_TC10_goi_api_that_tong_don_khop(self):
        client = TestClient(app)
        payload = {}  # TODO: điền payload mẫu đúng schema RouteOptimizationRequest thật
        resp = client.post("/api/v1/optimization/route", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        total_out = sum(len(r["stops"]) for r in data["routes"]) + len(data["unassigned"])
        assert total_out == len(payload.get("orders", []))


if __name__ == "__main__":
    import sys
    sys.exit(pytest.main([__file__, "-v", "-s"]))