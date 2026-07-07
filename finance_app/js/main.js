/**
 * MAIN.JS - Bộ xử lý logic cho Sổ Thu Chi Cá Nhân
 */

// 1. KHỞI TẠO DỮ LIỆU MẪU (Nếu chưa có trong LocalStorage)
const initialData = {
    transactions: [
        { id: 1, title: 'Tiền trọ', amount: 6000000, type: 'expense', category: 'Tiền trọ', date: '2026-07-07' },
        { id: 2, title: 'Lương tháng', amount: 10000000, type: 'income', category: 'Lương hằng tháng', date: '2026-07-07' }
    ],
    budgets: {
        'Ăn uống': 1000000,
        'Mua sắm': 1000000,
        'Giải trí': 1000000,
        'Sức khỏe': 1000000
    }
};

if (!localStorage.getItem('financeData')) {
    localStorage.setItem('financeData', JSON.stringify(initialData));
}

// 2. CÁC HÀM TIỆN ÍCH (UTILITIES)
const utils = {
    // Định dạng tiền tệ VND
    formatMoney: (amount) => {
        return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
    },
    // Lấy dữ liệu từ LocalStorage
    getData: () => JSON.parse(localStorage.getItem('financeData')),
    // Lưu dữ liệu vào LocalStorage
    saveData: (data) => localStorage.setItem('financeData', JSON.stringify(data)),
    // Chuyển hướng trang
    goTo: (page) => window.location.href = page
};

// 3. XỬ LÝ KHI TRANG LOAD XONG
document.addEventListener('DOMContentLoaded', () => {
    const currentPage = window.location.pathname.split("/").pop();
    const data = utils.getData();

    // --- LOGIC CHO TRANG DASHBOARD (index.html) ---
    if (currentPage === 'index.html' || currentPage === '') {
        renderDashboard(data);
    }

    // --- LOGIC CHO TRANG GHI CHÉP (add-transaction.html) ---
    if (currentPage === 'add-transaction.html') {
        handleTransactionForm();
    }

    // --- LOGIC CHO TRANG LỊCH SỬ (history.html) ---
    if (currentPage === 'history.html') {
        renderHistoryTable(data.transactions);
    }

    // --- XỬ LÝ CHUNG CHO SIDEBAR ---
    handleSidebarActive(currentPage);
});

// 4. CHI TIẾT CÁC HÀM XỬ LÝ TRANG

// Xử lý Dashboard: Tính toán số dư, thu, chi
function renderDashboard(data) {
    let totalIncome = 0;
    let totalExpense = 0;

    data.transactions.forEach(t => {
        if (t.type === 'income') totalIncome += t.amount;
        else totalExpense += t.amount;
    });

    const balance = totalIncome - totalExpense;

    // Cập nhật DOM nếu các phần tử tồn tại
    const elBalance = document.querySelector('.stats-grid .stat-card.primary .val');
    const elIncome = document.querySelector('.stats-grid .stat-card:nth-child(2) .val');
    const elExpense = document.querySelector('.stats-grid .stat-card:nth-child(3) .val');

    if (elBalance) elBalance.innerText = utils.formatMoney(balance);
    if (elIncome) elIncome.innerText = utils.formatMoney(totalIncome);
    if (elExpense) elExpense.innerText = utils.formatMoney(totalExpense);
}

// Xử lý Form thêm giao dịch
function handleTransactionForm() {
    const form = document.querySelector('form');
    const btnExpense = document.getElementById('btnExpense');
    const btnIncome = document.getElementById('btnIncome');
    let currentType = 'expense';

    if (btnExpense && btnIncome) {
        btnExpense.onclick = () => {
            currentType = 'expense';
            btnExpense.classList.add('active-expense');
            btnIncome.classList.remove('active-income');
        };
        btnIncome.onclick = () => {
            currentType = 'income';
            btnIncome.classList.add('active-income');
            btnExpense.classList.remove('active-expense');
        };
    }

    if (form) {
        form.onsubmit = (e) => {
            e.preventDefault();
            const data = utils.getData();
            const newTransaction = {
                id: Date.now(),
                title: form.querySelector('input[placeholder*="Thực phẩm"]').value,
                amount: parseInt(form.querySelector('.amount-input').value.replace(/\D/g, '')),
                type: currentType,
                category: form.querySelector('select').value,
                date: form.querySelector('input[type="date"]').value
            };

            if (!newTransaction.title || !newTransaction.amount) {
                alert('Vui lòng nhập đầy đủ thông tin!');
                return;
            }

            data.transactions.unshift(newTransaction);
            utils.saveData(data);
            alert('Đã ghi sổ thành công!');
            utils.goTo('history.html');
        };
    }
}

// Xử lý Bảng lịch sử
function renderHistoryTable(transactions) {
    const tbody = document.querySelector('table tbody');
    if (!tbody) return;

    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td style="font-weight: 700;">${t.title}</td>
            <td>
                <span class="pill ${t.type === 'expense' ? 'pill-danger' : 'pill-success'}">
                    ${t.category}
                </span>
            </td>
            <td style="color: #64748b;">${t.date}</td>
            <td class="amount ${t.type === 'expense' ? 'amt-negative' : 'amt-positive'}">
                ${t.type === 'expense' ? '-' : '+'}${utils.formatMoney(t.amount)}
            </td>
            <td>
                <div class="row-actions">
                    <i class="fa-regular fa-pen-to-square" onclick="alert('Tính năng sửa đang phát triển')"></i>
                    <i class="fa-regular fa-trash-can" onclick="deleteTransaction(${t.id})"></i>
                </div>
            </td>
        </tr>
    `).join('');
}

// Xóa giao dịch
function deleteTransaction(id) {
    if (confirm('Bạn có chắc chắn muốn xóa giao dịch này?')) {
        const data = utils.getData();
        data.transactions = data.transactions.filter(t => t.id !== id);
        utils.saveData(data);
        renderHistoryTable(data.transactions);
    }
}

// Tự động active menu sidebar dựa trên URL
function handleSidebarActive(currentPage) {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPage) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}