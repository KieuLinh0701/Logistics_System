# Logistics_System
Ứng dụng công nghệ Spring Boot và ReactJS để xây dựng website hỗ trợ quản lý và theo dõi vận chuyển

## Giới thiệu dự án

Dự án "Logistics System" là một hệ thống quản lý vận tải và giao nhận cung cấp các chức năng quản lý đơn hàng, vận đơn, tài xế, shipper, bảng kê thanh toán, báo cáo và thông báo theo thời gian thực.

## Mục tiêu dự án

- Quản lý luồng vận chuyển và đơn hàng hiệu quả
- Hỗ trợ phân công tài xế/shipper, theo dõi trạng thái giao nhận
- Tạo báo cáo, xử lý thanh toán và lịch trình thanh toán
- Cập nhật thông báo, sự cố và theo dõi thời gian thực

## Tính năng chính

- Quản lý người dùng, nhân viên, shipper
- Tạo và quản lý đơn hàng, shipment, shipment order
- Phân công shipper/tài xế và cập nhật trạng thái
- Quản lý phí vận chuyển, khuyến mãi, dịch vụ
- Quản lý thanh toán, đối soát và lịch thanh toán
- Báo cáo thống kê và bảng điều khiển
- Gửi thông báo thời gian thực qua WebSocket
- Upload tài liệu/ảnh (Cloudinary) và xuất báo cáo (Excel/PDF)

## Công nghệ sử dụng

**Backend**
- Java 21, Spring Boot 3.x
- Spring Data JPA, Spring Security
- Web, WebSocket, Mail
- jjwt (JWT), Lombok, Hibernate Envers
- MySQL (mysql-connector-j)
- Cloudinary, Apache POI, ZXing

**Frontend**
- React 18, TypeScript, Vite
- Ant Design, react-router, react-redux
- axios, stompjs/sockjs cho WebSocket

**Database**
- MySQL (schema: `logistics`)

## Development Tools

- Maven (wrapper included)
- Node.js & npm
- Vite, ESLint, TypeScript
- IDEs: IntelliJ/VS Code

## Hướng dẫn cài đặt

### Yêu cầu hệ thống

- Java 21
- Maven (hoặc sử dụng wrapper `mvnw` / `mvnw.cmd` có sẵn trong `backend`)
- Node.js (v18+ được khuyến nghị) và `npm`
- MySQL server

### Bước 1: Clone repository

```bash
git clone https://github.com/KieuLinh0701/Logistics_System
cd Logistics_System
```

### Bước 2: Cài đặt dependencies

- Backend (sử dụng Maven wrapper)

Windows:

```powershell
cd backend
.\mvnw.cmd clean install -DskipTests
```

- Frontend

```bash
cd frontend
npm install
```

### Bước 3: Cấu hình database

1. Tạo database MySQL (ví dụ tên `logistics`):

```bash
mysql -u root -p -e "CREATE DATABASE logistics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### Bước 4: Tạo file `application.properties`

Tạo file `application.properties` trong `backend/src/main/resources/` (hoặc cấu hình tương đương bằng biến môi trường). Ví dụ nội dung mẫu:

```
spring.datasource.url=jdbc:mysql://localhost:3306/logistics?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_db_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.port=8080

# JWT và các thông số khác
# jwt.secret=your_jwt_secret
# vnpay, mail ...
```

### Bước 5: Khởi chạy ứng dụng

- Chạy backend server

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

- Chạy frontend (mở terminal mới)

```bash
cd frontend
npm run dev
```

Frontend mặc định chạy trên http://localhost:5173, backend mặc định trên http://localhost:8080.


