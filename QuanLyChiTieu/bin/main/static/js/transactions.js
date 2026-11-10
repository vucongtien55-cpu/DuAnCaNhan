// 10. JS controller for transactions.html

function populateCategoryFilter() {
    const select = document.getElementById('filter-category');
    const lang = state.language;
    select.innerHTML = '';

    // Default first option
    const defaultOpt = document.createElement('option');
    defaultOpt.value = 'ALL';
    defaultOpt.innerText = lang === 'vi' ? 'Tất cả danh mục' : 'All Categories';
    select.appendChild(defaultOpt);

    // Combine custom & default categories
    const allCats = [...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES, ...state.expenseCategories, ...state.incomeCategories];

    // Get unique names to prevent duplicates
    const uniqueNames = [];
    allCats.forEach(c => {
        if (!uniqueNames.includes(c.name)) {
            uniqueNames.push(c.name);
            const option = document.createElement('option');
            option.value = c.name;
            option.innerText = c.name;
            select.appendChild(option);
        }
    });
}

function applyFilters() {
    const searchQuery = document.getElementById('search-input').value.toLowerCase().trim();
    const typeFilter = document.getElementById('filter-type').value;
    const categoryFilter = document.getElementById('filter-category').value;

    let list = state.transactions || [];

    // 1. Filter by Search Query (title, category, or notes)
    if (searchQuery) {
        list = list.filter(tx =>
            tx.title.toLowerCase().includes(searchQuery) ||
            tx.category.toLowerCase().includes(searchQuery) ||
            (tx.notes && tx.notes.toLowerCase().includes(searchQuery))
        );
    }

    // 2. Filter by Type (EXPENSE or INCOME)
    if (typeFilter !== 'ALL') {
        list = list.filter(tx => tx.type === typeFilter);
    }

    // 3. Filter by Category
    if (categoryFilter !== 'ALL') {
        list = list.filter(tx => tx.category === categoryFilter);
    }

    renderLedger(list);
}
window.applyFilters = applyFilters;

function renderLedger(list) {
    const container = document.getElementById('ledger-rows-container');
    container.innerHTML = '';

    const lang = state.language;

    if (list.length === 0) {
        document.getElementById('empty-ledger-state').classList.remove('hidden');
        return;
    }

    document.getElementById('empty-ledger-state').classList.add('hidden');

    list.forEach(tx => {
        const row = document.createElement('tr');
        row.className = "border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50/50 dark:hover:bg-slate-900/10 transition-all text-xs font-medium";

        const isIncome = tx.type === 'INCOME';
        const color = getCategoryColor(tx.category);
        const icon = getCategoryIcon(tx.category);
        const dateFormatted = new Date(tx.date).toLocaleDateString(lang === 'vi' ? 'vi-VN' : 'en-US', { year: 'numeric', month: '2-digit', day: '2-digit' });

        row.innerHTML = `
      <!-- Date -->
      <td class="p-4 font-mono text-slate-500 font-bold">${dateFormatted}</td>
      
      <!-- Title & Category Info -->
      <td class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center text-white" style="background-color: ${color}">
            <i data-lucide="${icon}" class="w-4 h-4"></i>
          </div>
          <div>
            <h5 class="font-extrabold text-slate-800 dark:text-white">${tx.title}</h5>
            <span class="text-[9px] font-extrabold tracking-wider uppercase" style="color: ${color}">${tx.category}</span>
          </div>
        </div>
      </td>
      
      <!-- Notes -->
      <td class="p-4 text-slate-400 max-w-[200px] truncate font-sans">${tx.notes || '-'}</td>
      
      <!-- Amount -->
      <td class="p-4 text-right font-black font-mono ${isIncome ? 'text-emerald-500' : 'text-slate-800 dark:text-white'}">
        ${isIncome ? '+' : '-'}${formatVND(tx.amount)}
      </td>
      
      <!-- Actions buttons -->
      <td class="p-4 text-center">
        <div class="flex items-center justify-center gap-1.5">
          <a href="record.html?edit=${tx.id}" class="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-indigo-500 transition-all cursor-pointer">
            <i data-lucide="edit" class="w-4 h-4"></i>
          </a>
          <button onclick="deleteTransaction('${tx.id}')" class="p-1.5 hover:bg-rose-500/10 rounded-lg text-rose-500 transition-all cursor-pointer">
            <i data-lucide="trash-2" class="w-4 h-4"></i>
          </button>
        </div>
      </td>
    `;

        container.appendChild(row);
    });

    if (window.lucide) window.lucide.createIcons();
}

function deleteTransaction(id) {
    const lang = state.language;
    const index = state.transactions.findIndex(tx => tx.id === id);
    if (index !== -1) {
        state.transactions.splice(index, 1);
        saveUserData();
        showToast(lang === 'vi' ? 'Đã xóa giao dịch thành công!' : 'Transaction deleted successfully!', 'warning');
        applyFilters();
    }
}
window.deleteTransaction = deleteTransaction;

// Export data
function exportData() {
    const lang = state.language;
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(state.transactions, null, 2));
    const dlAnchor = document.createElement('a');
    dlAnchor.setAttribute("href", dataStr);
    dlAnchor.setAttribute("download", `chi_tieu_export_${Date.now()}.json`);
    document.body.appendChild(dlAnchor);
    dlAnchor.click();
    dlAnchor.remove();
    showToast(lang === 'vi' ? 'Xuất dữ liệu JSON thành công!' : 'JSON export completed!');
}
window.exportData = exportData;

// Import data
function importData(event) {
    const lang = state.language;
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const imported = JSON.parse(e.target.result);
            if (Array.isArray(imported)) {
                // Simple verification
                const validated = imported.filter(item => item.title && item.amount && item.type);
                if (validated.length > 0) {
                    state.transactions = [...validated, ...state.transactions];
                    saveUserData();
                    showToast(lang === 'vi' ? `Nhập thành công ${validated.length} giao dịch!` : `Successfully imported ${validated.length} entries!`);
                    applyFilters();
                } else {
                    showToast(lang === 'vi' ? 'Định dạng dữ liệu không phù hợp!' : 'Invalid data format!', 'error');
                }
            } else {
                showToast(lang === 'vi' ? 'Định dạng tệp tin phải là danh sách!' : 'Import source file must contain a list!', 'error');
            }
        } catch(err) {
            showToast(lang === 'vi' ? 'Không thể đọc tệp tin JSON này!' : 'Unable to parse this JSON file!', 'error');
        }
    };
    reader.readAsText(file);
}
window.importData = importData;

function initTransactionsPage() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Translate static headers
    document.getElementById('transactions-header-title').innerText = t.transactionListTab;
    document.getElementById('transactions-header-sub').innerText = lang === 'vi' ? 'Danh sách chi tiết các khoản thu và chi tiêu' : 'Comprehensive list of expenses and cashflow entries';
    document.getElementById('export-btn-label').innerText = t.exportCsv;
    document.getElementById('import-btn-label').innerText = t.importCsv;
    document.getElementById('search-input').placeholder = lang === 'vi' ? 'Tìm kiếm giao dịch, danh mục, ghi chú...' : 'Search transactions, categories, notes...';

    document.getElementById('opt-all-types').innerText = lang === 'vi' ? 'Tất cả phân loại' : 'All transaction types';
    document.getElementById('opt-expense-type').innerText = t.expenseLabel;
    document.getElementById('opt-income-type').innerText = t.incomeLabel;

    document.getElementById('th-date').innerText = t.dateLabel;
    document.getElementById('th-title').innerText = lang === 'vi' ? 'Giao dịch / Danh mục' : 'Transactions / Categories';
    document.getElementById('th-notes').innerText = lang === 'vi' ? 'Ghi chú thêm' : 'Notes';
    document.getElementById('th-amount').innerText = t.amountLabel;
    document.getElementById('th-actions').innerText = lang === 'vi' ? 'Thao tác' : 'Actions';

    document.getElementById('empty-state-title').innerText = lang === 'vi' ? 'Chưa tìm thấy kết quả phù hợp' : 'No matching results found';
    document.getElementById('empty-state-desc').innerText = lang === 'vi' ? 'Hãy thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.' : 'Try adjusting the filters or searching keyword.';

    populateCategoryFilter();
    applyFilters();
}

if (document.readyState !== 'loading') {
    injectSharedLayout('transactions');
    initTransactionsPage();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        injectSharedLayout('transactions');
        initTransactionsPage();
    });
}
