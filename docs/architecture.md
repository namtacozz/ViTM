# Kiến trúc Hệ thống VITB - VITBANK ViTM (Chuẩn Maven)

## 1. Phân tầng ứng dụng (`com.vietbank.atm`)
- **`config/`**: `AppConfig` (đọc `application.properties`), `DbConfig` (quản lý kết nối MySQL, `db.properties`, khởi tạo schema + seed).
- **`state/`**: `ATMState` (máy trạng thái 26 trạng thái), `SessionManager` (phiên làm việc, đếm ngược timeout).
- **`model/`**: Các thực thể `Account`, `Card`, `BankInfo`, `CashBreakdown`, `CashVault`, `TransactionRecord`, `TransactionResult`, `LoginResult`.
- **`dao/`**: Tầng truy xuất JDBC thuần (`AccountDao`, `CardDao`, `TransactionDao`, `CashDao`, `AuditLogDao`).
- **`service/`**: Tầng nghiệp vụ (`AuthService`, `WithdrawService`, `DepositService`, `TransferService`, `PinService`, `CashDispenser`, `ATMSystemService`), sử dụng JDBC Transaction (`commit/rollback`).
- **`ui/`**: `MainFrame` (Bezel 4:3, 8 phím mềm FDK, `CardLayout`), `ui/component/` (`HardwareSlotPanel`, `ATMDialogs`), `ui/theme/` (`Colors`, `Fonts`, `Theme`).
- **`util/`**: `I18n` (`ResourceBundle` VI/EN), `Hasher` (SHA-256 & kiểm tra PIN yếu), `MoneyFormat` (che STK/thẻ, bỏ dấu tiếng Việt).
