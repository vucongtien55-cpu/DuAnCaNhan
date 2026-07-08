// 2. Default categories & data presets
const EXPENSE_CATEGORIES = [
    { id: 'anuong', name: 'Ăn uống', icon: 'utensils', color: '#EF4444', type: 'EXPENSE' },
    { id: 'dichuyen', name: 'Di chuyển', icon: 'car', color: '#3B82F6', type: 'EXPENSE' },
    { id: 'muasam', name: 'Mua sắm', icon: 'shopping-bag', color: '#EC4899', type: 'EXPENSE' },
    { id: 'nhacua', name: 'Nhà cửa & Dịch vụ', icon: 'home', color: '#F59E0B', type: 'EXPENSE' },
    { id: 'giaitri', name: 'Giải trí', icon: 'film', color: '#8B5CF6', type: 'EXPENSE' },
    { id: 'suckhoe', name: 'Sức khỏe', icon: 'heart-pulse', color: '#10B981', type: 'EXPENSE' },
    { id: 'giaoduc', name: 'Giáo dục', icon: 'graduation-cap', color: '#06B6D4', type: 'EXPENSE' },
    { id: 'expense_khac', name: 'Khác (Chi tiêu)', icon: 'more-horizontal', color: '#64748B', type: 'EXPENSE' }
];

const INCOME_CATEGORIES = [
    { id: 'luong', name: 'Lương hằng tháng', icon: 'briefcase', color: '#10B981', type: 'INCOME' },
    { id: 'lamthem', name: 'Làm thêm (Freelance)', icon: 'laptop', color: '#14B8A6', type: 'INCOME' },
    { id: 'dautu', name: 'Đầu tư', icon: 'trending-up', color: '#06B6D4', type: 'INCOME' },
    { id: 'quatang', name: 'Quà tặng, Thưởng', icon: 'gift', color: '#F43F5E', type: 'INCOME' },
    { id: 'income_khac', name: 'Khác (Thu nhập)', icon: 'dollar-sign', color: '#64748B', type: 'INCOME' }
];

const INITIAL_BUDGETS = [
    { category: 'Ăn uống', limit: 4500000 },
    { category: 'Di chuyển', limit: 1200000 },
    { category: 'Mua sắm', limit: 3000000 },
    { category: 'Nhà cửa & Dịch vụ', limit: 5000000 },
    { category: 'Giải trí', limit: 2000000 },
    { category: 'Sức khỏe', limit: 1500000 }
];

const INITIAL_TRANSACTIONS = [
    { id: 'tx-1', title: 'Nhận lương tháng 6', amount: 18500000, type: 'INCOME', category: 'Lương hằng tháng', date: '2026-06-05', notes: 'Lương công ty chính thức' },
    { id: 'tx-2', title: 'Trả tiền thuê nhà & điện nước', amount: 4500000, type: 'EXPENSE', category: 'Nhà cửa & Dịch vụ', date: '2026-06-06', notes: 'Tiền nhà tháng 6' },
    { id: 'tx-3', title: 'Dự án website thiết kế', amount: 3200000, type: 'INCOME', category: 'Làm thêm (Freelance)', date: '2026-06-10', notes: 'Thiết kế UI/UX Landing Page' },
    { id: 'tx-4', title: 'Mua thực phẩm tuần mới', amount: 850000, type: 'EXPENSE', category: 'Ăn uống', date: '2026-06-08', notes: 'Đi siêu thị Winmart' },
    { id: 'tx-5', title: 'Đăng ký tập Gym 3 tháng', amount: 1200000, type: 'EXPENSE', category: 'Sức khỏe', date: '2026-06-11', notes: 'Phòng gym Fit24' },
    { id: 'tx-6', title: 'Đổ xăng xe máy', amount: 90000, type: 'EXPENSE', category: 'Di chuyển', date: '2026-06-12', notes: 'Đầy bình xăng xe Lead' },
    { id: 'tx-7', title: 'Ăn tối cùng gia đình', amount: 750000, type: 'EXPENSE', category: 'Ăn uống', date: '2026-06-13', notes: 'Ăn lẩu Haidilao' },
    { id: 'tx-8', title: 'Mua sách kỹ năng mềm', amount: 280000, type: 'EXPENSE', category: 'Giáo dục', date: '2026-06-14', notes: 'Sách Nguyên Lý 80/20 và Đắc Nhân Tâm' },
    { id: 'tx-9', title: 'Tiền lãi đầu tư cổ phiếu', amount: 640000, type: 'INCOME', category: 'Đầu tư', date: '2026-06-15', notes: 'Cổ tức nhận quý 2' },
    { id: 'tx-10', title: 'Mua quần áo đi đám cưới', amount: 1450000, type: 'EXPENSE', category: 'Mua sắm', date: '2026-06-16', notes: 'Mua vest nhẹ và giày tây' },
    { id: 'tx-11', title: 'Xem phim cuối tuần rạp CGV', amount: 320000, type: 'EXPENSE', category: 'Giải trí', date: '2026-06-19', notes: 'Vé xem phim + bỏng nước' },
    { id: 'tx-12', title: 'Đăng ký gói Netflix', amount: 260000, type: 'EXPENSE', category: 'Giải trí', date: '2026-06-20', notes: 'Gia hạn gói Ultra HD tháng này' },
    { id: 'tx-13', title: 'Đặt Highlands Coffee', amount: 135000, type: 'EXPENSE', category: 'Ăn uống', date: '2026-06-21', notes: 'Trà sen vàng và freeze trà xanh' }
];

window.EXPENSE_CATEGORIES = EXPENSE_CATEGORIES;
window.INCOME_CATEGORIES = INCOME_CATEGORIES;
window.INITIAL_BUDGETS = INITIAL_BUDGETS;
window.INITIAL_TRANSACTIONS = INITIAL_TRANSACTIONS;
