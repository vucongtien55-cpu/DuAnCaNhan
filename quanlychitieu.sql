-- SQL Schema cho Ứng dụng Quản lý Chi tiêu
-- Hệ quản trị cơ sở dữ liệu khuyên dùng: PostgreSQL

-- 1. Bảng lưu trữ thông tin người dùng (users)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(255) UNIQUE NOT NULL,
                                     password VARCHAR(255) NOT NULL,
                                     name VARCHAR(255),
                                     phone VARCHAR(50)
);

-- 2. Bảng phân loại thu chi (categories)
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGSERIAL PRIMARY KEY,
                                          user_email VARCHAR(255) NOT NULL,
                                          name VARCHAR(255) NOT NULL,
                                          type VARCHAR(50) NOT NULL, -- 'income' (thu nhập) hoặc 'expense' (chi tiêu)
                                          icon VARCHAR(100) NOT NULL,
                                          color VARCHAR(100) NOT NULL
);

-- 3. Bảng lưu lịch sử giao dịch (transactions)
CREATE TABLE IF NOT EXISTS transactions (
                                            id BIGSERIAL PRIMARY KEY,
                                            user_email VARCHAR(255) NOT NULL,
                                            date VARCHAR(50) NOT NULL, -- Định dạng YYYY-MM-DD
                                            amount DOUBLE PRECISION NOT NULL,
                                            type VARCHAR(50) NOT NULL, -- 'income' hoặc 'expense'
                                            category VARCHAR(255) NOT NULL,
                                            note VARCHAR(1000)
);

-- 4. Bảng quản lý hạn mức ngân sách (budgets)
CREATE TABLE IF NOT EXISTS budgets (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_email VARCHAR(255) NOT NULL,
                                       category VARCHAR(255) NOT NULL,
                                       limit_amount DOUBLE PRECISION NOT NULL
);

-- Tối ưu hóa: Tạo các chỉ mục (index) giúp truy vấn theo email nhanh hơn
CREATE INDEX IF NOT EXISTS idx_categories_user_email ON categories(user_email);
CREATE INDEX IF NOT EXISTS idx_transactions_user_email ON transactions(user_email);
CREATE INDEX IF NOT EXISTS idx_budgets_user_email ON budgets(user_email);

-- 1. Demo Users (Mật khẩu mẫu để text đơn giản)
INSERT INTO users (email, password, name, phone) VALUES
                                                     ('vucongtien55@gmail.com', '123456', 'Vũ Công Tiến', '0346783991'),
                                                     ('demo@example.com', '123456', 'User Demo', '0123456789')
ON CONFLICT (email) DO NOTHING;

-- 2. Demo Categories (Danh mục thu chi)
INSERT INTO categories (user_email, name, type, icon, color) VALUES
-- Cho tài khoản của bạn (vucongtien55@gmail.com)
('vucongtien55@gmail.com', 'Lương', 'income', 'briefcase', '#10B981'),
('vucongtien55@gmail.com', 'Kinh doanh', 'income', 'trending-up', '#3B82F6'),
('vucongtien55@gmail.com', 'Ăn uống', 'expense', 'utensils', '#EF4444'),
('vucongtien55@gmail.com', 'Di chuyển', 'expense', 'car', '#F59E0B'),
('vucongtien55@gmail.com', 'Mua sắm', 'expense', 'shopping-cart', '#EC4899'),
('vucongtien55@gmail.com', 'Hóa đơn & Tiện ích', 'expense', 'home', '#8B5CF6'),
('vucongtien55@gmail.com', 'Giải trí', 'expense', 'film', '#6366F1'),

-- Cho tài khoản demo@example.com
('demo@example.com', 'Lương', 'income', 'briefcase', '#10B981'),
('demo@example.com', 'Ăn uống', 'expense', 'utensils', '#EF4444'),
('demo@example.com', 'Mua sắm', 'expense', 'shopping-cart', '#EC4899')
ON CONFLICT DO NOTHING;

-- 3. Demo Budgets (Hạn mức chi tiêu)
INSERT INTO budgets (user_email, category, limit_amount) VALUES
                                                             ('vucongtien55@gmail.com', 'Ăn uống', 5000000),
                                                             ('vucongtien55@gmail.com', 'Mua sắm', 3000000),
                                                             ('vucongtien55@gmail.com', 'Di chuyển', 1000000),
                                                             ('demo@example.com', 'Ăn uống', 4000000);

-- 4. Demo Transactions (Giao dịch thu chi mẫu)
INSERT INTO transactions (user_email, date, amount, type, category, note) VALUES
-- Giao dịch của vucongtien55@gmail.com
('vucongtien55@gmail.com', '2026-07-01', 15000000, 'income', 'Lương', 'Lương tháng 7'),
('vucongtien55@gmail.com', '2026-07-02', 3000000, 'income', 'Kinh doanh', 'Bán hàng online'),
('vucongtien55@gmail.com', '2026-07-03', 150000, 'expense', 'Ăn uống', 'Ăn trưa với đồng nghiệp'),
('vucongtien55@gmail.com', '2026-07-03', 500000, 'expense', 'Mua sắm', 'Mua áo sơ mi mới'),
('vucongtien55@gmail.com', '2026-07-04', 50000, 'expense', 'Di chuyển', 'Đổ xăng xe máy'),
('vucongtien55@gmail.com', '2026-07-05', 1200000, 'expense', 'Hóa đơn & Tiện ích', 'Tiền điện nước tháng 6'),
('vucongtien55@gmail.com', '2026-07-06', 250000, 'expense', 'Giải trí', 'Xem phim rạp CGV'),
('vucongtien55@gmail.com', '2026-07-07', 85000, 'expense', 'Ăn uống', 'Mua cà phê sáng'),

-- Giao dịch của demo@example.com
('demo@example.com', '2026-07-01', 12000000, 'income', 'Lương', 'Lương tháng 7'),
('demo@example.com', '2026-07-02', 200000, 'expense', 'Ăn uống', 'Ăn tối lẩu'),
('demo@example.com', '2026-07-04', 1500000, 'expense', 'Mua sắm', 'Mua giày thể thao');

select * from categories