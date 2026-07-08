// 9. JS controller for record.html

let currentType = 'EXPENSE'; // 'EXPENSE' or 'INCOME'
let editTxId = null;

function setRecordType(type) {
    currentType = type;

    const expenseBtn = document.getElementById('type-expense-btn');
    const incomeBtn = document.getElementById('type-income-btn');

    if (type === 'EXPENSE') {
        expenseBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer bg-white dark:bg-slate-900 text-rose-500 shadow-sm border border-slate-200/20 dark:border-slate-800";
        incomeBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer text-slate-400 dark:text-slate-500 hover:text-slate-700 dark:hover:text-slate-300";
    } else {
        incomeBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer bg-white dark:bg-slate-900 text-emerald-500 shadow-sm border border-slate-200/20 dark:border-slate-800";
        expenseBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer text-slate-400 dark:text-slate-500 hover:text-slate-700 dark:hover:text-slate-300";
    }

    populateCategories();
}
window.setRecordType = setRecordType;

function populateCategories() {
    const select = document.getElementById('tx-category');
    select.innerHTML = '';

    const categoriesList = currentType === 'EXPENSE'
        ? state.expenseCategories
        : state.incomeCategories;

    categoriesList.forEach(cat => {
        const option = document.createElement('option');
        option.value = cat.name;
        option.innerText = cat.name;
        select.appendChild(option);
    });
}

function initRecordPage() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Translate labels
    document.getElementById('back-label').innerText = lang === 'vi' ? 'Quay lại Tổng quan' : 'Back to Dashboard';
    document.getElementById('type-label').innerText = lang === 'vi' ? 'Phân loại' : 'Classification Type';
    document.getElementById('type-expense-btn').innerText = t.expenseLabel;
    document.getElementById('type-income-btn').innerText = t.incomeLabel;
    document.getElementById('amount-label').innerText = t.amountLabel;
    document.getElementById('title-label').innerText = t.titleLabel;
    document.getElementById('category-label').innerText = t.categoryLabel;
    document.getElementById('date-label').innerText = t.dateLabel;
    document.getElementById('notes-label').innerText = t.notesLabel;
    document.getElementById('tx-notes').placeholder = t.notesPlaceholder;
    document.getElementById('tx-title').placeholder = t.titlePlaceholder;
    document.getElementById('tx-amount').placeholder = t.amountPlaceholder;
    document.getElementById('cancel-btn').innerText = t.cancel;
    document.getElementById('save-btn').innerText = t.save;

    // Check URL query parameters for 'edit'
    const urlParams = new URLSearchParams(window.location.search);
    editTxId = urlParams.get('edit');

    // Set default date to today
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('tx-date').value = today;

    if (editTxId) {
        // Mode: Edit
        document.getElementById('form-header').innerText = t.editRecordHeader;

        const txToEdit = state.transactions.find(tx => tx.id === editTxId);
        if (txToEdit) {
            currentType = txToEdit.type;
            setRecordType(currentType);

            document.getElementById('tx-amount').value = txToEdit.amount;
            document.getElementById('tx-title').value = txToEdit.title;
            document.getElementById('tx-date').value = txToEdit.date;
            document.getElementById('tx-notes').value = txToEdit.notes || '';

            // Delay selecting the category until population finishes
            setTimeout(() => {
                document.getElementById('tx-category').value = txToEdit.category;
            }, 50);
        } else {
            showToast(lang === 'vi' ? 'Giao dịch không tồn tại!' : 'Transaction does not exist!', 'error');
            setTimeout(() => window.location.href = 'dashboard.html', 1000);
        }
    } else {
        // Mode: Create
        document.getElementById('form-header').innerText = t.addRecordHeader;
        setRecordType('EXPENSE');
    }

    // Handle form submission
    const form = document.getElementById('record-form');
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        const amount = Number(document.getElementById('tx-amount').value);
        const title = document.getElementById('tx-title').value.trim();
        const category = document.getElementById('tx-category').value;
        const date = document.getElementById('tx-date').value;
        const notes = document.getElementById('tx-notes').value.trim();

        if (amount <= 0 || !title || !category || !date) {
            showToast(lang === 'vi' ? 'Vui lòng điền đủ các thông tin hợp lệ!' : 'Please fill in all valid fields!', 'error');
            return;
        }

        const currentTx = {
            id: editTxId || `tx-${Date.now()}`,
            title,
            amount,
            type: currentType,
            category,
            date,
            notes
        };

        if (editTxId) {
            // Replace existing item
            const index = state.transactions.findIndex(tx => tx.id === editTxId);
            if (index !== -1) {
                state.transactions[index] = currentTx;
            }
        } else {
            // Add new item to front of the stack
            state.transactions.unshift(currentTx);
        }

        // Save transaction state back to browser memory
        saveUserData();

        // Check Budget exceeded warnings
        if (currentType === 'EXPENSE') {
            const budgetLimit = state.budgets.find(b => b.category === category);
            if (budgetLimit) {
                // Calculate total spent on this category this month
                const todayObj = new Date(date);
                const curYear = todayObj.getFullYear();
                const curMonth = todayObj.getMonth();

                let totalSpent = 0;
                state.transactions.forEach(tx => {
                    if (tx.type === 'EXPENSE' && tx.category === category) {
                        const txD = new Date(tx.date);
                        if (txD.getFullYear() === curYear && txD.getMonth() === curMonth) {
                            totalSpent += Number(tx.amount);
                        }
                    }
                });

                if (totalSpent > budgetLimit.limit) {
                    showToast(`${t.alertOverhead}: ${category} (${formatVND(totalSpent)} / ${formatVND(budgetLimit.limit)})`, 'warning');
                } else {
                    showToast(lang === 'vi' ? 'Lưu giao dịch thành công!' : 'Transaction saved successfully!');
                }
            } else {
                showToast(lang === 'vi' ? 'Lưu giao dịch thành công!' : 'Transaction saved successfully!');
            }
        } else {
            showToast(lang === 'vi' ? 'Lưu giao dịch thành công!' : 'Transaction saved successfully!');
        }

        // Redirect back to transactions ledger or dashboard
        setTimeout(() => {
            window.location.href = editTxId ? 'transactions.html' : 'dashboard.html';
        }, 1500);
    });
}

if (document.readyState !== 'loading') {
    injectSharedLayout('record');
    initRecordPage();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        injectSharedLayout('record');
        initRecordPage();
    });
}
