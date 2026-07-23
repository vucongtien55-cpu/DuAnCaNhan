// 11. JS controller for budgets.html

function initBudgetsPage() {
    console.log("Initializing budgets page...");
    const lang = state.language || 'vi';
    const t = TRANSLATIONS[lang] || TRANSLATIONS['vi'];

    // Translate headers defensively
    const titleEl = document.getElementById('budgets-header-title');
    if (titleEl) titleEl.innerText = t.budgetTab || 'Hạn mức ngân sách';

    const subEl = document.getElementById('budgets-header-sub');
    if (subEl) subEl.innerText = lang === 'vi' ? 'Thiết lập hạn mức tối đa cho từng nhóm chi tiêu hằng tháng' : 'Set monthly expense thresholds per spending category';

    const saveBtn = document.getElementById('save-budgets-btn');
    if (saveBtn) saveBtn.innerText = lang === 'vi' ? 'Lưu tất cả hạn mức' : 'Save All Limits';

    renderBudgetInputs();

    // Form submit handler
    const form = document.getElementById('budgets-form');
    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();

            const inputs = document.querySelectorAll('[data-budget-cat]');
            const newBudgets = [];

            inputs.forEach(input => {
                const category = input.getAttribute('data-budget-cat');
                const limit = Number(input.value) || 0;
                if (category && limit >= 0) {
                    newBudgets.push({ category, limit });
                }
            });

            state.budgets = newBudgets;
            saveUserData();

            showToast(lang === 'vi' ? 'Hạn mức ngân sách đã được cập nhật!' : 'Budget limits updated successfully!');
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1200);
        });
    } else {
        console.warn("budgets-form not found in DOM.");
    }
}

function renderBudgetInputs() {
    const container = document.getElementById('budgets-inputs-container');
    if (!container) {
        console.warn("budgets-inputs-container not found in DOM.");
        return;
    }
    container.innerHTML = '';

    // Load current expense categories (both defaults and custom)
    const expenseCats = (state.expenseCategories || []).filter(c => c && c.name);
    const budgets = (state.budgets || []).filter(b => b && b.category);

    console.log("Rendering budget inputs. Categories count:", expenseCats.length, "Budgets count:", budgets.length);
    console.log("Expense categories detail:", expenseCats);
    console.log("Budgets detail:", budgets);

    expenseCats.forEach(cat => {
        const activeLimitObj = budgets.find(b => b && b.category === cat.name);
        const limitValue = activeLimitObj ? activeLimitObj.limit : 0;

        const row = document.createElement('div');
        row.className = 'flex flex-col sm:flex-row sm:items-center justify-between py-4 gap-3';

        const color = cat.color || '#64748b';
        const icon = cat.icon || 'help-circle';

        row.innerHTML = `
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg flex items-center justify-center text-white flex-shrink-0" style="background-color: ${color}">
          <i data-lucide="${icon}" class="w-4.5 h-4.5"></i>
        </div>
        <div>
          <h5 class="text-xs font-extrabold text-slate-800 dark:text-white">${translateCategory(cat.name)}</h5>
          <span class="text-[9px] font-bold text-slate-400">${state.language === 'vi' ? 'Danh mục Chi phí' : 'Expense Category'}</span>
        </div>
      </div>
      
      <!-- Input limit -->
      <div class="w-full sm:w-48 relative flex items-center">
        <div class="absolute left-3.5 text-xs font-black text-slate-400">₫</div>
        <input type="number" data-budget-cat="${cat.name}" value="${limitValue}" min="0" step="1" required class="w-full pl-8 pr-4 py-2 text-xs font-bold font-mono rounded-xl border border-slate-200 dark:border-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-800 dark:text-white bg-transparent" placeholder="${state.language === 'vi' ? 'Nhập hạn mức...' : 'Enter limit...'}" />
      </div>
    `;

        container.appendChild(row);
    });

    if (window.lucide) window.lucide.createIcons();
}

if (document.readyState !== 'loading') {
    console.log("DOM already ready in budgets.js");
    injectSharedLayout('budgets');
    initBudgetsPage();
} else {
    console.log("Waiting for DOMContentLoaded in budgets.js");
    document.addEventListener('DOMContentLoaded', () => {
        injectSharedLayout('budgets');
        initBudgetsPage();
    });
}
