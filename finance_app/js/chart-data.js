/**
 * CHART-DATA.JS - Xử lý biểu đồ trực quan
 * Sử dụng thư viện Chart.js
 */

document.addEventListener('DOMContentLoaded', () => {
    // Chỉ chạy nếu trang có canvas biểu đồ (thường là ở index.html)
    const ctx = document.getElementById('expenseChart');
    if (!ctx) return;

    renderExpenseChart(ctx);
});

function renderExpenseChart(ctx) {
    // 1. LẤY DỮ LIỆU TỪ LOCALSTORAGE
    const data = JSON.parse(localStorage.getItem('financeData'));
    if (!data || !data.transactions) return;

    // 2. XỬ LÝ DỮ LIỆU GIAO DỊCH
    // Lọc ra các khoản chi (expense)
    const expenses = data.transactions.filter(t => t.type === 'expense');

    // Gom nhóm tiền theo danh mục
    // Kết quả mong muốn: { "Tiền trọ": 6000000, "Ăn uống": 500000, ... }
    const categoryTotals = expenses.reduce((acc, curr) => {
        acc[curr.category] = (acc[curr.category] || 0) + curr.amount;
        return acc;
    }, {});

    const labels = Object.keys(categoryTotals);
    const amounts = Object.values(categoryTotals);
    const totalExpense = amounts.reduce((a, b) => a + b, 0);

    // Nếu không có dữ liệu chi tiêu, hiển thị biểu đồ rỗng màu xám
    if (amounts.length === 0) {
        labels.push('Chưa có dữ liệu');
        amounts.push(1);
    }

    // 3. ĐỊNH NGHĨA MÀU SẮC (Dựa theo bảng màu UI đã dùng)
    const colorPalette = [
        '#ef4444', // Đỏ (Tiền trọ/Ăn uống)
        '#3b82f6', // Xanh dương (Di chuyển)
        '#f59e0b', // Vàng (Nhà cửa)
        '#ec4899', // Hồng (Mua sắm)
        '#8b5cf6', // Tím (Giải trí)
        '#10b981', // Xanh lá (Sức khỏe)
        '#64748b'  // Xám (Khác)
    ];

    // 4. CẤU HÌNH BIỂU ĐỒ
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: amounts,
                backgroundColor: amounts[0] === 1 && labels[0] === 'Chưa có dữ liệu' ? ['#f1f5f9'] : colorPalette,
                borderWidth: 0,
                hoverOffset: 10
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '75%', // Độ dày của vòng nhẫn
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        usePointStyle: true,
                        padding: 25,
                        font: {
                            size: 13,
                            family: "'Inter', sans-serif",
                            weight: '600'
                        },
                        color: '#64748b'
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.label || '';
                            let value = context.raw || 0;
                            return ` ${label}: ${new Intl.NumberFormat('vi-VN').format(value)} đ`;
                        }
                    }
                }
            }
        },
        // Plugin tùy chỉnh để vẽ chữ ở giữa hình tròn
        plugins: [{
            id: 'centerText',
            beforeDraw: function(chart) {
                const { width, height, ctx } = chart;
                ctx.restore();

                // Chữ "TỔNG QUAN" nhỏ ở trên
                const fontSizeSmall = (height / 280).toFixed(2);
                ctx.font = `800 ${fontSizeSmall}em Inter`;
                ctx.textBaseline = "middle";
                ctx.fillStyle = "#94a3b8";
                
                const textUpper = "TỔNG QUAN";
                const textUpperX = Math.round((width - ctx.measureText(textUpper).width) / 2.7);
                const textUpperY = height / 2 - 20;
                ctx.fillText(textUpper, textUpperX, textUpperY);

                // Số tiền tổng lớn ở dưới
                const fontSizeBig = (height / 180).toFixed(2);
                ctx.font = `800 ${fontSizeBig}em Inter`;
                ctx.fillStyle = "#1e293b";

                const textLower = new Intl.NumberFormat('vi-VN').format(totalExpense) + " đ";
                const textLowerX = Math.round((width - ctx.measureText(textLower).width) / 2.7);
                const textLowerY = height / 2 + 15;
                ctx.fillText(textLower, textLowerX, textLowerY);

                ctx.save();
            }
        }]
    });
}