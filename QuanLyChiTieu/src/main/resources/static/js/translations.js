// 1. Multi-language dictionary
const TRANSLATIONS = {
  vi: {
    loginTitle: 'Chào mừng quay trở lại',
    loginSubtitle: 'Quản lý tài chính cá nhân hiệu quả và thông minh hơn',
    emailLabel: 'Địa chỉ Email',
    passwordLabel: 'Mật khẩu',
    forgotPasswordLink: 'Quên mật khẩu?',
    loginButton: 'Đăng nhập',
    orContinueWith: 'Hoặc tiếp tục bằng',
    noAccount: 'Chưa có tài khoản?',
    registerNow: 'Đăng ký ngay',
    registerTitle: 'Tạo tài khoản mới',
    registerSubtitle: 'Bắt đầu quản lý dòng tiền của bạn ngay hôm nay',
    fullNameLabel: 'Họ và tên',
    agreeTerms: 'Tôi đồng ý với các điều khoản dịch vụ & chính sách bảo mật.',
    hasAccount: 'Đã có tài khoản?',
    loginNow: 'Đăng nhập ngay',
    forgotTitle: 'Khôi phục mật khẩu',
    forgotSubtitle: 'Nhập email của bạn để nhận liên kết đặt lại mật khẩu',
    sendResetLink: 'Gửi yêu cầu khôi phục',
    backToLogin: 'Quay lại đăng nhập',
    phoneTitle: 'Đăng nhập bằng số điện thoại',
    phoneSubtitle: 'Hệ thống sẽ gửi mã OTP xác thực qua SMS',
    phoneNumberLabel: 'Số điện thoại',
    sendOtpButton: 'Gửi mã OTP',
    otpLabel: 'Mã xác thực OTP (6 chữ số)',
    verifyOtpButton: 'Xác thực & Đăng nhập',
    resendOtp: 'Gửi lại mã OTP',
    googleLogin: 'Google',
    phoneLoginOption: 'Số điện thoại',
    emailLoginOption: 'Email',
    demoOtpSent: 'Mã OTP mô phỏng là 123456. Hãy nhập mã này để tiếp tục.',
    demoOtpSuccess: 'Xác thực thành công! Đang chuyển hướng...',
    loginSuccess: 'Đăng nhập thành công!',
    registerSuccess: 'Đăng ký thành công! Hãy đăng nhập.',
    resetSuccess: 'Liên kết khôi phục đã được mô phỏng gửi đến email của bạn!',
    themeLight: 'Sáng',
    themeDark: 'Tối',
    languageName: 'Tiếng Việt',
    appName: 'Sổ Thu Chi Cá Nhân',
    appSubVersion: 'Phiên bản Web 2026',
    walletBalance: "Số dư hiện tại",
    walletSub: 'Từ ngày tải dữ liệu hệ thống',
    mainMenu: 'Menu chính',
    reportTab: 'Báo cáo dòng tiền',
    recordTab: 'Ghi chép giao dịch',
    transactionListTab: 'Sổ chi tiết giao dịch',
    budgetTab: 'Hạn mức ngân sách',
    categoriesTab: 'Danh mục',
    savingsTab: 'Hũ tiết kiệm',
    activeTabIndicator: 'Live',
    addRecordButton: 'Ghi chép mới',
    systemTimeLabel: 'Hệ thống',
    storageLabel: 'Dữ liệu lưu trữ',
    storageValue: 'Trình duyệt (Local)',
    logoutButton: 'Đăng xuất',
    welcomeUser: 'Xin chào',
    quickAddTitle: 'Ghi chép nhanh',
    cancel: 'Hủy bỏ',
    save: 'Lưu',
    addRecordHeader: 'Ghi chép giao dịch mới 💰',
    editRecordHeader: 'Chỉnh sửa giao dịch ✏️',
    expenseLabel: 'Khoản Chi (Chi phí)',
    incomeLabel: 'Khoản Thu (Thu nhập)',
    amountLabel: 'Số tiền (VND)',
    titleLabel: 'Tiêu đề / Tên giao dịch',
    categoryLabel: 'Danh mục',
    dateLabel: 'Ngày thực hiện',
    notesLabel: 'Ghi chú (Tùy chọn)',
    notesPlaceholder: 'Nhập ghi chú thêm cho giao dịch...',
    titlePlaceholder: 'Mua sắm thực phẩm, trà sữa, lương...',
    amountPlaceholder: 'Ví dụ: 250000',
    totalIncome: 'Tổng Thu nhập',
    totalExpense: 'Tổng Chi tiêu',
    netBalance: 'Số dư thuần',
    alertOverhead: 'Cảnh báo quá hạn mức ngân sách!',
    budgetReached: 'Đã đạt hạn mức chi tiêu',
    noTransactionsYet: 'Chưa có giao dịch nào được ghi nhận.',
    recentTransactions: 'Giao dịch gần đây',
    allTime: 'Tất cả thời gian',
    currentMonth: 'Tháng này',
    filters: 'Bộ lọc',
    resetDefault: 'Khôi phục mẫu dữ liệu',
    importCsv: 'Nhập dữ liệu JSON',
    exportCsv: 'Xuất dữ liệu JSON',
    disclaimer: 'Tuyên bố miễn trừ trách nhiệm',
    privacyPolicy: 'Chính sách bảo mật Local',
    copyright: '© 2026 Sổ Thu Chi. Thiết kế tối ưu dữ liệu trình duyệt.'
  },
  en: {
    loginTitle: 'Welcome Back',
    loginSubtitle: 'Manage your finances smarter and more efficiently',
    emailLabel: 'Email Address',
    passwordLabel: 'Password',
    forgotPasswordLink: 'Forgot password?',
    loginButton: 'Sign In',
    orContinueWith: 'Or continue with',
    noAccount: 'Do not have an account?',
    registerNow: 'Register now',
    registerTitle: 'Create New Account',
    registerSubtitle: 'Start tracking your money today',
    fullNameLabel: 'Full Name',
    agreeTerms: 'I agree with the terms of service & privacy policy.',
    hasAccount: 'Already have an account?',
    loginNow: 'Sign in now',
    forgotTitle: 'Recover Password',
    forgotSubtitle: 'Enter your email to receive a password reset link',
    sendResetLink: 'Send Reset Link',
    backToLogin: 'Back to Sign In',
    phoneTitle: 'Sign In with Phone Number',
    phoneSubtitle: 'An SMS verification setup code will be simulated',
    phoneNumberLabel: 'Phone Number',
    sendOtpButton: 'Send OTP Code',
    otpLabel: 'OTP Verification Code (6 digits)',
    verifyOtpButton: 'Verify & Sign In',
    resendOtp: 'Resend OTP Code',
    googleLogin: 'Google',
    phoneLoginOption: 'Phone',
    emailLoginOption: 'Email',
    demoOtpSent: 'Your simulated OTP code is 123456. Enter it to proceed.',
    demoOtpSuccess: 'Verified successfully! Redirecting...',
    loginSuccess: 'Signed in successfully!',
    registerSuccess: 'Registered successfully! Please sign in.',
    resetSuccess: 'A recovery link has been simulated & dispatched to your email!',
    themeLight: 'Light',
    themeDark: 'Dark',
    languageName: 'English',
    appName: 'Personal Finance Book',
    appSubVersion: 'Web Version 2026',
    walletBalance: 'Accumulated Wallet Balance',
    walletSub: 'From the tracking date in browser storage',
    mainMenu: 'Main Menu',
    reportTab: 'Cashflow Dashboard',
    recordTab: 'Record Keeping',
    transactionListTab: 'Detailed Transaction Ledger',
    budgetTab: 'Budget Limits',
    categoriesTab: 'Categories',
    savingsTab: 'Savings Jars',
    activeTabIndicator: 'Live',
    addRecordButton: 'New Record',
    systemTimeLabel: 'System',
    storageLabel: 'Infrastructure',
    storageValue: 'Browser Memory (Local)',
    logoutButton: 'Log Out',
    welcomeUser: 'Welcome',
    quickAddTitle: 'Quick Record',
    cancel: 'Cancel',
    save: 'Save',
    addRecordHeader: 'Add New Record 💰',
    editRecordHeader: 'Edit Record ✏️',
    expenseLabel: 'Expense Outlay',
    incomeLabel: 'Income Source',
    amountLabel: 'Amount (VND)',
    titleLabel: 'Title / Description',
    categoryLabel: 'Category',
    dateLabel: 'Date Conducted',
    notesLabel: 'Notes (Optional)',
    notesPlaceholder: 'Enter additional details...',
    titlePlaceholder: 'Groceries, salary, coffee...',
    amountPlaceholder: 'Example: 250000',
    totalIncome: 'Total Income',
    totalExpense: 'Total Expenses',
    netBalance: 'Net Balance',
    alertOverhead: 'Budget limits exceeded!',
    budgetReached: 'Budget limit reached',
    noTransactionsYet: 'No transactions recorded yet.',
    recentTransactions: 'Recent Transactions',
    allTime: 'All time',
    currentMonth: 'This Month',
    filters: 'Filters',
    resetDefault: 'Reset to Sample Data',
    importCsv: 'Import JSON Data',
    exportCsv: 'Export JSON Data',
    disclaimer: 'Disclaimer Agreement',
    privacyPolicy: 'Local Storage Privacy',
    copyright: '© 2026 Personal Finance Book. Offline-first client security.'
  }
};

// 2. Category translations dictionary
const CATEGORY_TRANSLATIONS = {
  vi: {
    'Ăn uống': 'Ăn uống',
    'Di chuyển': 'Di chuyển',
    'Mua sắm': 'Mua sắm',
    'Nhà cửa': 'Nhà cửa',
    'Nhà cửa & Dịch vụ': 'Nhà cửa & Dịch vụ',
    'Giải trí': 'Giải trí',
    'Sức khỏe': 'Sức khỏe',
    'Giáo dục': 'Giáo dục',
    'Quà tặng': 'Quà tặng',
    'Lương': 'Lương',
    'Lương hằng tháng': 'Lương hằng tháng',
    'Làm thêm (Freelance)': 'Làm thêm (Freelance)',
    'Kinh doanh': 'Kinh doanh',
    'Đầu tư': 'Đầu tư',
    'Quà tặng, Thưởng': 'Quà tặng, Thưởng',
    'Khác': 'Khác',
    'Khác (Chi tiêu)': 'Khác (Chi tiêu)',
    'Khác (Thu nhập)': 'Khác (Thu nhập)',

    'Food & Dining': 'Ăn uống',
    'Transport': 'Di chuyển',
    'Shopping': 'Mua sắm',
    'Housing': 'Nhà cửa',
    'Housing & Utilities': 'Nhà cửa & Dịch vụ',
    'Entertainment': 'Giải trí',
    'Medical & Health': 'Sức khỏe',
    'Health & Medical': 'Sức khỏe',
    'Education': 'Giáo dục',
    'Gifts & Donations': 'Quà tặng',
    'Salary': 'Lương',
    'Monthly Salary': 'Lương hằng tháng',
    'Freelance / Side Hustle': 'Làm thêm (Freelance)',
    'Business': 'Kinh doanh',
    'Investment': 'Đầu tư',
    'Gifts & Bonuses': 'Quà tặng, Thưởng',
    'Others': 'Khác',
    'Others (Expense)': 'Khác (Chi tiêu)',
    'Others (Income)': 'Khác (Thu nhập)'
  },
  en: {
    'Ăn uống': 'Food & Dining',
    'Di chuyển': 'Transport',
    'Mua sắm': 'Shopping',
    'Nhà cửa': 'Housing',
    'Nhà cửa & Dịch vụ': 'Housing & Utilities',
    'Giải trí': 'Entertainment',
    'Sức khỏe': 'Health & Medical',
    'Giáo dục': 'Education',
    'Quà tặng': 'Gifts & Donations',
    'Lương': 'Salary',
    'Lương hằng tháng': 'Monthly Salary',
    'Làm thêm (Freelance)': 'Freelance / Side Hustle',
    'Kinh doanh': 'Business',
    'Đầu tư': 'Investment',
    'Quà tặng, Thưởng': 'Gifts & Bonuses',
    'Khác': 'Others',
    'Khác (Chi tiêu)': 'Others (Expense)',
    'Khác (Thu nhập)': 'Others (Income)',

    'Food & Dining': 'Food & Dining',
    'Transport': 'Transport',
    'Shopping': 'Shopping',
    'Housing': 'Housing',
    'Housing & Utilities': 'Housing & Utilities',
    'Entertainment': 'Entertainment',
    'Medical & Health': 'Health & Medical',
    'Health & Medical': 'Health & Medical',
    'Education': 'Education',
    'Gifts & Donations': 'Gifts & Donations',
    'Salary': 'Salary',
    'Monthly Salary': 'Monthly Salary',
    'Freelance / Side Hustle': 'Freelance / Side Hustle',
    'Business': 'Business',
    'Investment': 'Investment',
    'Gifts & Bonuses': 'Gifts & Bonuses',
    'Others': 'Others',
    'Others (Expense)': 'Others (Expense)',
    'Others (Income)': 'Others (Income)'
  }
};

const USER_TEXT_TRANSLATIONS = {
  // Common preset / user-entered titles & notes
  'Nhận lương tháng 6': 'June Salary Payout',
  'Lương công ty chính thức': 'Official Corporate Salary',
  'Trả tiền thuê nhà & điện nước': 'House Rent & Utilities Payment',
  'Tiền nhà tháng 6': 'Rent for June',
  'Dự án website thiết kế': 'Website Design Project',
  'Thiết kế UI/UX Landing Page': 'UI/UX Landing Page Design',
  'Mua thực phẩm tuần mới': 'Weekly Grocery Shopping',
  'Đi siêu thị Winmart': 'Winmart Supermarket Shopping',
  'Đăng ký tập Gym 3 tháng': '3-Month Gym Membership',
  'Phòng gym Fit24': 'Fit24 Gym Center',
  'Đổ xăng xe máy': 'Motorbike Refueling',
  'Đầy bình xăng xe Lead': 'Full tank for Honda Lead',
  'Ăn tối cùng gia đình': 'Family Dinner Night',
  'Ăn lẩu Haidilao': 'Haidilao Hotpot Feast',
  'Mua sách kỹ năng mềm': 'Self-Help Books Shopping',
  'Sách Nguyên Lý 80/20 và Đắc Nhân Tâm': '80/20 Principle and How to Win Friends books',
  'Tiền lãi đầu tư cổ phiếu': 'Stock Investment Dividend',
  'Cổ tức nhận quý 2': 'Q2 Dividend Payout',
  'Mua quần áo đi đám cưới': 'Wedding Clothes Shopping',
  'Mua vest nhẹ và giày tây': 'Light blazer and leather shoes',
  'Xem phim cuối tuần rạp CGV': 'CGV Weekend Movie Night',
  'Vé xem phim + bỏng nước': 'Movie tickets + popcorn bundle',
  'Đăng ký gói Netflix': 'Netflix Premium Subscription',
  'Gia hạn gói Ultra HD tháng này': 'Renew Ultra HD subscription',
  'Đặt Highlands Coffee': 'Highlands Coffee Order',
  'Trà sen vàng và freeze trà xanh': 'Golden lotus tea & green tea freeze',

  // Savings Jars
  'Quỹ khẩn cấp (Emergency)': 'Emergency Fund',
  'Mua Macbook Pro M4': 'Buy Macbook Pro M4',
  'Đi du lịch Nhật Bản': 'Japan Trip Fund',
  'Mua ô tô': 'Buy a Car',
  'Mua oto': 'Buy a Car',
  'Mua xe': 'Buy a Car',
  'Mua xe hơi': 'Buy a Car',
  'Mua oto con': 'Buy a Car',
  'Mua ô tô con': 'Buy a Car',
  'Mua xe ô tô': 'Buy a Car',
  'Mua xe oto': 'Buy a Car'
};

function translateCategory(catName) {
  if (!catName) return '';
  const lang = (window.state && window.state.language) || localStorage.getItem('chi_tieu_lang') || 'vi';
  if (CATEGORY_TRANSLATIONS[lang] && CATEGORY_TRANSLATIONS[lang][catName]) {
    return CATEGORY_TRANSLATIONS[lang][catName];
  }
  return catName; // Fallback if custom category
}

function translateUserText(text) {
  if (!text) return '';
  const lang = (window.state && window.state.language) || localStorage.getItem('chi_tieu_lang') || 'vi';
  if (lang === 'vi') return text; // If Vietnamese, keep original

  // Exact match first
  if (USER_TEXT_TRANSLATIONS[text]) {
    return USER_TEXT_TRANSLATIONS[text];
  }

  // Try substring or word translation if possible, or case-insensitive exact match
  const lowerText = text.toLowerCase().trim();
  for (const [key, val] of Object.entries(USER_TEXT_TRANSLATIONS)) {
    if (key.toLowerCase() === lowerText) {
      return val;
    }
  }

  // Fallback: search and replace common Vietnamese words
  let translated = text;
  const wordReplacements = {
    'mua ô tô': 'buy a car',
    'mua oto': 'buy a car',
    'mua xe hơi': 'buy a car',
    'mua xe': 'buy a car',
    'ô tô': 'car',
    'oto': 'car',
    'xe hơi': 'car',
    'mua': 'buy',
    'lương': 'salary',
    'tiền thuê nhà': 'house rent',
    'tiền điện': 'electricity bill',
    'tiền nước': 'water bill',
    'tiền nhà': 'rent',
    'ăn uống': 'dining',
    'trà sữa': 'boba tea',
    'cà phê': 'coffee',
    'mua sắm': 'shopping',
    'đi siêu thị': 'supermarket',
    'siêu thị': 'supermarket',
    'xăng': 'gas',
    'xe máy': 'motorbike',
    'du lịch': 'travel',
    'học phí': 'tuition',
    'sách': 'books',
    'tiền lãi': 'interest / dividends',
    'đầu tư': 'investment',
    'quà tặng': 'gift',
    'quà': 'gift',
    'thưởng': 'bonus',
    'quần áo': 'clothes',
    'phim': 'movie',
    'ăn tối': 'dinner',
    'ăn trưa': 'lunch',
    'ăn sáng': 'breakfast',
    'phòng gym': 'gym',
    'tập gym': 'gym training',
    'thiết kế': 'design',
    'dự án': 'project',
    'cổ tức': 'dividend',
    'đám cưới': 'wedding'
  };

  for (const [key, val] of Object.entries(wordReplacements)) {
    const regex = new RegExp(key, 'gi');
    if (regex.test(translated)) {
      translated = translated.replace(regex, val);
    }
  }

  // Capitalize first letter if the original had a capital first letter
  if (text && text[0] === text[0].toUpperCase() && translated.length > 0) {
    translated = translated.charAt(0).toUpperCase() + translated.slice(1);
  }

  return translated;
}

window.TRANSLATIONS = TRANSLATIONS;
window.CATEGORY_TRANSLATIONS = CATEGORY_TRANSLATIONS;
window.translateCategory = translateCategory;
window.translateUserText = translateUserText;
