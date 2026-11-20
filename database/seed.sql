USE atm_db;

INSERT INTO atm_config (config_key, config_value) VALUES
  ('atm_id', 'VITB-DN-086'),
  ('atm_location', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng'),
  ('atm_status', 'ONLINE');

INSERT INTO atm_vault (menh_gia, so_to) VALUES
  (500000, 100),
  (200000, 150),
  (100000, 200),
  (50000, 200);

INSERT INTO accounts (id, so_tai_khoan, ten_chu_tk, so_du) VALUES
  (1, '1234567890', 'NGUYỄN VĂN AN', 50000000),
  (2, '0987654321', 'TRẦN THỊ BÍCH', 1000000),
  (3, '1111222233', 'LÊ HOÀNG MINH', 30000000);

INSERT INTO external_accounts (ma_ngan_hang, ten_ngan_hang, so_tai_khoan, ten_chu_tk) VALUES
  ('VCB', 'Vietcombank', '0011001234567', 'PHẠM MINH TUẤN'),
  ('TCB', 'Techcombank', '1903456789012', 'NGUYỄN THỊ LAN ANH'),
  ('MB', 'MB Bank', '0905123456', 'HOÀNG QUỐC BẢO'),
  ('BIDV', 'BIDV', '31410001234567', 'ĐẶNG VĂN HÙNG'),
  ('CTG', 'VietinBank', '108001234567', 'VŨ THỊ MAI HƯƠNG');

INSERT INTO cards (so_the, pin, account_id, so_lan_sai, trang_thai) VALUES
  ('9704001234567890', '67590a0da7df033b72ae36cbf36b2965ea1c497b4635c36c2bdb2a1867c7b457', 1, 0, 'ACTIVE'),
  ('9704009876543210', 'd5a7c9f79076a0a24e97831badaf73be1b38d2614143578f8dfc90a2ca461280', 2, 0, 'ACTIVE'),
  ('9704001111222233', '7e88a746a766befa474f0585e08d5275eab7b30f6ba0ecd8f0071226d35f49f9', 3, 0, 'LOCKED');

INSERT INTO transactions (ma_tham_chieu, account_id, loai, so_tien, phi, so_du_sau, tk_doi_tac, ngan_hang_nhan, noi_dung, dia_diem_atm, thoi_gian) VALUES
  ('VITB202611100001', 1, 'CHUYEN', 1500000, 0, 48500000, '0987654321', 'VITBANK', 'Chuyển tiền sinh hoạt', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 10 DAY)),
  ('VITB202611110002', 1, 'NHAN', 3000000, 0, 51500000, '0987654321', 'VITBANK', 'Hoàn tiền đặt vé', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 9 DAY)),
  ('VITB202611120003', 1, 'RUT', 1000000, 0, 50500000, '-', 'VITBANK', 'Rút tiền mặt tại ViTM', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  ('VITB202611130004', 1, 'CHUYEN', 500000, 5500, 49994500, '0011001234567', 'Vietcombank', 'Thanh toán tiền điện', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  ('VITB202611140005', 1, 'DOI_PIN', 0, 0, 49994500, '-', 'VITBANK', 'Đổi mã PIN thẻ ViTM', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  ('VITB202611150006', 1, 'NAP', 2500000, 0, 52494500, '-', 'VITBANK', 'Nộp tiền mặt tại ViTM', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  ('VITB202611160007', 1, 'RUT', 1500000, 0, 50994500, '-', 'VITBANK', 'Rút tiền mặt tại ViTM', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  ('VITB202611170008', 1, 'CHUYEN', 994500, 0, 50000000, '0987654321', 'VITBANK', 'Thanh toán hóa đơn', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  ('VITB202611180009', 1, 'RUT', 500000, 0, 49500000, '-', 'VITBANK', 'Rút tiền mặt tại ViTM', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  ('VITB202611190010', 1, 'NAP', 500000, 0, 50000000, '-', 'VITBANK', 'Nộp tiền mặt tại ViTM', '128 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 1 DAY));
