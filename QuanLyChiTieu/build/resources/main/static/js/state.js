// 3. Module quản lý Trạng thái Toàn cục và hiển thị giao diện
const state = {
  user: localStorage.getItem('chi_tieu_user') || null,
  language: localStorage.getItem('chi_tieu_lang') || 'vi',
  theme: localStorage.getItem('chi_tieu_theme') || 'light',
  transactions: [],
  budgets: [],
  expenseCategories: [],
  incomeCategories: [],
  savingsJars: [],
  backendConnected: false,
  backendSyncing: false
};

// Tự động xác định URL backend để hỗ trợ proxy Vite, môi trường phát triển local và hosting Spring Boot trực tiếp
const BACKEND_API_URL = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
    ? (window.location.port === '3000' ? '/api' : 'http://localhost:8081/api')
    : '/api';
window.BACKEND_API_URL = BACKEND_API_URL;

// để các file JavaScript khác có thể truy cập cùng một đối tượng state
window.state = state;

// Kiểm tra trạng thái xác thực và điều hướng nếu cần thiết
function checkAuth() {
  const path = window.location.pathname;
  const isAuthPage = path.endsWith('index.html') || path.endsWith('register.html') || path.endsWith('forgot.html') || path.endsWith('phone-login.html') || path === '/' || path === '';

  if (state.user) {
    // Nếu đã đăng nhập, tải dữ liệu người dùng
    loadUserData();

    // Điều hướng nếu người dùng cố truy cập các trang đăng nhập/xác thực
    if (isAuthPage) {
      window.location.href = 'dashboard.html';
    }
  } else {
    // Nếu chưa đăng nhập và cố truy cập trang ứng dụng, điều hướng về trang đăng nhập
    if (!isAuthPage) {
      window.location.href = 'index.html';
    }
  }
}

// Định dạng tiền tệ VND
function formatVND(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}
window.formatVND = formatVND;

// Các hàm hỗ trợ lấy màu sắc và biểu tượng của danh mục
function getCategoryIcon(catName) {
  const allCats = [...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES, ...state.expenseCategories, ...state.incomeCategories];
  const found = allCats.find(c => c.name === catName);
  return found ? found.icon : 'help-circle';
}
window.getCategoryIcon = getCategoryIcon;

//Hàm Lấy màu sắc của một danh mục để hiển thị trên giao diện.
function getCategoryColor(catName) {
  const allCats = [...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES, ...state.expenseCategories, ...state.incomeCategories];
  const found = allCats.find(c => c.name === catName);
  return found ? found.color : '#64748b';
}
window.getCategoryColor = getCategoryColor;

// Hàm hỗ trợ hiển thị thông báo (Toast)
function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) {
    const freshContainer = document.createElement('div');
    freshContainer.id = 'toast-container';
    freshContainer.className = 'fixed top-5 right-5 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none';
    document.body.appendChild(freshContainer);
  }
  const targetContainer = document.getElementById('toast-container');

  const toast = document.createElement('div');
  toast.className = `p-4 rounded-xl shadow-lg border transition-all duration-300 transform translate-y-2 opacity-0 flex items-center gap-3 bg-white dark:bg-slate-900 pointer-events-auto ${
      type === 'success' ? 'border-emerald-500 text-emerald-800 dark:text-emerald-400' :
          type === 'error' ? 'border-rose-500 text-rose-800 dark:text-rose-400' :
              'border-amber-500 text-amber-800 dark:text-amber-400'
  }`;

  const iconName = type === 'success' ? 'check-circle' : type === 'error' ? 'x-circle' : 'alert-circle';
  toast.innerHTML = `
    <i data-lucide="${iconName}" class="flex-shrink-0"></i>
    <span class="text-xs font-bold font-sans">${message}</span>
  `;

  targetContainer.appendChild(toast);
  if (window.lucide) window.lucide.createIcons();

  setTimeout(() => {
    toast.classList.remove('translate-y-2', 'opacity-0');
  }, 10);

  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-[-10px]');
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}
window.showToast = showToast;

// Các hàm hỗ trợ Lưu và Tải dữ liệu người dùng với sự hỗ trợ từ DB Spring Boot
async function loadUserData() {
  if (!state.user) return;
  const email = state.user;

  // 1. Tải nhanh ban đầu từ local storage
  try {
    const localTxs = localStorage.getItem(`chi_tieu_${email}_transactions`);
    state.transactions = localTxs ? JSON.parse(localTxs) : [];

    const localBuds = localStorage.getItem(`chi_tieu_${email}_budgets`);
    state.budgets = localBuds ? JSON.parse(localBuds) : [];

    const localExp = localStorage.getItem(`chi_tieu_${email}_expense_categories`);
    state.expenseCategories = localExp ? JSON.parse(localExp) : [...EXPENSE_CATEGORIES];

    const localInc = localStorage.getItem(`chi_tieu_${email}_income_categories`);
    state.incomeCategories = localInc ? JSON.parse(localInc) : [...INCOME_CATEGORIES];

    const localSavings = localStorage.getItem(`chi_tieu_${email}_savings_jars`);
    state.savingsJars = localSavings ? JSON.parse(localSavings) : [...INITIAL_SAVINGS_JARS];
  } catch (e) {
    state.transactions = [];
    state.budgets = [];
    state.expenseCategories = [...EXPENSE_CATEGORIES];
    state.incomeCategories = [...INCOME_CATEGORIES];
    state.savingsJars = [...INITIAL_SAVINGS_JARS];
  }

  // Đảm bảo các khóa dữ liệu tồn tại ban đầu
  saveUserDataLocally();

  // 2. Chủ động thăm dò và lấy dữ liệu từ backend nếu đang hoạt động
  try {
    const probe = await fetch(`${BACKEND_API_URL}/users/profile?email=${encodeURIComponent(email)}`);
    if (probe.ok) {
      state.backendConnected = true;
      updateBackendBadge();

      // Lấy và cập nhật các danh mục tùy chỉnh
      const catRes = await fetch(`${BACKEND_API_URL}/categories?email=${encodeURIComponent(email)}`);
      if (catRes.ok) {
        const backendCats = await catRes.json();
        if (Array.isArray(backendCats)) {
          const customExpense = backendCats.filter(c => c && c.name && c.type && c.type.toLowerCase() === 'expense');
          const customIncome = backendCats.filter(c => c && c.name && c.type && c.type.toLowerCase() === 'income');

          state.expenseCategories = [...EXPENSE_CATEGORIES, ...customExpense];
          state.incomeCategories = [...INCOME_CATEGORIES, ...customIncome];
        }
      }

      // Lấy và cập nhật hạn mức chi tiêu (budgets)
      const budRes = await fetch(`${BACKEND_API_URL}/budgets?email=${encodeURIComponent(email)}`);
      if (budRes.ok) {
        const backendBuds = await budRes.json();
        if (Array.isArray(backendBuds)) {
          state.budgets = backendBuds
              .filter(b => b && b.category)
              .map(b => ({
                category: b.category,
                limit: Number(b.limitAmount) || 0,
                id: b.id
              }));
        }
      }

      // Lấy và cập nhật các giao dịch
      const txRes = await fetch(`${BACKEND_API_URL}/transactions?email=${encodeURIComponent(email)}`);
      if (txRes.ok) {
        const backendTxs = await txRes.json();
        if (Array.isArray(backendTxs)) {
          const backendMappedTxs = backendTxs
              .filter(t => t)
              .map(t => ({
                id: String(t.id),
                title: t.title || t.note || t.notes || t.category || 'Giao dịch',
                date: t.date || new Date().toISOString().split('T')[0],
                amount: Number(t.amount) || 0,
                type: (t.type || 'EXPENSE').toUpperCase(),
                category: t.category || 'Khác (Chi tiêu)',
                notes: t.note || t.notes || ''
              }));

          const mergedByKey = new Map();

          function getTransactionKey(tx) {
            return [
              tx.date || '',
              tx.type || '',
              tx.category || '',
              Number(tx.amount) || 0
            ].join('|');
          }

          (state.transactions || []).forEach(tx => {
            if (tx) {
              mergedByKey.set(getTransactionKey(tx), tx);
            }
          });

          backendMappedTxs.forEach(tx => {
            if (tx) {
              const key = getTransactionKey(tx);
              const existingTx = mergedByKey.get(key);

              mergedByKey.set(key, {
                ...existingTx,
                ...tx,
                title: tx.title || existingTx?.title || 'Giao dịch',
                notes: tx.notes || existingTx?.notes || ''
              });
            }
          });

          state.transactions = Array.from(mergedByKey.values());
        }
      }

      // Lấy và cập nhật các hũ tiết kiệm
      const savingsRes = await fetch(`${BACKEND_API_URL}/savings-jars?email=${encodeURIComponent(email)}`);
      if (savingsRes.ok) {
        const backendSavings = await savingsRes.json();
        if (Array.isArray(backendSavings)) {
          state.savingsJars = backendSavings
              .filter(s => s)
              .map(s => ({
                id: s.id,
                name: s.name,
                targetAmount: Number(s.targetAmount) || 0,
                currentAmount: Number(s.currentAmount) || 0,
                color: s.color || '#3b82f6',
                icon: s.icon || 'piggy-bank'
              }));
        }
      }

      // Lưu cục bộ vào bộ nhớ đệm (cache)
      saveUserDataLocally();

      // Kích hoạt cập nhật trực tiếp trên các trang hiện tại
      if (typeof renderDashboard === 'function') renderDashboard();
      if (typeof applyFilters === 'function') applyFilters();
      if (typeof populateCategoryFilter === 'function') populateCategoryFilter();
      if (typeof renderBudgetInputs === 'function') renderBudgetInputs();
      if (typeof renderCategoriesList === 'function') renderCategoriesList();
      if (typeof populateCategories === 'function') populateCategories();
      if (typeof renderSavingsJars === 'function') renderSavingsJars();
    } else {
      state.backendConnected = false;
      updateBackendBadge();
    }
  } catch (err) {
    state.backendConnected = false;
    updateBackendBadge();
  }
}
window.loadUserData = loadUserData;

function saveUserDataLocally() {
  if (!state.user) return;
  const email = state.user;
  localStorage.setItem(`chi_tieu_${email}_transactions`, JSON.stringify(state.transactions));
  localStorage.setItem(`chi_tieu_${email}_budgets`, JSON.stringify(state.budgets));
  localStorage.setItem(`chi_tieu_${email}_expense_categories`, JSON.stringify(state.expenseCategories));
  localStorage.setItem(`chi_tieu_${email}_income_categories`, JSON.stringify(state.incomeCategories));
  localStorage.setItem(`chi_tieu_${email}_savings_jars`, JSON.stringify(state.savingsJars));
}

async function saveUserData() {
  if (!state.user) return;
  const email = state.user;

  // 1. Luôn lưu vào local storage ngay lập tức
  saveUserDataLocally();

  // 2. Thực hiện đồng bộ chạy ngầm lên Spring Boot nếu có kết nối
  if (state.backendConnected) {
    state.backendSyncing = true;
    try {
      // Đồng bộ Giao dịch
      await fetch(`${BACKEND_API_URL}/transactions/sync?email=${encodeURIComponent(email)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(state.transactions)
      });

      // Đồng bộ Hạn mức (Budgets)
      await fetch(`${BACKEND_API_URL}/budgets/sync?email=${encodeURIComponent(email)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(state.budgets)
      });

      // Đồng bộ Hũ tiết kiệm
      await fetch(`${BACKEND_API_URL}/savings-jars/sync?email=${encodeURIComponent(email)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(state.savingsJars)
      });

      // Đồng bộ danh mục tùy chỉnh (chỉ lưu các mục không phải mặc định)
      const customCats = [
        ...state.expenseCategories.filter(c => !EXPENSE_CATEGORIES.some(dc => dc.name === c.name)),
        ...state.incomeCategories.filter(c => !INCOME_CATEGORIES.some(dc => dc.name === c.name))
      ];
      await fetch(`${BACKEND_API_URL}/categories/sync?email=${encodeURIComponent(email)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(customCats)
      });

      console.log("Đã đồng bộ thành công tất cả thay đổi cục bộ lên PostgreSQL qua Spring Boot!");
    } catch (e) {
      console.warn("Đồng bộ chạy ngầm thất bại:", e);
    } finally {
      state.backendSyncing = false;
    }
  }
}
window.saveUserData = saveUserData;

// Chuyển đổi Giao diện (Sáng/Tối)
function toggleTheme() {
  state.theme = state.theme === 'light' ? 'dark' : 'light';
  localStorage.setItem('chi_tieu_theme', state.theme);
  applyTheme();
}
window.toggleTheme = toggleTheme;

function applyTheme() {
  if (state.theme === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
  // Cập nhật biểu tượng chuyển đổi chủ đề trên toàn trang nếu tồn tại
  const themeBtnSpan = document.querySelector('#theme-toggle-span');
  if (themeBtnSpan) {
    const lang = state.language === 'en' ? 'en' : 'vi';
    themeBtnSpan.innerText = state.theme === 'light' ? TRANSLATIONS[lang].themeDark : TRANSLATIONS[lang].themeLight;
  }
  const themeIcon = document.querySelector('[data-theme-icon]');
  if (themeIcon) {
    themeIcon.setAttribute('data-lucide', state.theme === 'light' ? 'moon' : 'sun');
  }
  if (window.lucide) window.lucide.createIcons();
}
window.applyTheme = applyTheme;

// Chuyển đổi Ngôn ngữ
function toggleLanguage() {
  state.language = state.language === 'vi' ? 'en' : 'vi';
  localStorage.setItem('chi_tieu_lang', state.language);
  window.location.reload();
}
window.toggleLanguage = toggleLanguage;

// Hàm hỗ trợ đăng xuất
function logout() {
  localStorage.removeItem('chi_tieu_user');
  state.user = null;
  window.location.href = 'index.html';
}
window.logout = logout;

// Xóa và đặt lại toàn bộ dữ liệu người dùng cục bộ và backend hoàn toàn (đưa mọi thứ về 0)
async function resetAllUserData() {
  if (!state.user) return;
  const email = state.user;
  const isVi = state.language === 'vi';

  const confirmMsg = isVi
      ? "⚠️ CHÚ Ý: Bạn có chắc chắn muốn xóa toàn bộ dữ liệu (giao dịch, hạn mức chi tiêu, danh mục tự tạo) của tài khoản này trên cả trình duyệt và máy chủ không?\n\nThao tác này sẽ đưa tất cả số dư và biểu đồ về 0đ và không thể khôi phục lại!"
      : "⚠️ WARNING: Are you sure you want to permanently delete all data (transactions, budget limits, custom categories) for this account on both this device and the database server?\n\nThis will reset your entire cashflow to 0 and cannot be undone!";

  if (!confirm(confirmMsg)) {
    return;
  }

  // 1. Xóa tất cả các biến đã lưu trong bộ nhớ đệm (local storage) của trình duyệt
  localStorage.removeItem(`chi_tieu_${email}_transactions`);
  localStorage.removeItem(`chi_tieu_${email}_budgets`);
  localStorage.removeItem(`chi_tieu_${email}_expense_categories`);
  localStorage.removeItem(`chi_tieu_${email}_income_categories`);
  localStorage.removeItem(`chi_tieu_${email}_savings_jars`);

  // 2. Xóa các biến trạng thái toàn cục trong bộ nhớ máy
  state.transactions = [];
  state.budgets = [];
  state.expenseCategories = [...EXPENSE_CATEGORIES];
  state.incomeCategories = [...INCOME_CATEGORIES];
  state.savingsJars = [];

  saveUserDataLocally();

  // 3. Lệnh cho cơ sở dữ liệu backend xóa các bản ghi nếu Spring Boot đang kết nối
  if (state.backendConnected) {
    try {
      // Xóa tất cả giao dịch
      await fetch(`${BACKEND_API_URL}/transactions/clear-all?email=${encodeURIComponent(email)}`, {
        method: 'DELETE'
      });

      // Xóa hạn mức trên máy chủ bằng cách đồng bộ với danh sách trống
      await fetch(`${BACKEND_API_URL}/budgets/sync?email=${encodeURIComponent(email)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify([])
      });

      // Xóa danh mục tùy chỉnh trên máy chủ bằng cách đồng bộ với danh sách trống
      await fetch(`${BACKEND_API_URL}/categories/sync?email=${encodeURIComponent(email)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify([])
      });

      // Xóa hũ tiết kiệm trên máy chủ bằng cách đồng bộ với danh sách trống
      await fetch(`${BACKEND_API_URL}/savings-jars/sync?email=${encodeURIComponent(email)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify([])
      });

      showToast(isVi ? "Đã xóa toàn bộ dữ liệu sạch sẽ trên máy chủ!" : "Successfully cleared all server database records!");
    } catch (err) {
      console.error("Đặt lại cơ sở dữ liệu backend thất bại:", err);
      showToast(isVi ? "Đã xóa dữ liệu cục bộ, nhưng đồng bộ với máy chủ thất bại!" : "Cleared local memory, but server synchronization failed!", "error");
    }
  } else {
    showToast(isVi ? "Đã xóa sạch toàn bộ dữ liệu cục bộ!" : "Local cache cleared successfully!");
  }

  // 4. Làm mới ngay lập tức giao diện trang hiện đang hoạt động
  if (typeof renderDashboard === 'function') renderDashboard();
  if (typeof applyFilters === 'function') applyFilters();
  if (typeof populateCategoryFilter === 'function') populateCategoryFilter();
  if (typeof renderBudgetInputs === 'function') renderBudgetInputs();
  if (typeof renderCategoriesList === 'function') renderCategoriesList();
  if (typeof populateCategories === 'function') populateCategories();
  if (typeof renderSavingsJars === 'function') renderSavingsJars();
}
window.resetAllUserData = resetAllUserData;

// Khôi phục dữ liệu mẫu chuẩn và đồng bộ lên máy chủ cơ sở dữ liệu
async function restoreAllDemoData() {
  if (!state.user) return;
  const email = state.user;
  const isVi = state.language === 'vi';

  // 1. Đưa dữ liệu mẫu trực tiếp vào bộ nhớ đệm local storage
  localStorage.setItem(`chi_tieu_${email}_transactions`, JSON.stringify(INITIAL_TRANSACTIONS));
  localStorage.setItem(`chi_tieu_${email}_budgets`, JSON.stringify(INITIAL_BUDGETS));
  localStorage.setItem(`chi_tieu_${email}_expense_categories`, JSON.stringify(EXPENSE_CATEGORIES));
  localStorage.setItem(`chi_tieu_${email}_income_categories`, JSON.stringify(INCOME_CATEGORIES));
  localStorage.setItem(`chi_tieu_${email}_savings_jars`, JSON.stringify(INITIAL_SAVINGS_JARS));

  // 2. Đọc ngược từ bộ nhớ đệm vào trạng thái toàn cục
  await loadUserData();

  // 3. Đồng bộ lên PostgreSQL nếu backend Spring Boot đang kết nối
  if (state.backendConnected) {
    try {
      await saveUserData();
      showToast(isVi ? "Đã khôi phục và đồng bộ dữ liệu mẫu lên máy chủ!" : "Demo data successfully loaded and synchronized to server database!");
    } catch (err) {
      console.error("Đồng bộ dữ liệu mẫu thất bại:", err);
      showToast(isVi ? "Đã khôi phục dữ liệu mẫu cục bộ!" : "Demo data loaded in local memory!");
    }
  } else {
    showToast(isVi ? "Đã khôi phục dữ liệu mẫu cục bộ!" : "Demo data loaded in local memory!");
  }

  // 4. Bắt buộc kích hoạt cập nhật giao diện ngay lập tức
  if (typeof renderDashboard === 'function') renderDashboard();
  if (typeof applyFilters === 'function') applyFilters();
  if (typeof populateCategoryFilter === 'function') populateCategoryFilter();
  if (typeof renderBudgetInputs === 'function') renderBudgetInputs();
  if (typeof renderCategoriesList === 'function') renderCategoriesList();
  if (typeof populateCategories === 'function') populateCategories();
  if (typeof renderSavingsJars === 'function') renderSavingsJars();
}
window.restoreAllDemoData = restoreAllDemoData;

// Đồng hồ hệ thống trực tiếp
function initClock() {
  const clockEl = document.getElementById('system-clock');
  if (clockEl) {
    setInterval(() => {
      const now = new Date();
      clockEl.innerText = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }, 1000);
    const now = new Date();
    clockEl.innerText = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }
}

// Cập nhật huy hiệu trạng thái backend
function updateBackendBadge() {
  const badge = document.getElementById('backend-status-badge');
  if (badge) {
    const isVi = state.language === 'vi';
    if (state.backendConnected) {
      badge.className = "flex items-center gap-1.5 px-2 py-1 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-lg text-[9px] font-bold cursor-pointer";
      badge.innerHTML = `<span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>Spring Boot: ${isVi ? 'Đang kết nối' : 'Connected'}`;
    } else {
      badge.className = "flex items-center gap-1.5 px-2 py-1 bg-slate-500/10 border border-slate-500/20 text-slate-400 rounded-lg text-[9px] font-bold cursor-pointer hover:bg-slate-500/25 transition-all";
      badge.innerHTML = `<span class="w-1.5 h-1.5 rounded-full bg-slate-500"></span>Spring Boot: ${isVi ? 'Ngoại tuyến (Offline)' : 'Offline'}`;
      badge.title = isVi ? "Chạm để kết nối lại" : "Click to reconnect";
    }
  }
}
window.updateBackendBadge = updateBackendBadge;

async function checkBackendConnection() {
  if (!state.user) return false;
  try {
    const probe = await fetch(`${BACKEND_API_URL}/users/profile?email=${encodeURIComponent(state.user)}`);
    state.backendConnected = probe.ok;
    updateBackendBadge();
    return probe.ok;
  } catch (err) {
    state.backendConnected = false;
    updateBackendBadge();
    return false;
  }
}
window.checkBackendConnection = checkBackendConnection;

// Chèn bố cục dùng chung (Sidebar điều hướng & Header)
function injectSharedLayout(activePageId) {
  const wrapper = document.getElementById('app-root');
  if (!wrapper) return;

  const lang = state.language === 'en' ? 'en' : 'vi';
  const t = TRANSLATIONS[lang];

  // Lấy nội dung của không gian làm việc chính trước
  const mainContentHTML = wrapper.innerHTML;

  // Hiển thị cấu trúc Bố cục đầy đủ
  document.body.classList.add('md:overflow-hidden');
  wrapper.className = "min-h-screen md:h-screen flex flex-col md:flex-row w-full md:overflow-hidden";
  wrapper.innerHTML = `
    <!-- Thanh điều hướng Sidebar -->
    <aside class="w-full md:w-60 bg-[#121318] text-slate-200 flex-shrink-0 flex flex-col justify-between border-r border-[#1c1e24] font-sans md:h-full md:overflow-y-auto custom-scrollbar">
      <div>
        <!-- Tiêu đề Sidebar -->
        <div class="p-5 border-b border-[#1a1b22] flex items-center justify-between">
          <div class="flex items-center gap-2.5">
            <div class="p-1.5 bg-indigo-600/10 border border-indigo-500/20 rounded-lg text-indigo-400"><i data-lucide="piggy-bank" class="w-4 h-4"></i></div>
            <div>
              <h1 class="font-bold text-sm tracking-tight text-white leading-none">${t.appName}</h1>
              <span class="text-[9px] font-bold text-slate-500 tracking-wider uppercase mt-1 block">Beta Workspace</span>
            </div>
          </div>
        </div>

        <!-- Thông tin trạng thái Người dùng trên Sidebar -->
        <div class="p-3 mx-4 my-3 bg-[#191b22]/50 rounded-xl border border-[#22242e]/60">
          <div class="flex items-center gap-2.5">
            <div class="w-7 h-7 rounded-lg bg-[#222430] border border-[#2d3040] flex items-center justify-center font-bold text-slate-300 text-xs">
              ${state.user ? state.user.charAt(0).toUpperCase() : '?'}
            </div>
            <div class="min-w-0">
              <span class="text-[9px] text-slate-500 font-bold uppercase tracking-wider block leading-none mb-0.5">${t.welcomeUser}</span>
              <h3 class="text-xs font-bold truncate text-slate-200 leading-tight">${state.user || 'Guest'}</h3>
            </div>
          </div>
        </div>

        <!-- Huy hiệu Trạng thái Kết nối -->
        <div class="px-4 mb-3 flex justify-start">
          <div id="backend-status-badge" onclick="checkBackendConnection().then(connected => { if (connected) { showToast(state.language === 'vi' ? 'Đã kết nối Spring Boot!' : 'Spring Boot connected!'); loadUserData(); } else { showToast(state.language === 'vi' ? 'Lỗi kết nối Spring Boot (localhost:8080)' : 'Spring Boot offline (localhost:8080)', 'error'); } })"></div>
        </div>

        <!-- Các mục Menu với liên kết A thực tế -->
        <nav class="px-3 space-y-1">
          <a href="dashboard.html" class="w-full flex items-center justify-between py-2 rounded-lg transition-all font-semibold text-[11px] ${activePageId === 'dashboard' ? 'bg-[#1a1b23] text-white border-l-2 border-indigo-500 rounded-l-none pl-3' : 'text-slate-400 hover:text-slate-200 hover:bg-[#191b22]/40 pl-3.5'}">
            <span class="flex items-center gap-2.5"><i data-lucide="line-chart" class="w-3.5 h-3.5"></i> ${t.reportTab}</span>
            ${activePageId === 'dashboard' ? `<span class="px-1.5 py-0.5 text-[8px] uppercase tracking-wider font-extrabold bg-indigo-600/20 text-indigo-400 rounded-md border border-indigo-500/10">Active</span>` : ''}
          </a>
          <a href="record.html" class="w-full flex items-center py-2 rounded-lg transition-all font-semibold text-[11px] ${activePageId === 'record' ? 'bg-[#1a1b23] text-white border-l-2 border-indigo-500 rounded-l-none pl-3' : 'text-slate-400 hover:text-slate-200 hover:bg-[#191b22]/40 pl-3.5'}">
            <span class="flex items-center gap-2.5"><i data-lucide="plus-circle" class="w-3.5 h-3.5"></i> ${t.recordTab}</span>
          </a>
          <a href="transactions.html" class="w-full flex items-center py-2 rounded-lg transition-all font-semibold text-[11px] ${activePageId === 'transactions' ? 'bg-[#1a1b23] text-white border-l-2 border-indigo-500 rounded-l-none pl-3' : 'text-slate-400 hover:text-slate-200 hover:bg-[#191b22]/40 pl-3.5'}">
            <span class="flex items-center gap-2.5"><i data-lucide="clock" class="w-3.5 h-3.5"></i> ${t.transactionListTab}</span>
          </a>
          <a href="budgets.html" class="w-full flex items-center py-2 rounded-lg transition-all font-semibold text-[11px] ${activePageId === 'budgets' ? 'bg-[#1a1b23] text-white border-l-2 border-indigo-500 rounded-l-none pl-3' : 'text-slate-400 hover:text-slate-200 hover:bg-[#191b22]/40 pl-3.5'}">
            <span class="flex items-center gap-2.5"><i data-lucide="trending-up" class="w-3.5 h-3.5"></i> ${t.budgetTab}</span>
          </a>
          <a href="categories.html" class="w-full flex items-center py-2 rounded-lg transition-all font-semibold text-[11px] ${activePageId === 'categories' ? 'bg-[#1a1b23] text-white border-l-2 border-indigo-500 rounded-l-none pl-3' : 'text-slate-400 hover:text-slate-200 hover:bg-[#191b22]/40 pl-3.5'}">
            <span class="flex items-center gap-2.5"><i data-lucide="tag" class="w-3.5 h-3.5"></i> ${lang === 'vi' ? 'Danh mục' : 'Categories'}</span>
          </a>
          <a href="savings.html" class="w-full flex items-center py-2 rounded-lg transition-all font-semibold text-[11px] ${activePageId === 'savings' ? 'bg-[#1a1b23] text-white border-l-2 border-[#6366F1] rounded-l-none pl-3' : 'text-slate-400 hover:text-slate-200 hover:bg-[#191b22]/40 pl-3.5'}">
            <span class="flex items-center gap-2.5"><i data-lucide="piggy-bank" class="w-3.5 h-3.5"></i> ${t.savingsTab}</span>
          </a>
        </nav>
      </div>

      <!-- Chân trang Cài đặt và Đăng xuất -->
      <div class="p-4 border-t border-[#1a1b22] space-y-3">
        <div class="flex items-center gap-2">
          <button onclick="toggleTheme()" class="flex-grow py-2 border border-[#1e202b] hover:border-[#272a39] hover:bg-[#1a1b23] rounded-lg text-[10px] font-bold text-slate-400 hover:text-slate-200 flex items-center justify-center gap-1.5 transition-all cursor-pointer">
            <i data-theme-icon data-lucide="${state.theme === 'light' ? 'moon' : 'sun'}" class="w-3.5 h-3.5"></i>
            <span id="theme-toggle-span">${state.theme === 'light' ? t.themeDark : t.themeLight}</span>
          </button>
          <button onclick="toggleLanguage()" class="flex-grow py-2 border border-[#1e202b] hover:border-[#272a39] hover:bg-[#1a1b23] rounded-lg text-[10px] font-bold text-slate-400 hover:text-slate-200 flex items-center justify-center gap-1.5 transition-all cursor-pointer">
            <i data-lucide="languages" class="w-3.5 h-3.5"></i>
            <span>${state.language.toUpperCase()}</span>
          </button>
        </div>

        <button onclick="logout()" class="w-full py-2 bg-rose-950/10 hover:bg-rose-950/20 text-rose-400 border border-rose-950/30 rounded-lg text-[10px] font-bold uppercase tracking-wider flex items-center justify-center gap-2 transition-all cursor-pointer">
          <i data-lucide="log-out" class="w-3.5 h-3.5"></i> ${t.logoutButton}
        </button>
      </div>
    </aside>

    <!-- Vùng bao quanh Không gian nội dung chính -->
    <main class="flex-grow p-6 md:p-8 overflow-y-auto max-w-7xl mx-auto w-full custom-scrollbar bg-slate-50/40 dark:bg-zinc-950 text-slate-850 dark:text-slate-100">
      ${mainContentHTML}
    </main>
  `;

  // Gọi các hàm hỗ trợ khởi tạo
  applyTheme();
  initClock();
  updateBackendBadge();
  if (window.lucide) window.lucide.createIcons();
}
window.injectSharedLayout = injectSharedLayout;

// Thăm dò khi tải trang
if (state.user) {
  checkBackendConnection();
}

// Hàm tính toán số dư thực tế trong ví (Dán vào cuối file state.js)
function getWalletBalance() {
  return (state.transactions || []).reduce((acc, tx) => {
    const amt = Number(tx.amount) || 0;
    return tx.type === 'INCOME' ? acc + amt : acc - amt;
  }, 0);
}
window.getWalletBalance = getWalletBalance;

// Tự động chạy kiểm tra xác thực khi tải trang
checkAuth();
applyTheme();