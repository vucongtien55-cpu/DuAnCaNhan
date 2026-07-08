// 8. JS controller for dashboard.html

let currentFilter = 'all'; // 'all', 'month', 'custom_range', or 'YYYY-MM-DD'
let categoryChartInstance = null;

function setDashboardFilter(filter) {
    currentFilter = filter;

    const allBtn = document.getElementById('filter-all-btn');
    const monthBtn = document.getElementById('filter-month-btn');
    const customInput = document.getElementById('filter-custom-date');

    const startInput = document.getElementById('filter-start-date');
    const endInput = document.getElementById('filter-end-date');

    if (allBtn && monthBtn && customInput) {
        if (filter === 'all') {
            // All time
            allBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-indigo-600 text-white shadow-sm";
            monthBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-transparent text-slate-600 dark:text-slate-400 hover:bg-slate-200/80 dark:hover:bg-slate-800/80";
            customInput.value = "";
            if (startInput) startInput.value = "";
            if (endInput) endInput.value = "";
        } else if (filter === 'month') {
            // This Month
            allBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-transparent text-slate-600 dark:text-slate-400 hover:bg-slate-200/80 dark:hover:bg-slate-800/80";
            monthBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-indigo-600 text-white shadow-sm";
            customInput.value = "";
            if (startInput) startInput.value = "";
            if (endInput) endInput.value = "";
        } else if (filter === 'custom_range') {
            // Custom date range
            allBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-transparent text-slate-600 dark:text-slate-400 hover:bg-slate-200/80 dark:hover:bg-slate-800/80";
            monthBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-transparent text-slate-600 dark:text-slate-400 hover:bg-slate-200/80 dark:hover:bg-slate-800/80";
            customInput.value = "";
        } else {
            // Specific Date (YYYY-MM-DD or custom selection)
            allBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-transparent text-slate-600 dark:text-slate-400 hover:bg-slate-200/80 dark:hover:bg-slate-800/80";
            monthBtn.className = "px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer border-0 bg-transparent text-slate-600 dark:text-slate-400 hover:bg-slate-200/80 dark:hover:bg-slate-800/80";
            if (startInput) startInput.value = "";
            if (endInput) endInput.value = "";

            // Assign value to date input if it is a valid date string (YYYY-MM-DD)
            const parts = filter.split('-');
            if (parts.length === 3) {
                customInput.value = filter;
            } else if (parts.length === 2) {
                // Fallback for YYYY-MM -> set to YYYY-MM-01
                customInput.value = `${filter}-01`;
            } else {
                customInput.value = "";
            }
        }
    }

    renderDashboard();
}
window.setDashboardFilter = setDashboardFilter;

function applyDateRangeFilter() {
    const startInput = document.getElementById('filter-start-date');
    const endInput = document.getElementById('filter-end-date');
    if (!startInput || !endInput) return;

    const startVal = startInput.value;
    const endVal = endInput.value;

    if (startVal || endVal) {
        setDashboardFilter('custom_range');
    }
}
window.applyDateRangeFilter = applyDateRangeFilter;

function changeDashboardMonth(direction) {
    const today = new Date();

    if (currentFilter === 'all') {
        // If navigating from all, go to current date
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        setDashboardFilter(`${yyyy}-${mm}-${dd}`);
        return;
    }

    if (currentFilter === 'month') {
        const targetDate = new Date(today.getFullYear(), today.getMonth() + direction, 1);
        const newYear = targetDate.getFullYear();
        const newMonth = String(targetDate.getMonth() + 1).padStart(2, '0');
        const newDay = String(targetDate.getDate()).padStart(2, '0');
        setDashboardFilter(`${newYear}-${newMonth}-01`);
        return;
    }

    // Custom date range navigation (moves start and end date by 1 month)
    if (currentFilter === 'custom_range') {
        const startInput = document.getElementById('filter-start-date');
        const endInput = document.getElementById('filter-end-date');
        if (startInput && endInput) {
            if (startInput.value) {
                const sParts = startInput.value.split('-');
                if (sParts.length === 3) {
                    const sDate = new Date(parseInt(sParts[0], 10), parseInt(sParts[1], 10) - 1 + direction, parseInt(sParts[2], 10));
                    startInput.value = `${sDate.getFullYear()}-${String(sDate.getMonth() + 1).padStart(2, '0')}-${String(sDate.getDate()).padStart(2, '0')}`;
                }
            }
            if (endInput.value) {
                const eParts = endInput.value.split('-');
                if (eParts.length === 3) {
                    const eDate = new Date(parseInt(eParts[0], 10), parseInt(eParts[1], 10) - 1 + direction, parseInt(eParts[2], 10));
                    endInput.value = `${eDate.getFullYear()}-${String(eDate.getMonth() + 1).padStart(2, '0')}-${String(eDate.getDate()).padStart(2, '0')}`;
                }
            }
            renderDashboard();
        }
        return;
    }

    // Check if currentFilter is a 4-digit year
    if (currentFilter.length === 4 && !isNaN(currentFilter)) {
        const currentYear = parseInt(currentFilter, 10);
        const newYear = currentYear + direction;
        setDashboardFilter(String(newYear));
        return;
    }

    // Otherwise, it's a specific date YYYY-MM-DD or YYYY-MM
    const parts = currentFilter.split('-');
    if (parts.length === 3) {
        const y = parseInt(parts[0], 10);
        const m = parseInt(parts[1], 10) - 1; // 0-11
        const d = parseInt(parts[2], 10);

        // Move month-by-month but keeping the day (standard behavior)
        const targetDate = new Date(y, m + direction, d);
        const newYear = targetDate.getFullYear();
        const newMonth = String(targetDate.getMonth() + 1).padStart(2, '0');
        const newDay = String(targetDate.getDate()).padStart(2, '0');

        setDashboardFilter(`${newYear}-${newMonth}-${newDay}`);
        return;
    }

    if (parts.length === 2) {
        const y = parseInt(parts[0], 10);
        const m = parseInt(parts[1], 10) - 1; // 0-11

        const targetDate = new Date(y, m + direction, 1);
        const newYear = targetDate.getFullYear();
        const newMonth = String(targetDate.getMonth() + 1).padStart(2, '0');

        setDashboardFilter(`${newYear}-${newMonth}-01`);
        return;
    }
}
window.changeDashboardMonth = changeDashboardMonth;

function restoreDemoData() {
    const email = state.user;
    if (!email) return;

    localStorage.setItem(`chi_tieu_${email}_transactions`, JSON.stringify(INITIAL_TRANSACTIONS));
    localStorage.setItem(`chi_tieu_${email}_budgets`, JSON.stringify(INITIAL_BUDGETS));
    localStorage.setItem(`chi_tieu_${email}_expense_categories`, JSON.stringify(EXPENSE_CATEGORIES));
    localStorage.setItem(`chi_tieu_${email}_income_categories`, JSON.stringify(INCOME_CATEGORIES));

    showToast(state.language === 'vi' ? 'Khôi phục dữ liệu mẫu thành công!' : 'Sample data restored successfully!');
    loadUserData();
    renderDashboard();
}
window.restoreDemoData = restoreDemoData;

function renderDashboard() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Apply quick text translations dynamically
    document.getElementById('dashboard-heading').innerText = t.reportTab;

    let subText = lang === 'vi' ? 'Tổng quan tình hình thu chi & ngân sách' : 'Overall status of income, expenses & budgets';

    const fastFilterLabel = document.getElementById('fast-filter-label');
    if (fastFilterLabel) fastFilterLabel.innerText = lang === 'vi' ? 'Bộ lọc nhanh' : 'Quick Filters';

    const customPickerLabel = document.getElementById('custom-picker-label');
    if (customPickerLabel) customPickerLabel.innerText = lang === 'vi' ? 'Ngày thực hiện' : 'Date selected';

    const rangePickerLabel = document.getElementById('range-picker-label');
    if (rangePickerLabel) rangePickerLabel.innerText = lang === 'vi' ? 'Lọc theo khoảng ngày' : 'Date range filter';

    const rangeStartLabel = document.getElementById('range-start-label');
    if (rangeStartLabel) rangeStartLabel.innerText = lang === 'vi' ? 'Từ' : 'From';

    const rangeEndLabel = document.getElementById('range-end-label');
    if (rangeEndLabel) rangeEndLabel.innerText = lang === 'vi' ? 'Đến' : 'To';

    const navFilterLabel = document.getElementById('nav-filter-label');
    if (navFilterLabel) navFilterLabel.innerText = lang === 'vi' ? 'Di chuyển' : 'Browse';

    const filterAllBtn = document.getElementById('filter-all-btn');
    if (filterAllBtn) filterAllBtn.innerText = lang === 'vi' ? 'Tất cả' : 'All time';

    const filterMonthBtn = document.getElementById('filter-month-btn');
    if (filterMonthBtn) filterMonthBtn.innerText = lang === 'vi' ? 'Tháng này' : 'This Month';

    document.getElementById('balance-label').innerText = t.walletBalance;
    document.getElementById('balance-sub-label').innerText = t.walletSub;
    document.getElementById('income-label').innerText = t.totalIncome;
    document.getElementById('expense-label').innerText = t.totalExpense;

    document.getElementById('income-desc-label').innerText = lang === 'vi' ? 'Từ giao dịch thực tế' : 'From actual sources';
    document.getElementById('expense-desc-label').innerText = lang === 'vi' ? 'Đã thanh toán thực tế' : 'Actually spent';

    // Filter transactions based on date if filter is 'month', 'custom_range', or specific date
    let list = state.transactions || [];

    if (currentFilter === 'month') {
        const today = new Date(); // In 2026, let's look at June/July 2026
        const curYear = today.getFullYear();
        const curMonth = today.getMonth(); // 0-11

        subText += lang === 'vi' ? ` (Tháng này: ${curMonth + 1}/${curYear})` : ` (This month: ${curMonth + 1}/${curYear})`;

        list = list.filter(tx => {
            const txDate = new Date(tx.date);
            return txDate.getFullYear() === curYear && txDate.getMonth() === curMonth;
        });
    } else if (currentFilter === 'custom_range') {
        const startInput = document.getElementById('filter-start-date');
        const endInput = document.getElementById('filter-end-date');
        const startVal = startInput ? startInput.value : '';
        const endVal = endInput ? endInput.value : '';

        const formatDate = (val) => {
            const p = val.split('-');
            return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : val;
        };

        if (startVal && endVal) {
            subText += lang === 'vi' ? ` (Từ ngày ${formatDate(startVal)} đến ${formatDate(endVal)})` : ` (From ${formatDate(startVal)} to ${formatDate(endVal)})`;
        } else if (startVal) {
            subText += lang === 'vi' ? ` (Từ ngày ${formatDate(startVal)})` : ` (From ${formatDate(startVal)})`;
        } else if (endVal) {
            subText += lang === 'vi' ? ` (Đến ngày ${formatDate(endVal)})` : ` (Until ${formatDate(endVal)})`;
        } else {
            subText += lang === 'vi' ? ' (Tất cả khoảng ngày)' : ' (All ranges)';
        }

        list = list.filter(tx => {
            if (!tx.date) return false;
            if (startVal && tx.date < startVal) return false;
            if (endVal && tx.date > endVal) return false;
            return true;
        });
    } else if (currentFilter !== 'all') {
        const parts = currentFilter.split('-');
        if (parts.length === 3) {
            // Filter by Exact Date (YYYY-MM-DD)
            subText += lang === 'vi' ? ` (Ngày ${parts[2]}/${parts[1]}/${parts[0]})` : ` (Date ${parts[2]}/${parts[1]}/${parts[0]})`;
            list = list.filter(tx => tx.date === currentFilter);
        } else if (parts.length === 2) {
            // Filter by Month (YYYY-MM)
            const filterYear = parseInt(parts[0], 10);
            const filterMonth = parseInt(parts[1], 10) - 1; // 0-11 JS Month is 0-indexed
            subText += lang === 'vi' ? ` (Tháng ${parts[1]}/${parts[0]})` : ` (Month ${parts[1]}/${parts[0]})`;
            list = list.filter(tx => {
                const txDate = new Date(tx.date);
                return txDate.getFullYear() === filterYear && txDate.getMonth() === filterMonth;
            });
        } else if (parts.length === 1 && currentFilter.length === 4 && !isNaN(currentFilter)) {
            // Filter by Year
            const filterYear = parseInt(currentFilter, 10);
            subText += lang === 'vi' ? ` (Năm ${filterYear})` : ` (Year ${filterYear})`;
            list = list.filter(tx => {
                const txDate = new Date(tx.date);
                return txDate.getFullYear() === filterYear;
            });
        }
    } else {
        subText += lang === 'vi' ? ' (Tất cả thời gian)' : ' (All time)';
    }

    document.getElementById('dashboard-sub').innerText = subText;

    // Calculate Metrics
    let totalIncome = 0;
    let totalExpense = 0;

    list.forEach(tx => {
        if (tx.type === 'INCOME') {
            totalIncome += Number(tx.amount);
        } else {
            totalExpense += Number(tx.amount);
        }
    });

    const balance = totalIncome - totalExpense;

    // Render metrics
    document.getElementById('balance-value').innerText = formatVND(balance);
    document.getElementById('income-value').innerText = formatVND(totalIncome);
    document.getElementById('expense-value').innerText = formatVND(totalExpense);

    // Group Expenses by Category
    const expenseMap = {};
    list.forEach(tx => {
        if (tx.type === 'EXPENSE') {
            expenseMap[tx.category] = (expenseMap[tx.category] || 0) + Number(tx.amount);
        }
    });

    // Render Charts and category breakdowns
    renderCategoryBreakdown(expenseMap, totalExpense);

    // Render Budgets Progress meters
    renderBudgetMeters(expenseMap);

    // Render Recent Transactions
    renderRecentTransactionsList(list.slice(0, 5));

    if (window.lucide) window.lucide.createIcons();
}

function renderCategoryBreakdown(expenseMap, totalExpense) {
    const lang = state.language;
    const labelsContainer = document.getElementById('pie-labels-container');
    labelsContainer.innerHTML = '';

    const categories = Object.keys(expenseMap);

    if (categories.length === 0) {
        document.getElementById('pie-chart-placeholder').classList.remove('hidden');
        document.getElementById('category-pie-chart').style.display = 'none';

        labelsContainer.innerHTML = `
      <div class="text-xs text-slate-400 dark:text-slate-500 font-bold text-center py-4">
        ${lang === 'vi' ? 'Không tìm thấy dữ liệu chi tiêu phù hợp.' : 'No matching expense data found.'}
      </div>
    `;

        if (categoryChartInstance) {
            categoryChartInstance.destroy();
            categoryChartInstance = null;
        }
        return;
    }

    document.getElementById('pie-chart-placeholder').classList.add('hidden');
    document.getElementById('category-pie-chart').style.display = 'block';

    const chartData = [];
    const chartLabels = [];
    const chartColors = [];

    // Sort categories by amount descending
    const sortedCats = categories.map(cat => ({
        name: cat,
        amount: expenseMap[cat],
        percentage: totalExpense > 0 ? ((expenseMap[cat] / totalExpense) * 100).toFixed(1) : 0,
        color: getCategoryColor(cat),
        icon: getCategoryIcon(cat)
    })).sort((a, b) => b.amount - a.amount);

    sortedCats.forEach(item => {
        chartData.push(item.amount);
        chartLabels.push(item.name);
        chartColors.push(item.color);

        // Create side detailed label
        const labelRow = document.createElement('div');
        labelRow.className = 'flex items-center justify-between p-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-950 transition-all';
        labelRow.innerHTML = `
      <div class="flex items-center gap-2.5">
        <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${item.color}"></span>
        <span class="text-xs font-bold text-slate-600 dark:text-slate-300">${item.name}</span>
      </div>
      <div class="text-right">
        <span class="text-xs font-extrabold font-mono text-slate-800 dark:text-white">${formatVND(item.amount)}</span>
        <span class="block text-[9px] font-bold text-slate-400 font-mono">${item.percentage}%</span>
      </div>
    `;
        labelsContainer.appendChild(labelRow);
    });

    // Render/Update ChartJS Instance
    const ctx = document.getElementById('category-pie-chart').getContext('2d');

    if (categoryChartInstance) {
        categoryChartInstance.destroy();
    }

    categoryChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: chartLabels,
            datasets: [{
                data: chartData,
                backgroundColor: chartColors,
                borderWidth: state.theme === 'dark' ? 2 : 1,
                borderColor: state.theme === 'dark' ? '#0f172a' : '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return ` ${context.label}: ${formatVND(context.raw)}`;
                        }
                    }
                }
            },
            cutout: '70%'
        }
    });
}

function renderBudgetMeters(expenseMap) {
    const container = document.getElementById('budget-meters-container');
    container.innerHTML = '';

    const budgets = state.budgets || [];
    const lang = state.language;

    if (budgets.length === 0) {
        container.innerHTML = `
      <div class="text-xs text-slate-400 font-bold text-center py-6">
        ${lang === 'vi' ? 'Chưa cấu hình hạn mức nào.' : 'No budget limits configured yet.'}
      </div>
    `;
        return;
    }

    let totalExceededCount = 0;

    budgets.forEach(b => {
        const spent = expenseMap[b.category] || 0;
        const limit = b.limit || 0;
        const percentage = limit > 0 ? Math.min((spent / limit) * 100, 100) : 0;
        const isExceeded = spent > limit;

        if (isExceeded) {
            totalExceededCount++;
        }

        const meter = document.createElement('div');
        meter.className = 'space-y-1.5';
        meter.innerHTML = `
      <div class="flex justify-between items-center text-[11px] font-bold">
        <span class="text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
          <span class="w-2 h-2 rounded-full" style="background-color: ${getCategoryColor(b.category)}"></span>
          ${b.category}
        </span>
        <span class="${isExceeded ? 'text-rose-500 font-extrabold' : 'text-slate-400'} font-mono">
          ${formatVND(spent)} / ${formatVND(limit)}
        </span>
      </div>
      <div class="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden relative">
        <div class="h-full rounded-full transition-all duration-500 ${isExceeded ? 'bg-rose-500' : 'bg-emerald-500'}" style="width: ${percentage}%"></div>
      </div>
    `;
        container.appendChild(meter);
    });

    // Display budget alarm if any category exceeded
    const alarmEl = document.getElementById('budget-alarm');
    if (alarmEl) {
        if (totalExceededCount > 0) {
            alarmEl.classList.remove('hidden');
            alarmEl.innerText = `${TRANSLATIONS[lang].alertOverhead} (${totalExceededCount})`;
        } else {
            alarmEl.classList.add('hidden');
        }
    }
}

function renderRecentTransactionsList(transactions) {
    const container = document.getElementById('recent-transactions-list');
    container.innerHTML = '';

    const lang = state.language;
    const t = TRANSLATIONS[lang];

    if (transactions.length === 0) {
        container.innerHTML = `
      <div class="text-center py-8 text-xs font-bold text-slate-400">
        ${t.noTransactionsYet}
      </div>
    `;
        return;
    }

    transactions.forEach(tx => {
        const item = document.createElement('div');
        item.className = 'flex items-center justify-between py-3 hover:bg-slate-50 dark:hover:bg-slate-900/40 rounded-xl px-2 transition-all';

        const isIncome = tx.type === 'INCOME';
        const color = getCategoryColor(tx.category);
        const icon = getCategoryIcon(tx.category);

        // Formatting date neatly
        const dateFormatted = new Date(tx.date).toLocaleDateString(lang === 'vi' ? 'vi-VN' : 'en-US', { month: 'short', day: 'numeric', year: 'numeric' });

        item.innerHTML = `
      <div class="flex items-center gap-3 min-w-0">
        <div class="w-10 h-10 rounded-xl flex items-center justify-center text-white" style="background-color: ${color}">
          <i data-lucide="${icon}" class="w-5 h-5"></i>
        </div>
        <div class="min-w-0">
          <h5 class="text-xs font-extrabold text-slate-800 dark:text-white truncate">${tx.title}</h5>
          <div class="flex items-center gap-2 mt-0.5">
            <span class="text-[9px] font-extrabold uppercase tracking-wider px-1.5 py-0.5 rounded-md" style="background-color: ${color}20; color: ${color}">${tx.category}</span>
            <span class="text-[10px] font-semibold text-slate-400">${dateFormatted}</span>
          </div>
        </div>
      </div>
      <div class="text-right flex-shrink-0">
        <span class="text-xs font-black font-mono ${isIncome ? 'text-emerald-500' : 'text-slate-800 dark:text-white'}">
          ${isIncome ? '+' : '-'}${formatVND(tx.amount)}
        </span>
        ${tx.notes ? `<span class="block text-[9px] font-bold text-slate-400 mt-0.5 max-w-[120px] truncate">${tx.notes}</span>` : ''}
      </div>
    `;

        container.appendChild(item);
    });

    if (window.lucide) window.lucide.createIcons();
}

// Run initialization
if (document.readyState !== 'loading') {
    injectSharedLayout('dashboard');
    renderDashboard();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        injectSharedLayout('dashboard');
        renderDashboard();
    });
}
