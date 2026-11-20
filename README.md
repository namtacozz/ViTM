# 🏧 VIETBANK ATM SIMULATOR (DESKTOP) — CHUẨN KI-ỐT VIETBANK (CAM ViT)

Hệ thống mô phỏng cây rút tiền tự động (ATM) chuẩn ngân hàng Việt Nam, xây dựng trên nền tảng **Java Swing thuần** và **MySQL**, tuân thủ cấu trúc tinh gọn 3 file nguồn, không chia package, mã nguồn sạch không comment thừa.

### 🎨 Bộ nhận diện thương hiệu Cam ViT
- **Màu chủ đạo (Cam ViT)**: `#E0562D` (Header, nút xác nhận, header bảng, viền nhấn thương hiệu).
- **Màu nền ứng dụng**: Nền kem ấm `#FAF8F5` tạo chiều sâu tách lớp rõ rệt với khung thẻ trắng tinh khiết `#FFFFFF`.
- **Chữ chính / Đen xám tối (Dark Charcoal)**: `#18181B` (Hiển thị văn bản, số tiền, thông tin tài khoản sắc nét không ánh xanh).
- **Nền cam nhạt (nhãn/thẻ)**: `#FFF7ED` (viền `#FED7AA`) (Dùng cho thẻ thông tin, highlight số dư, trạng thái active input).
- **Viền mặc định ô nhập liệu**: `#D4D4D8` (xám trung tính rõ ranh giới) và tự động chuyển sang `#DC2626` (đỏ) khi có lỗi.

---

## 1. Cấu Trúc Dự Án

```
f:\AppMaking\ViTM\
├── schema.sql              # CSDL, bảng accounts, cards, transactions & dữ liệu mẫu (SHA-256 kèm Salt)
├── DB.java                 # Toàn bộ logic JDBC, Transaction, SELECT ... FOR UPDATE, nạp tiền, chia cơ số tiền
├── ATM.java                # Giao diện Ki-ốt JFrame 980x700, Menu 4 trái - 4 phải đối xứng, Numpad 3 màu
├── mysql-connector-j.jar   # Driver JDBC MySQL
├── init_db.bat             # Script chạy nạp schema.sql vào MySQL 1-click
├── run.bat                 # Script chạy ứng dụng ATM 1-click
├── screenshots/            # Bộ 17 ảnh chụp màn hình giao diện ki-ốt thực tế (Tone Cam ViT)
├── docs/                   # Báo cáo học phần hoàn chỉnh (HTML & PDF 18+ trang chuẩn Tone Cam ViT)
```

---

## 2. Các Tính Năng & Màn Hình Giao Diện Đã Hoàn Thiện

1. **Màn hình Chờ (`WAIT`)**: Nền tối Đen xám (`#18181B`), đồng hồ thời gian thực Cam ViT (`#E0562D`), nút chuyển ngữ song ngữ `VI | EN`, logo ATM vector, nút đưa thẻ nổi bật.
2. **Màn hình Nhập thẻ (`CARD`)**: Nền kem ấm, định dạng số thẻ nhóm 4 số (`9704 0012 3456 7890`), 3 nút thẻ mẫu nền cam nhạt (`#FFF7ED` viền `#FDBA74`), con trỏ caret ở cuối chuỗi.
3. **Màn hình Nhập PIN (`PIN`)**: Tiêu đề rút gọn `XÁC THỰC MÃ PIN`, `JPasswordField` cỡ chữ 30px viền Cam ViT, khung 360px cân đối, cụm Numpad chuẩn 3 màu tách biệt: **HỦY** (đỏ), **XÓA** (vàng hổ phách), **ĐỒNG Ý** (Cam ViT `#E0562D`).
4. **Màn hình Menu chính (`MENU`)**: Lưới **4 trái – 4 phải đối xứng** chuẩn ki-ốt ngân hàng:
   - *Cột trái*: Rút tiền mặt, Nạp tiền mặt, Chuyển khoản, Xem số dư (căn lề trái kèm `‹`).
   - *Cột phải*: Đổi mã PIN, Sao kê mini, Rút nhanh 500k, Đăng xuất / Trả thẻ (căn lề phải kèm `›`).
   - *Tâm điểm giữa*: Mô phỏng phôi thẻ EMV dập nổi sang trọng, Lời chào trân trọng, Tên chủ thẻ và Số tài khoản.
5. **Màn hình Xem số dư (`BALANCE`)**: Thẻ thông tin tài khoản riêng biệt nền `#FFF7ED` viền `#FED7AA`, số dư khả dụng cỡ chữ 26px màu Cam ViT nổi bật.
6. **Màn hình Rút tiền (`WITHDRAW`)**: 6 mệnh giá cố định, ô nhập số khác thu gọn 1 dòng căn phải (48px) có `DocumentFilter` tự động chèn dấu chấm phân cách hàng nghìn.
7. **Màn hình Nạp tiền mặt (`DEPOSIT`)**: Tính năng nạp tiền mặt trực tiếp, kiểm soát bội số 50k và hạn mức 50.000.000đ/lần.
8. **Màn hình Xử lý giao dịch (`PROCESSING`)**: Thông điệp an toàn động theo từng nghiệp vụ (rút tiền, nạp tiền, chuyển khoản, đổi PIN), thanh tiến trình Cam ViT.
9. **Màn hình Hỏi in biên lai (`ASK_RECEIPT`)**: Thông báo giao dịch thành công, nút in biên lai Cam ViT, nút không in viền Secondary Outline và bảng hiển thị cơ số tờ tiền nhả ra (500k, 200k, 100k, 50k theo thuật toán tham lam).
10. **Màn hình Chuyển khoản (`TRANSFER`)**: Kiểm tra tên người thụ hưởng tức thì, ô nhập số tiền highlight Cam ViT trên nền cam nhạt.
11. **Màn hình Xác nhận chuyển khoản (`CONFIRM_TRANSFER`)**: Đối soát người gửi, người nhận, số tiền màu Cam ViT `#E0562D`, phí giao dịch 0 VNĐ, nút XÁC NHẬN CHUYỂN Cam ViT đồng bộ.
12. **Màn hình Đổi mã PIN (`CHANGE_PIN`)**: 3 ô bảo mật kèm bàn phím số ảo, thống nhất phím ĐỒNG Ý Cam ViT.
13. **Màn hình Sao kê mini (`STATEMENT`)**: Bảng `JTable` 5 cột rõ nét, thanh tóm tắt thông tin tài khoản phía trên, header căn phải đồng bộ với số tiền, bổ sung nút **XUẤT SAO KÊ (.TXT)** trực tiếp.
14. **Màn hình Hóa đơn in nhiệt (`RECEIPT`)**: Mô phỏng giấy in nhiệt 80mm: Mã máy ATM (`VB-DN-086`), Mã GD, chi tiết cơ số tờ tiền nhả ra, phí 0đ, nút lưu file `.txt` thực tế.
15. **Màn hình Cảnh báo nhập sai PIN (`PIN Error`)**: Bọc HTML xuống dòng không bị cắt chữ, tự động xóa trắng ô PIN và đổi sang viền đỏ `#DC2626`.
16. **Màn hình Cảnh báo vượt hạn mức (`WITHDRAW Error`)**: Nhãn đỏ báo lỗi tại chỗ và viền đỏ ô nhập tiền.
17. **Màn hình Chế độ tiếng Anh (`English Bilingual`)**: Song ngữ 100% chuẩn ngân hàng quốc tế, trạng thái CSDL xanh lục `● DB: Ready`.

---

## 3. Hướng Dẫn Kết Nối CSDL MySQL (Xử Lý Lỗi "Thẻ Không Tồn Tại")

> [!IMPORTANT]
> Nếu thấy thông báo **"Số thẻ không tồn tại"** hoặc **"Lỗi kết nối CSDL"**, đó là do MySQL trên máy chưa được cấp mật khẩu kết nối hoặc chưa khởi tạo database `atm_db`.

Có 2 cách kích hoạt kết nối cực kỳ nhanh chóng:
- **Cách 1 (Ngay trên giao diện ATM - Khuyên dùng)**:
  1. Khởi chạy ATM, nhìn xuống góc dưới bên trái màn hình thấy dòng chữ: `● CSDL: Chưa kết nối (Nhấp để cấu hình)`.
  2. Nhấp chuột vào dòng chữ này, một hộp thoại hiện lên: nhập `MySQL User: root` và `MySQL Password` của máy Ngài.
  3. Bấm **OK**. Hệ thống sẽ tự động tạo cơ sở dữ liệu `atm_db`, tạo 3 bảng và nạp sẵn 3 thẻ mẫu, sau đó chuyển sang xanh `● CSDL: Sẵn sàng`.
- **Cách 2 (Qua dòng lệnh)**:
  - Chạy tệp `init_db.bat` và nhập mật khẩu root của MySQL khi được hỏi.

---

## 3. Dữ Liệu Thử Nghiệm Sẵn Có

| STT | Số thẻ | Số tài khoản | Tên chủ thẻ | Mã PIN gốc | Số dư ban đầu | Trạng thái |
|---|---|---|---|---|---|---|
| 1 | `9704 0012 3456 7890` | `1234567890` | NGUYỄN VĂN AN | `123456` | 50.000.000 VNĐ | ACTIVE (Thử nghiệm rút, nạp, chuyển, đổi PIN) |
| 2 | `9704 0098 7654 3210` | `0987654321` | TRẦN THỊ BÍCH | `654321` | 1.000.000 VNĐ | ACTIVE (Thử lỗi vượt quá số dư) |
| 3 | `9704 0011 1122 2233` | `1111222233` | LÊ HOÀNG MINH | `111111` | 30.000.000 VNĐ | LOCKED (Thử lỗi thẻ đã bị khóa) |

---

## 4. Cách Khởi Chạy

```bash
# Biên dịch UTF-8
javac -encoding UTF-8 -cp "mysql-connector-j.jar;." DB.java ATM.java

# Khởi chạy chế độ Ki-ốt
java -Dfile.encoding=UTF-8 -cp "mysql-connector-j.jar;." ATM
```
*Phím tắt quản trị thoát khẩn cấp ki-ốt:* **`Ctrl + Shift + Q`**.
