// 12. JS controller for savings.html

// Colors available for selection
const SAVINGS_COLORS = [
  { value: '#6366F1', label: 'Indigo' },
  { value: '#3B82F6', label: 'Blue' },
  { value: '#10B981', label: 'Emerald' },
  { value: '#14B8A6', label: 'Teal' },
  { value: '#F59E0B', label: 'Amber' },
  { value: '#EF4444', label: 'Rose' },
  { value: '#EC4899', label: 'Pink' },
  { value: '#8B5CF6', label: 'Violet' }
];

// Icons available for selection
const SAVINGS_ICONS = [
  { value: 'piggy-bank', label: 'Heo đất' },
  { value: 'laptop', label: 'Thiết bị' },
  { value: 'home', label: 'Nhà cửa' },
  { value: 'car', label: 'Xe cộ' },
  { value: 'plane', label: 'Du lịch' },
  { value: 'gift', label: 'Đám cưới' },
  { value: 'graduation-cap', label: 'Học tập' },
  { value: 'heart', label: 'Yêu thích' }
];

let selectedColor = '#6366F1';
let selectedIcon = 'piggy-bank';

function initSavingsPage() {
  console.log("Initializing Savings Jars page...");
  const lang = state.language || 'vi';
  const t = TRANSLATIONS[lang] || TRANSLATIONS['vi'];

  // Translate static labels
  translateSavingsLabels(lang, t);

  // Render selectors
  renderColorSelectors();
  renderIconSelectors();

  // Render Jars list & general summary progress
  renderSavingsJars();

  // Attach forms event listeners
  const form = document.getElementById('savings-form');
  if (form) {
    form.addEventListener('submit', handleSavingsFormSubmit);
  }

  const modalForm = document.getElementById('modal-action-form');
  if (modalForm) {
    modalForm.addEventListener('submit', handleModalFormSubmit);
  }
}

function translateSavingsLabels(lang, t) {
  const isVi = lang === 'vi';

  const titleEl = document.getElementById('savings-header-title');
  if (titleEl) titleEl.innerText = isVi ? 'Hũ tích lũy tài chính' : 'Savings & Accumulation Jars';

  const subEl = document.getElementById('savings-header-sub');
  if (subEl) subEl.innerText = isVi ? 'Chia sẻ nguồn tiền nhàn rỗi nuôi heo đất hoàn thành các ước mơ lớn' : 'Distribute extra funds to target goals and fulfill big dreams';

  // Stats labels
  const curLbl = document.getElementById('global-current-lbl');
  if (curLbl) curLbl.innerText = isVi ? 'Tổng số tiền hiện có trong các hũ' : 'Total money currently accumulated';

  const tarLbl = document.getElementById('global-target-lbl');
  if (tarLbl) tarLbl.innerText = isVi ? 'Tổng tiền mục tiêu cần đạt' : 'Total budget target required';

  const progressLbl = document.getElementById('global-progress-lbl');
  if (progressLbl) progressLbl.innerText = isVi ? 'Tỷ lệ hoàn thành kế hoạch chung' : 'Overall accumulation progress rate';

  // Form labels
  const formTitle = document.getElementById('form-mode-title');
  if (formTitle) formTitle.innerText = isVi ? 'Tạo Hũ Mới' : 'Create New Jar';

  const lblName = document.getElementById('lbl-jar-name');
  if (lblName) lblName.innerText = isVi ? 'Tên hũ tiết kiệm' : 'Savings Jar Name';

  const sNameInput = document.getElementById('savings-name');
  if (sNameInput) sNameInput.placeholder = isVi ? 'Ví dụ: Mua Macbook, Đi du lịch Nhật...' : 'Example: Buy Macbook, Trip to Tokyo...';

  const lblTarget = document.getElementById('lbl-jar-target');
  if (lblTarget) lblTarget.innerText = isVi ? 'Số tiền mục tiêu (VND)' : 'Target Goal (VND)';

  const sTargetInput = document.getElementById('savings-target');
  if (sTargetInput) sTargetInput.placeholder = isVi ? 'Ví dụ: 15000000' : 'Example: 15000000';

  const lblCurrent = document.getElementById('lbl-jar-current');
  if (lblCurrent) lblCurrent.innerText = isVi ? 'Số tiền đã tích lũy ban đầu (VND)' : 'Initial Saved Amount (VND)';

  const sCurrentInput = document.getElementById('savings-current');
  if (sCurrentInput) sCurrentInput.placeholder = isVi ? 'Mặc định: 0đ' : 'Default: 0';

  const lblColor = document.getElementById('lbl-jar-color');
  if (lblColor) lblColor.innerText = isVi ? 'Màu sắc hiển thị' : 'Theme Color';

  const lblIcon = document.getElementById('lbl-jar-icon');
  if (lblIcon) lblIcon.innerText = isVi ? 'Biểu tượng hũ' : 'Pick a Mascot Icon';

  const submitBtn = document.getElementById('save-jar-btn');
  if (submitBtn) submitBtn.innerText = isVi ? 'Bắt đầu nuôi hũ' : 'Start Growing Jar';

  // Section title
  const secTitle = document.getElementById('active-jars-section-title');
  if (secTitle) secTitle.innerText = isVi ? 'Danh Sách Hũ Tiết Kiệm Đang Nuôi' : 'Active Financial Jars';

  // Modal translation
  const modalCancel = document.getElementById('modal-cancel-btn');
  if (modalCancel) modalCancel.innerText = isVi ? 'Hủy bỏ' : 'Cancel';
}

function renderColorSelectors() {
  const container = document.getElementById('color-selectors-container');
  if (!container) return;
  container.innerHTML = '';

  SAVINGS_COLORS.forEach(color => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `w-7 h-7 rounded-full border-2 transition-all cursor-pointer transform hover:scale-115 ${selectedColor === color.value ? 'border-indigo-600 ring-2 ring-indigo-500/30' : 'border-transparent'}`;
    btn.style.backgroundColor = color.value;
    btn.title = color.label;
    btn.addEventListener('click', () => {
      selectedColor = color.value;
      document.getElementById('savings-color').value = color.value;
      renderColorSelectors();
    });
    container.appendChild(btn);
  });
}

function renderIconSelectors() {
  const container = document.getElementById('icon-selectors-container');
  if (!container) return;
  container.innerHTML = '';

  SAVINGS_ICONS.forEach(icon => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `p-2 rounded-xl border flex items-center justify-center transition-all cursor-pointer transform hover:scale-105 ${selectedIcon === icon.value ? 'bg-indigo-600 border-indigo-600 text-white' : 'bg-slate-50 dark:bg-slate-800/40 border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400'}`;
    btn.innerHTML = `<i data-lucide="${icon.value}" class="w-4 h-4"></i>`;
    btn.title = icon.label;
    btn.addEventListener('click', () => {
      selectedIcon = icon.value;
      document.getElementById('savings-icon').value = icon.value;
      renderIconSelectors();
    });
    container.appendChild(btn);
  });
  if (window.lucide) window.lucide.createIcons();
}

function renderSavingsJars() {
  const container = document.getElementById('jars-list-container');
  if (!container) return;
  container.innerHTML = '';

  const jars = state.savingsJars || [];
  const lang = state.language || 'vi';
  const isVi = lang === 'vi';

  // Update badge count
  const badge = document.getElementById('jars-count-badge');
  if (badge) {
    badge.innerText = isVi ? `${jars.length} hũ hoạt động` : `${jars.length} active goals`;
  }

  // Calculate overall stats
  let totalCurrent = 0;
  let totalTarget = 0;

  if (jars.length === 0) {
    container.innerHTML = `
      <div class="col-span-1 md:col-span-2 bg-white dark:bg-slate-900 border border-slate-200/50 dark:border-slate-800/80 rounded-2xl p-8 text-center flex flex-col items-center justify-center">
        <div class="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400 dark:text-slate-500 mb-3">
          <i data-lucide="piggy-bank" class="w-6 h-6 animate-bounce"></i>
        </div>
        <h4 class="text-xs font-bold text-slate-700 dark:text-slate-300">${isVi ? 'Chưa có hũ tiết kiệm nào' : 'No active savings jars found'}</h4>
        <p class="text-[10px] text-slate-400 mt-1">${isVi ? 'Hãy điền vào khung bên phải để tạo ngay mục tiêu tiết kiệm đầu tiên!' : 'Fill in the bento form to start tracking your first financial target!'}</p>
      </div>
    `;
  } else {
    jars.forEach(jar => {
      totalCurrent += jar.currentAmount || 0;
      totalTarget += jar.targetAmount || 0;

      const percentage = Math.min(100, Math.round(((jar.currentAmount || 0) / (jar.targetAmount || 1)) * 100));
      const achieved = percentage >= 100;

      const card = document.createElement('div');
      card.className = `bg-white dark:bg-slate-900 border ${achieved ? 'border-emerald-500/25 bg-emerald-500/[0.01]' : 'border-slate-200/50 dark:border-slate-800/80'} rounded-2xl p-5 shadow-sm hover:shadow-md transition-all relative flex flex-col justify-between`;

      card.innerHTML = `
        <div>
          <!-- Card Header details -->
          <div class="flex items-start justify-between gap-3 mb-3">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center text-white" style="background-color: ${jar.color || '#3b82f6'}">
                <i data-lucide="${jar.icon || 'piggy-bank'}" class="w-5 h-5"></i>
              </div>
              <div class="min-w-0">
                <h4 class="text-xs font-extrabold text-slate-800 dark:text-white truncate" title="${jar.name}">${jar.name}</h4>
                <span class="text-[9px] font-bold ${achieved ? 'text-emerald-500' : 'text-slate-400'}">
                  ${achieved ? (isVi ? '🎉 Đã hoàn thành!' : '🎉 Target Reached!') : (isVi ? 'Đang nuôi hũ' : 'Growing progress')}
                </span>
              </div>
            </div>

            <!-- Quick config menu actions -->
            <div class="flex items-center gap-1.5">
              <button onclick="editSavingsJar('${jar.id}')" class="p-1 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-400 hover:text-indigo-500 transition-all cursor-pointer" title="${isVi ? 'Sửa mục tiêu' : 'Edit target'}">
                <i data-lucide="edit-3" class="w-3.5 h-3.5"></i>
              </button>
              <button onclick="deleteSavingsJar('${jar.id}')" class="p-1 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-400 hover:text-rose-500 transition-all cursor-pointer" title="${isVi ? 'Xóa hũ này' : 'Delete Jar'}">
                <i data-lucide="trash-2" class="w-3.5 h-3.5"></i>
              </button>
            </div>
          </div>

          <!-- Cash tracking amounts -->
          <div class="grid grid-cols-2 gap-2 mt-4 bg-slate-50 dark:bg-slate-900/40 p-2.5 rounded-xl border border-slate-100 dark:border-slate-800/30">
            <div>
              <span class="text-[8px] text-slate-400 block font-bold uppercase tracking-wider">${isVi ? 'Hiện có' : 'Saved'}</span>
              <span class="text-xs font-black text-emerald-600 dark:text-emerald-400 font-mono">${formatVND(jar.currentAmount || 0)}</span>
            </div>
            <div>
              <span class="text-[8px] text-slate-400 block font-bold uppercase tracking-wider">${isVi ? 'Mục tiêu' : 'Target'}</span>
              <span class="text-xs font-black text-slate-700 dark:text-slate-300 font-mono">${formatVND(jar.targetAmount || 0)}</span>
            </div>
          </div>

          <!-- Individual Progress line -->
          <div class="mt-4">
            <div class="flex items-center justify-between mb-1">
              <span class="text-[9px] font-bold text-slate-400">${isVi ? 'Tiến trình' : 'Progress'}</span>
              <span class="text-[10px] font-black text-indigo-500 font-mono">${percentage}%</span>
            </div>
            <div class="w-full bg-slate-100 dark:bg-slate-800 h-2 rounded-full overflow-hidden">
              <div class="h-2 rounded-full transition-all duration-300" style="background-color: ${jar.color || '#3b82f6'}; width: ${percentage}%"></div>
            </div>
          </div>
        </div>

        <!-- Add & Subtract instant controls -->
        <div class="flex items-center gap-2 mt-5 pt-3 border-t border-slate-100 dark:border-slate-800/40">
          <button onclick="openActionModal('${jar.id}', 'deposit')" class="w-1/2 py-1.5 bg-indigo-500/10 hover:bg-indigo-500/25 border border-indigo-500/20 text-indigo-500 dark:text-indigo-400 rounded-lg text-[9px] font-bold uppercase tracking-widest flex items-center justify-center gap-1 transition-all cursor-pointer">
            <i data-lucide="arrow-down-left" class="w-3 h-3"></i> <span>${isVi ? 'Gửi thêm' : 'Add Cash'}</span>
          </button>
          <button onclick="openActionModal('${jar.id}', 'withdraw')" class="w-1/2 py-1.5 bg-rose-500/10 hover:bg-rose-500/25 border border-rose-500/20 text-rose-500 dark:text-rose-400 rounded-lg text-[9px] font-bold uppercase tracking-widest flex items-center justify-center gap-1 transition-all cursor-pointer">
            <i data-lucide="arrow-up-right" class="w-3 h-3"></i> <span>${isVi ? 'Rút tiền' : 'Withdraw'}</span>
          </button>
        </div>
      `;

      container.appendChild(card);
    });
  }

  // Render general summary
  const globalCurEl = document.getElementById('global-current-amt');
  if (globalCurEl) globalCurEl.innerText = formatVND(totalCurrent);

  const globalTarEl = document.getElementById('global-target-amt');
  if (globalTarEl) globalTarEl.innerText = formatVND(totalTarget);

  const globalPercent = totalTarget > 0 ? Math.min(100, Math.round((totalCurrent / totalTarget) * 100)) : 0;

  const percentEl = document.getElementById('global-percentage');
  if (percentEl) percentEl.innerText = `${globalPercent}%`;

  const barEl = document.getElementById('global-progress-bar');
  if (barEl) barEl.style.width = `${globalPercent}%`;

  if (window.lucide) window.lucide.createIcons();
}

function handleSavingsFormSubmit(e) {
  e.preventDefault();

  const idInput = document.getElementById('savings-id').value;
  const nameInput = document.getElementById('savings-name').value.trim();
  const targetInput = Number(document.getElementById('savings-target').value) || 0;
  const currentInput = Number(document.getElementById('savings-current').value) || 0;

  const lang = state.language || 'vi';
  const isVi = lang === 'vi';

  if (!nameInput || targetInput <= 0) {
    showToast(isVi ? 'Hãy nhập đầy đủ thông tin hợp lệ!' : 'Please enter valid target jar details!', 'error');
    return;
  }

  // Create or Update
  if (idInput) {
    // Edit existing jar
    const existingIndex = state.savingsJars.findIndex(j => String(j.id) === String(idInput));
    if (existingIndex !== -1) {
      state.savingsJars[existingIndex].name = nameInput;
      state.savingsJars[existingIndex].targetAmount = targetInput;
      state.savingsJars[existingIndex].color = selectedColor;
      state.savingsJars[existingIndex].icon = selectedIcon;
      showToast(isVi ? `Đã cập nhật hũ "${nameInput}"!` : `Jar "${nameInput}" updated successfully!`);
    }
  } else {
    // Verify name duplicates
    if (state.savingsJars.some(j => j.name.toLowerCase() === nameInput.toLowerCase())) {
      showToast(isVi ? 'Tên hũ tiết kiệm này đã tồn tại!' : 'Savings goal name already exists!', 'error');
      return;
    }

    // Add new jar
    const newId = 'sj-' + Date.now();
    const newJar = {
      id: newId,
      name: nameInput,
      targetAmount: targetInput,
      currentAmount: currentInput,
      color: selectedColor,
      icon: selectedIcon
    };
    state.savingsJars.push(newJar);
    showToast(isVi ? `Đã bắt đầu nuôi hũ "${nameInput}"! 🎉` : `New Jar "${nameInput}" started successfully! 🎉`);
  }

  // Save changes locally and to server
  saveUserData();

  // Reset form status
  resetSavingsForm();

  // Re-render
  renderSavingsJars();
}

function resetSavingsForm() {
  document.getElementById('savings-id').value = '';
  document.getElementById('savings-name').value = '';
  document.getElementById('savings-target').value = '';

  const currentInput = document.getElementById('savings-current');
  if (currentInput) currentInput.value = '0';

  const initialAmtContainer = document.getElementById('initial-amount-container');
  if (initialAmtContainer) initialAmtContainer.classList.remove('hidden');

  // Cancel button
  const cancelBtn = document.getElementById('cancel-edit-btn');
  if (cancelBtn) cancelBtn.classList.add('hidden');

  // Headers
  const isVi = state.language === 'vi';
  const formTitle = document.getElementById('form-mode-title');
  if (formTitle) formTitle.innerText = isVi ? 'Tạo Hũ Mới' : 'Create New Jar';

  const submitBtn = document.getElementById('save-jar-btn');
  if (submitBtn) submitBtn.innerText = isVi ? 'Bắt đầu nuôi hũ' : 'Start Growing Jar';

  // Default color & icon resets
  selectedColor = '#6366F1';
  document.getElementById('savings-color').value = '#6366F1';
  selectedIcon = 'piggy-bank';
  document.getElementById('savings-icon').value = 'piggy-bank';

  renderColorSelectors();
  renderIconSelectors();
}
window.resetSavingsForm = resetSavingsForm;

function editSavingsJar(id) {
  const jar = state.savingsJars.find(j => String(j.id) === String(id));
  if (!jar) return;

  const isVi = state.language === 'vi';

  // Fill input fields
  document.getElementById('savings-id').value = jar.id;
  document.getElementById('savings-name').value = jar.name;
  document.getElementById('savings-target').value = jar.targetAmount;

  // Hide current amount since it is edit mode (current amount is managed via quick transaction actions)
  const initialAmtContainer = document.getElementById('initial-amount-container');
  if (initialAmtContainer) initialAmtContainer.classList.add('hidden');

  // Show cancel edit button
  const cancelBtn = document.getElementById('cancel-edit-btn');
  if (cancelBtn) cancelBtn.classList.remove('hidden');

  // Toggle Header wording
  const formTitle = document.getElementById('form-mode-title');
  if (formTitle) formTitle.innerText = isVi ? 'Chỉnh Sửa Hũ' : 'Edit Savings Jar';

  const submitBtn = document.getElementById('save-jar-btn');
  if (submitBtn) submitBtn.innerText = isVi ? 'Lưu thay đổi' : 'Apply Changes';

  // Apply colors & icon selections
  selectedColor = jar.color || '#3b82f6';
  document.getElementById('savings-color').value = selectedColor;

  selectedIcon = jar.icon || 'piggy-bank';
  document.getElementById('savings-icon').value = selectedIcon;

  renderColorSelectors();
  renderIconSelectors();

  // Scroll smooth to sidebar bento form
  const formCard = document.getElementById('savings-form');
  if (formCard) {
    formCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
}
window.editSavingsJar = editSavingsJar;

async function deleteSavingsJar(id) {
  const jar = state.savingsJars.find(j => String(j.id) === String(id));
  if (!jar) return;

  const isVi = state.language === 'vi';
  const confirmMsg = isVi
    ? `Bạn có chắc chắn muốn xóa hũ tiết kiệm "${jar.name}"?\\n\\nHành động này không thể hoàn tác!`
    : `Are you sure you want to delete the savings goal "${jar.name}"?\\n\\nThis cannot be undone!`;

  if (!confirm(confirmMsg)) return;

  // If online, perform single DELETE API call defensively, or synchronize entire batch
  if (state.backendConnected && typeof jar.id === 'number') {
    try {
      await fetch(`${BACKEND_API_URL}/savings-jars/${jar.id}?email=${encodeURIComponent(state.user)}`, {
        method: 'DELETE'
      });
    } catch (e) {
      console.warn("Backend single delete call failed. Synchronizing full batch instead.");
    }
  }

  // Remove from state array
  state.savingsJars = state.savingsJars.filter(j => String(j.id) !== String(id));

  // Save locally & trigger background sync list
  saveUserData();

  showToast(isVi ? 'Đã xóa hũ tiết kiệm thành công!' : 'Savings jar deleted successfully!');
  renderSavingsJars();
}
window.deleteSavingsJar = deleteSavingsJar;

// QUICK TRANSACTIONS DEPOSIT/WITHDRAW MODAL
function openActionModal(id, actionType) {
  const jar = state.savingsJars.find(j => String(j.id) === String(id));
  if (!jar) return;

  const isVi = state.language === 'vi';

  document.getElementById('modal-jar-id').value = id;
  document.getElementById('modal-action-type').value = actionType;
  document.getElementById('modal-amount-input').value = '';
  document.getElementById('modal-limit-warning').classList.add('hidden');

  // Fill dynamic texts
  const titleEl = document.getElementById('action-modal-title');
  const descEl = document.getElementById('action-modal-desc');
  const submitBtn = document.getElementById('modal-submit-btn');
  const modalIcon = document.getElementById('action-modal-icon');
  const modalLblAmt = document.getElementById('modal-lbl-amt');

  if (actionType === 'deposit') {
    titleEl.innerText = isVi ? `Nạp Tiền: ${jar.name}` : `Deposit Money: ${jar.name}`;
    descEl.innerText = isVi
      ? `Gửi thêm tiền nhàn rỗi tích lũy của bạn vào hũ. (Hiện có: ${formatVND(jar.currentAmount)})`
      : `Add extra funds to grow this savings goal. (Current: ${formatVND(jar.currentAmount)})`;
    submitBtn.innerText = isVi ? 'Gửi tiết kiệm' : 'Deposit Cash';
    submitBtn.className = 'w-1/2 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-[10px] font-bold transition-all uppercase tracking-wider shadow-sm';
    if (modalIcon) modalIcon.setAttribute('data-lucide', 'arrow-down-left');
    if (modalLblAmt) modalLblAmt.innerText = isVi ? 'Số tiền gửi tiết kiệm (VND)' : 'Amount to Save (VND)';
  } else {
    titleEl.innerText = isVi ? `Rút Tiền: ${jar.name}` : `Withdraw Money: ${jar.name}`;
    descEl.innerText = isVi
      ? `Rút bớt tiền từ hũ tiết kiệm để chi tiêu mục tiêu khác. (Hiện có: ${formatVND(jar.currentAmount)})`
      : `Withdraw money from this jar to cover target expenses. (Current: ${formatVND(jar.currentAmount)})`;
    submitBtn.innerText = isVi ? 'Xác nhận rút' : 'Withdraw Cash';
    submitBtn.className = 'w-1/2 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-[10px] font-bold transition-all uppercase tracking-wider shadow-sm';
    if (modalIcon) modalIcon.setAttribute('data-lucide', 'arrow-up-right');
    if (modalLblAmt) modalLblAmt.innerText = isVi ? 'Số tiền rút ra (VND)' : 'Amount to Withdraw (VND)';
  }

  // Display modal smoothly
  const modal = document.getElementById('jar-action-modal');
  const card = document.getElementById('jar-modal-card');
  if (modal && card) {
    modal.classList.remove('hidden');
    setTimeout(() => {
      card.classList.remove('scale-95', 'opacity-0');
    }, 10);
  }
  if (window.lucide) window.lucide.createIcons();
}
window.openActionModal = openActionModal;

function closeActionModal() {
  const modal = document.getElementById('jar-action-modal');
  const card = document.getElementById('jar-modal-card');
  if (modal && card) {
    card.classList.add('scale-95', 'opacity-0');
    setTimeout(() => {
      modal.classList.add('hidden');
    }, 200);
  }
}
window.closeActionModal = closeActionModal;

function handleModalFormSubmit(e) {
  e.preventDefault();

  const id = document.getElementById('modal-jar-id').value;
  const actionType = document.getElementById('modal-action-type').value;
  const amount = Number(document.getElementById('modal-amount-input').value) || 0;

  const isVi = state.language === 'vi';

  if (amount <= 0) {
    showToast(isVi ? 'Số tiền giao dịch không hợp lệ!' : 'Invalid cash transaction amount!', 'error');
    return;
  }

  const jarIndex = state.savingsJars.findIndex(j => String(j.id) === String(id));
  if (jarIndex === -1) return;

  const jar = state.savingsJars[jarIndex];

  if (actionType === 'deposit') {
    state.savingsJars[jarIndex].currentAmount = (jar.currentAmount || 0) + amount;
    showToast(isVi ? `Đã gửi thêm +${formatVND(amount)} vào hũ "${jar.name}"! 🐖` : `Successfully deposited +${formatVND(amount)} into "${jar.name}"! 🐖`);
  } else {
    // Validate limits
    if (amount > (jar.currentAmount || 0)) {
      document.getElementById('modal-limit-warning').classList.remove('hidden');
      return;
    }
    state.savingsJars[jarIndex].currentAmount = (jar.currentAmount || 0) - amount;
    showToast(isVi ? `Đã rút bớt -${formatVND(amount)} từ hũ "${jar.name}"!` : `Successfully withdrew -${formatVND(amount)} from "${jar.name}"!`);
  }

  // Save changes locally and to backend Spring Boot
  saveUserData();

  // Reload views and close modal
  renderSavingsJars();
  closeActionModal();
}

// Global script load handler
if (document.readyState !== 'loading') {
  console.log("DOM already ready in savings.js");
  injectSharedLayout('savings');
  initSavingsPage();
} else {
  console.log("Waiting for DOMContentLoaded in savings.js");
  document.addEventListener('DOMContentLoaded', () => {
    injectSharedLayout('savings');
    initSavingsPage();
  });
}
