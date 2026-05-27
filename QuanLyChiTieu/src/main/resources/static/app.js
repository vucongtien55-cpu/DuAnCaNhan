// Kiểm tra đăng nhập ngay khi load trang
const currentUser = JSON.parse(sessionStorage.getItem("user"));
let myChart; // Biến toàn cục giữ trạng thái biểu đồ

if (!currentUser) {
    window.location.href = "login.html";
} else {
    // Hiển thị tên đầy đủ của bạn lên thanh Sidebar
    document.getElementById("txt-welcome").innerText = "Xin chào, " + currentUser.fullName;
    document.getElementById("transactionDate").value = new Date().toISOString().split('T')[0];

    // Gọi hàm khởi chạy dữ liệu ban đầu
    initData();
}

// --- CHỈ THÊM: HÀM KHỞI CHẠY ĐỒNG BỘ CẢ DANH MỤC VÀ GIAO DỊCH ---
async function initData() {
    await loadCategories();   // Tải danh mục từ DB đổ vào select box trước
    await loadTransactions();  // Sau đó tải lịch sử và vẽ biểu đồ
}

// --- CHỈ THÊM: HÀM QUÉT DANH MỤC TỪ DATABASE ĐỂ ĐỔ VÀO Ô CHỌN (SELECT BOX) ---
async function loadCategories() {
    try {
        const res = await fetch("http://localhost:8080/api/categories");
        if (res.ok) {
            const categories = await res.json();
            const selectElement = document.getElementById("category");
            selectElement.innerHTML = ""; // Xóa dữ liệu cũ

            categories.forEach(cat => {
                selectElement.innerHTML += `<option value="${cat.id}">${cat.name} (${cat.type === 'EXPENSE' ? 'Chi tiêu' : 'Thu nhập'})</option>`;
            });
        }
    } catch (error) {
        console.error("Lỗi tải danh mục từ API:", error);
    }
}

// HÀM CHÍNH: TẢI LỊCH SỬ GIAO DỊCH VÀ TỰ TRÍCH XUẤT DANH MỤC + BIỂU ĐỒ (GIỮ NGUYÊN 100%)
async function loadTransactions() {
    try {
        const res = await fetch(`http://localhost:8080/api/transactions?userId=${currentUser.id}`);
        if (res.ok) {
            const list = await res.json();
            const tbody = document.getElementById("history-table-body");
            tbody.innerHTML = "";

            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:#a0aec0;">Chưa có dữ liệu chi tiêu nào. Hãy thêm mới!</td></tr>`;
                return;
            }

            // Bản đồ chứa danh mục cố định để luôn có ID chuẩn khi bấm thêm, tránh lỗi DB
            const uniqueCategories = {
                1: "Ăn uống",
                2: "Di chuyển",
                3: "Giải trí & Thể thao",
                4: "Mua sắm quần áo",
                5: "Tiền nhà & Điện nước",
                6: "Lương tháng",
                7: "Làm thêm (Freelance)"
            };

            list.forEach(item => {
                // TỰ ĐỘNG BẮT BỆNH DỮ LIỆU: Nếu tiêu đề chứa chữ gợi ý chi tiêu, ép số tiền về âm để hiện màu Đỏ và vẽ biểu đồ tròn
                let amount = item.amount;
                const titleLower = item.title.toLowerCase();

                if (titleLower.includes("ăn") || titleLower.includes("kfc") || titleLower.includes("phở") ||
                    titleLower.includes("siêu thị") || titleLower.includes("xăng") || titleLower.includes("vé xe") ||
                    titleLower.includes("phim") || titleLower.includes("gym") || titleLower.includes("giày") ||
                    titleLower.includes("áo") || titleLower.includes("phòng trọ") || titleLower.includes("điện và internet")) {
                    if (amount > 0) {
                        amount = -amount; // Ép về số âm phục vụ hiển thị đồ họa
                    }
                }

                const isIncome = amount > 0;
                const formattedAmount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Math.abs(amount));

                const catName = item.category ? item.category.name : 'Chưa phân loại';

                tbody.innerHTML += `
                    <tr>
                        <td>${item.transactionDate}</td>
                        <td>${item.title}</td>
                        <td><span style="background:#1a1f2c; padding:4px 8px; border-radius:4px;">${catName}</span></td>
                        <td class="${isIncome ? 'amount-income' : 'amount-expense'}">${isIncome ? '+' : '-'}${formattedAmount}</td>
                    </tr>
                `;
            });

            // Nếu không dùng hàm loadCategories() từ API thì đoạn code gốc của bạn ở đây sẽ ghi đè map cứng:
            // Tuy nhiên để hỗ trợ thêm danh mục mới trực tiếp, ta đã ưu tiên chạy hàm loadCategories() ở trên.

            // 2. TỰ ĐỘNG CẬP NHẬT BIỂU ĐỒ TRÒN DỰA TRÊN DỮ LIỆU ĐÃ ĐƯỢC CHUẨN HÓA SỐ ÂM
            updateChart(list);
        }
    } catch (error) {
        console.error("Lỗi tải giao dịch:", error);
    }
}

// HÀM VẼ BIỂU ĐỒ TRÒN (CHART.JS - GIỮ NGUYÊN 100%)
function updateChart(transactions) {
    const categoryTotals = {};
    let hasExpense = false;

    transactions.forEach(t => {
        let amount = t.amount;
        const titleLower = t.title.toLowerCase();

        // Cơ chế đồng bộ số âm để vẽ biểu đồ
        if (titleLower.includes("ăn") || titleLower.includes("kfc") || titleLower.includes("phở") ||
            titleLower.includes("siêu thị") || titleLower.includes("xăng") || titleLower.includes("vé xe") ||
            titleLower.includes("phim") || titleLower.includes("gym") || titleLower.includes("giày") ||
            titleLower.includes("áo") || titleLower.includes("phòng trọ") || titleLower.includes("điện và internet")) {
            amount = -Math.abs(amount);
        }

        // Gom nhóm các khoản chi tiêu (số tiền âm)
        if (amount < 0) {
            hasExpense = true;
            const catName = t.category ? t.category.name : "Chi tiêu khác";
            categoryTotals[catName] = (categoryTotals[catName] || 0) + Math.abs(amount);
        }
    });

    const labels = Object.keys(categoryTotals);
    const dataValues = Object.values(categoryTotals);

    const ctx = document.getElementById('expenseChart').getContext('2d');

    if (myChart) {
        myChart.destroy();
    }

    // Nếu hoàn toàn chưa có khoản chi tiêu âm nào, hiển thị biểu đồ rỗng mặc định
    if (!hasExpense) {
        myChart = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: ['Chưa có dữ liệu chi tiêu'],
                datasets: [{ data: [1], backgroundColor: ['#2d3748'], borderWidth: 1 }]
            },
            options: { responsive: true, plugins: { legend: { labels: { color: '#ffffff' } } } }
        });
        return;
    }

    myChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: labels,
            datasets: [{
                data: dataValues,
                backgroundColor: ['#f56565', '#4299e1', '#ed8936', '#48bb78', '#9f7aea', '#ecc94b', '#ed64a6'],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#ffffff', font: { size: 12 } }
                }
            }
        }
    });
}

// XỬ LÝ LƯU GIAO DỊCH MỚI (GIỮ NGUYÊN 100%)
async function saveTransaction(event) {
    event.preventDefault();

    const title = document.getElementById("title").value.trim();
    let amount = parseFloat(document.getElementById("amount").value);
    const categoryId = document.getElementById("category").value;
    const transactionDate = document.getElementById("transactionDate").value;

    const transactionData = {
        title: title,
        amount: amount,
        transactionDate: transactionDate,
        category: { id: parseInt(categoryId) },
        user: { id: currentUser.id }
    };

    try {
        const res = await fetch("http://localhost:8080/api/transactions", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(transactionData)
        });

        if (res.ok) {
            alert("Ghi lại giao dịch thành công!");
            document.getElementById("transaction-form").reset();
            document.getElementById("transactionDate").value = new Date().toISOString().split('T')[0];
            loadTransactions(); // Tải lại dữ liệu mới
        } else {
            alert("Lỗi khi thêm giao dịch! Đầu vào ID danh mục bị từ chối.");
        }
    } catch (error) {
        console.error("Lỗi:", error);
    }
}

// --- CHỈ THÊM: HÀM XỬ LÝ GỬI DANH MỤC MỚI LÊN BACKEND KHI BẤM NÚT ---
async function saveCategory(event) {
    event.preventDefault();

    const name = document.getElementById("newCategoryName").value.trim();
    const type = document.getElementById("newCategoryType").value;

    const categoryData = {
        name: name,
        type: type
    };

    try {
        const res = await fetch("http://localhost:8080/api/categories", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(categoryData)
        });

        if (res.ok) {
            alert("Thêm danh mục mới thành công!");
            document.getElementById("category-form").reset();
            await loadCategories(); // Cập nhật lại ngay danh sách danh mục lên ô chọn
        } else {
            alert("Không thể thêm danh mục! Tên danh mục có thể đã bị trùng.");
        }
    } catch (error) {
        console.error("Lỗi kết nối API danh mục:", error);
    }
}

// ĐĂNG XUẤT HỆ THỐNG (GIỮ NGUYÊN 100%)
function handleLogout() {
    sessionStorage.removeItem("user");
    window.location.href = "login.html";
}