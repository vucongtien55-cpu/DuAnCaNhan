// 7. JS controller for phone-login.html

let otpSentState = false;

function initPhoneLoginPage() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Update languages indicator
    document.getElementById('lang-indicator').innerText = lang.toUpperCase();

    // Translate static text
    document.getElementById('app-name-display').innerText = t.appName;
    document.getElementById('app-ver-display').innerText = t.appSubVersion;
    document.getElementById('banner-title').innerHTML = lang === 'vi' ? 'Kiểm soát dòng tiền,<br/>kiến tạo tương lai.' : 'Control cashflow,<br/>forge your future.';
    document.getElementById('banner-desc').innerText = lang === 'vi' ? 'Bảo mật hoàn toàn cục bộ trên máy của bạn, tải nhanh không cần cài đặt phức tạp.' : 'Fully local security on your machine, lightning fast load without complex setups.';

    document.getElementById('phone-title').innerText = t.phoneTitle;
    document.getElementById('phone-sub').innerText = t.phoneSubtitle;
    document.getElementById('phone-label').innerText = t.phoneNumberLabel;
    document.getElementById('otp-label').innerText = t.otpLabel;
    document.getElementById('resend-otp-text').innerText = t.resendOtp;
    document.getElementById('back-to-login-text').innerText = t.backToLogin;
    document.getElementById('copyright-label').innerText = t.copyright;

    // Set initial button text
    updateButtonText();

    // Form submit handler
    const form = document.getElementById('phone-auth-form');
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const phone = document.getElementById('auth-phone').value.trim();

        if (!phone) {
            showToast(lang === 'vi' ? 'Vui lòng nhập số điện thoại' : 'Please enter your phone number', 'error');
            return;
        }

        if (!otpSentState) {
            // Step 1: Send OTP simulation
            otpSentState = true;
            document.getElementById('otp-container').classList.remove('hidden');
            document.getElementById('auth-otp').setAttribute('required', 'true');
            document.getElementById('auth-phone').setAttribute('readonly', 'true');
            document.getElementById('auth-phone').classList.add('bg-slate-100', 'dark:bg-slate-900', 'text-slate-400');

            updateButtonText();
            showToast(t.demoOtpSent, 'warning');
        } else {
            // Step 2: Verify OTP
            const otp = document.getElementById('auth-otp').value.trim();
            if (otp !== '123456') {
                showToast(lang === 'vi' ? 'Mã OTP không hợp lệ! Vui lòng thử lại với mã 123456' : 'Invalid OTP! Please try again with code 123456', 'error');
                return;
            }

            // Successful verification
            const email = `${phone}@phone.com`;
            const users = JSON.parse(localStorage.getItem('chi_tieu_users_list')) || {};
            if (!users[email]) {
                users[email] = { password: 'sms_verified_auth', name: `Phone (${phone})` };
                localStorage.setItem('chi_tieu_users_list', JSON.stringify(users));
            }

            localStorage.setItem('chi_tieu_user', email);
            state.user = email;

            showToast(t.demoOtpSuccess);
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1000);
        }
    });

    // Resend OTP trigger
    document.getElementById('resend-otp-text').addEventListener('click', () => {
        showToast(t.demoOtpSent, 'warning');
    });
}

function updateButtonText() {
    const btn = document.getElementById('submit-btn');
    const lang = state.language;
    const t = TRANSLATIONS[lang];
    if (!otpSentState) {
        btn.innerText = t.sendOtpButton;
        btn.className = "w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-extrabold shadow-md transition-all uppercase tracking-wider cursor-pointer";
    } else {
        btn.innerText = t.verifyOtpButton;
        btn.className = "w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-extrabold shadow-md transition-all uppercase tracking-wider cursor-pointer";
    }
}

if (document.readyState !== 'loading') {
    initPhoneLoginPage();
    if (window.lucide) window.lucide.createIcons();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        initPhoneLoginPage();
        if (window.lucide) window.lucide.createIcons();
    });
}
