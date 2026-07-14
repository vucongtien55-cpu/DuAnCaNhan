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

