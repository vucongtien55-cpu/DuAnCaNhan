// 12. JS controller for categories.html

const AVAILABLE_ICONS = [
    'utensils', 'car', 'shopping-bag', 'home', 'film', 'heart-pulse',
    'graduation-cap', 'gift', 'briefcase', 'laptop', 'trending-up', 'dollar-sign',
    'coffee', 'smartphone', 'sparkles', 'scissors', 'plane', 'gamepad-2'
];

const AVAILABLE_COLORS = [
    '#EF4444', '#F59E0B', '#10B981', '#06B6D4', '#3B82F6', '#8B5CF6',
    '#EC4899', '#F43F5E', '#14B8A6', '#6366F1', '#A855F7', '#64748B'
];

let selectedIcon = 'utensils';
let selectedColor = '#EF4444';

function selectIcon(iconName) {
    selectedIcon = iconName;
    const buttons = document.querySelectorAll('[data-select-icon]');
    buttons.forEach(btn => {
        const btnIcon = btn.getAttribute('data-select-icon');
        if (btnIcon === iconName) {
            btn.className = "p-2 rounded-xl bg-indigo-600 text-white shadow-sm flex items-center justify-center cursor-pointer border border-indigo-500";
        } else {
            btn.className = "p-2 rounded-xl bg-slate-50 dark:bg-slate-950 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 border border-slate-100 dark:border-slate-900 flex items-center justify-center cursor-pointer";
        }
    });
}
window.selectIcon = selectIcon;

function selectColor(colorHex) {
    selectedColor = colorHex;
    const buttons = document.querySelectorAll('[data-select-color]');
    buttons.forEach(btn => {
        const btnColor = btn.getAttribute('data-select-color');
        if (btnColor === colorHex) {
            btn.className = "w-7 h-7 rounded-full border-2 border-slate-800 dark:border-white shadow-md flex-shrink-0 cursor-pointer relative";
            // Add a small checkmark or center dot inside
            btn.innerHTML = '<span class="absolute inset-1.5 rounded-full bg-white dark:bg-slate-950"></span>';
        } else {
            btn.className = "w-7 h-7 rounded-full hover:scale-105 transition-all flex-shrink-0 cursor-pointer";
            btn.innerHTML = '';
        }
    });
}
window.selectColor = selectColor;

function initCategoriesPage() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Translate static headers
    document.getElementById('cat-header-title').innerText = lang === 'vi' ? 'Quản lý danh mục thu chi' : 'Category Manager';
    document.getElementById('cat-header-sub').innerText = lang === 'vi' ? 'Tự do cá nhân hóa các nhóm phân loại tài chính theo nhu cầu' : 'Personalize your financial groupings dynamically as needed';

    renderIconGrid();
    renderColorGrid();
    renderCategoriesList();

    // Handle addition
    const form = document.getElementById('add-category-form');
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        const name = document.getElementById('cat-name-input').value.trim();
        const type = document.getElementById('cat-type-select').value;

        if (!name) {
            showToast(lang === 'vi' ? 'Vui lòng nhập tên danh mục!' : 'Please enter category name!', 'error');
            return;
        }

        const targetList = type === 'EXPENSE' ? state.expenseCategories : state.incomeCategories;

        // Check duplication
        const duplicate = targetList.some(cat => cat.name.toLowerCase() === name.toLowerCase());
        if (duplicate) {
            showToast(lang === 'vi' ? 'Tên danh mục này đã tồn tại!' : 'This category name already exists!', 'error');
            return;
        }

        const newCat = {
            id: `cat-${Date.now()}`,
            name,
            icon: selectedIcon,
            color: selectedColor,
            type
        };

        targetList.push(newCat);
        saveUserData();
        showToast(lang === 'vi' ? 'Đã thêm danh mục thành công!' : 'Category added successfully!');

        // Reset form field
        document.getElementById('cat-name-input').value = '';

        renderCategoriesList();
    });
}

function renderIconGrid() {
    const grid = document.getElementById('icons-selection-grid');
    grid.innerHTML = '';

    AVAILABLE_ICONS.forEach(icon => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.setAttribute('data-select-icon', icon);
        btn.onclick = () => selectIcon(icon);

        if (icon === selectedIcon) {
            btn.className = "p-2 rounded-xl bg-indigo-600 text-white shadow-sm flex items-center justify-center cursor-pointer border border-indigo-500";
        } else {
            btn.className = "p-2 rounded-xl bg-slate-50 dark:bg-slate-950 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 border border-slate-100 dark:border-slate-900 flex items-center justify-center cursor-pointer";
        }

        btn.innerHTML = `<i data-lucide="${icon}" class="w-4 h-4"></i>`;
        grid.appendChild(btn);
    });
}

function renderColorGrid() {
    const grid = document.getElementById('colors-selection-grid');
    grid.innerHTML = '';

    AVAILABLE_COLORS.forEach(color => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.setAttribute('data-select-color', color);
        btn.style.backgroundColor = color;
        btn.onclick = () => selectColor(color);

        if (color === selectedColor) {
            btn.className = "w-7 h-7 rounded-full border-2 border-slate-800 dark:border-white shadow-md flex-shrink-0 cursor-pointer relative";
            btn.innerHTML = '<span class="absolute inset-1.5 rounded-full bg-white dark:bg-slate-950"></span>';
        } else {
            btn.className = "w-7 h-7 rounded-full hover:scale-105 transition-all flex-shrink-0 cursor-pointer";
        }

        grid.appendChild(btn);
    });
}

function renderCategoriesList() {
    const expensesContainer = document.getElementById('expenses-cats-container');
    const incomesContainer = document.getElementById('incomes-cats-container');
    const lang = state.language;

    expensesContainer.innerHTML = '';
    incomesContainer.innerHTML = '';

    // Render Expenses list
    state.expenseCategories.forEach(cat => {
        const item = document.createElement('div');
        item.className = 'flex items-center justify-between p-3 border border-slate-100 dark:border-slate-800 rounded-2xl bg-slate-50/30 dark:bg-slate-950/20';
        item.innerHTML = `
      <div class="flex items-center gap-2.5 min-w-0">
        <div class="w-8 h-8 rounded-lg flex items-center justify-center text-white flex-shrink-0" style="background-color: ${cat.color}">
          <i data-lucide="${cat.icon}" class="w-4 h-4"></i>
        </div>
        <span class="text-xs font-bold text-slate-700 dark:text-slate-300 truncate">${cat.name}</span>
      </div>
      <button onclick="deleteCategory('${cat.id}', 'EXPENSE')" class="p-1 text-slate-400 hover:text-rose-500 rounded-lg hover:bg-rose-500/10 transition-all cursor-pointer">
        <i data-lucide="trash" class="w-3.5 h-3.5"></i>
      </button>
    `;
        expensesContainer.appendChild(item);
    });

    // Render Incomes list
    state.incomeCategories.forEach(cat => {
        const item = document.createElement('div');
        item.className = 'flex items-center justify-between p-3 border border-slate-100 dark:border-slate-800 rounded-2xl bg-slate-50/30 dark:bg-slate-950/20';
        item.innerHTML = `
      <div class="flex items-center gap-2.5 min-w-0">
        <div class="w-8 h-8 rounded-lg flex items-center justify-center text-white flex-shrink-0" style="background-color: ${cat.color}">
          <i data-lucide="${cat.icon}" class="w-4 h-4"></i>
        </div>
        <span class="text-xs font-bold text-slate-700 dark:text-slate-300 truncate">${cat.name}</span>
      </div>
      <button onclick="deleteCategory('${cat.id}', 'INCOME')" class="p-1 text-slate-400 hover:text-rose-500 rounded-lg hover:bg-rose-500/10 transition-all cursor-pointer">
        <i data-lucide="trash" class="w-3.5 h-3.5"></i>
      </button>
    `;
        incomesContainer.appendChild(item);
    });

    if (window.lucide) window.lucide.createIcons();
}

function deleteCategory(id, type) {
    const lang = state.language;
    const targetList = type === 'EXPENSE' ? state.expenseCategories : state.incomeCategories;

    if (targetList.length <= 1) {
        showToast(lang === 'vi' ? 'Không thể xóa danh mục cuối cùng!' : 'Cannot delete the very last category!', 'error');
        return;
    }

    const index = targetList.findIndex(c => c.id === id);
    if (index !== -1) {
        targetList.splice(index, 1);
        saveUserData();
        showToast(lang === 'vi' ? 'Đã xóa danh mục thành công!' : 'Category deleted successfully!', 'warning');
        renderCategoriesList();
    }
}
window.deleteCategory = deleteCategory;

if (document.readyState !== 'loading') {
    injectSharedLayout('categories');
    initCategoriesPage();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        injectSharedLayout('categories');
        initCategoriesPage();
    });
}
