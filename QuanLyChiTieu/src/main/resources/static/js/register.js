// 5. JS controller for register.html

function initRegisterPage() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Update languages indicator
    document.getElementById('lang-indicator').innerText = lang.toUpperCase();

    // Translate static text
    document.getElementById('app-name-display').innerText = t.appName;
    document.getElementById('app-ver-display').innerText = t.appSubVersion;
    document.getElementById('banner-title').innerHTML = lang === 'vi' ? 'Kiểm soát dòng tiền,<br/>kiến tạo tương lai.' : 'Control cashflow,<br/>forge your future.';
    document.getElementById('banner-desc').innerText = lang === 'vi' ? 'Bảo mật hoàn toàn cục bộ trên máy của bạn, tải nhanh không cần cài đặt phức tạp.' : 'Fully local security on your machine, lightning fast load without complex setups.';

    document.getElementById('register-title').innerText = t.registerTitle;
    document.getElementById('register-sub').innerText = t.registerSubtitle;
    document.getElementById('fullname-label').innerText = t.fullNameLabel;
    document.getElementById('email-label').innerText = t.emailLabel;
    document.getElementById('password-label').innerText = t.passwordLabel;
    document.getElementById('agree-label').innerText = t.agreeTerms;
    document.getElementById('register-btn-text').innerText = t.registerNow;
    document.getElementById('has-acc-label').innerText = t.hasAccount;
    document.getElementById('login-link-text').innerText = t.loginNow;
    document.getElementById('copyright-label').innerText = t.copyright;

    // Form submit handler
    const form = document.getElementById('register-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('reg-name').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const password = document.getElementById('reg-password').value;

        if (!name || !email || !password) {
            showToast(lang === 'vi' ? 'Hãy điền đầy đủ các thông tin bắt buộc' : 'Please fill in all required fields', 'error');
            return;
        }

        let backendSuccess = false;
        try {
            const regRes = await fetch(`${BACKEND_API_URL}/users/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password })
            });
            if (regRes.ok) {
                backendSuccess = true;
            } else {
                const errText = await regRes.text();
                if (errText.includes("already exists") || errText.includes("đã tồn tại")) {
                    showToast(lang === 'vi' ? 'Địa chỉ email này đã được sử dụng trên máy chủ!' : 'This email is already registered on backend!', 'error');
                    return;
                }
            }
        } catch (err) {
            console.log("Spring Boot registration offline, falling back to local storage.");
        }

        if (!backendSuccess) {
            const users = JSON.parse(localStorage.getItem('chi_tieu_users_list')) || {};
            if (users[email]) {
                showToast(lang === 'vi' ? 'Địa chỉ email này đã được sử dụng!' : 'This email address is already taken!', 'error');
                return;
            }

            // Save registration
            users[email] = { password, name };
            localStorage.setItem('chi_tieu_users_list', JSON.stringify(users));
        }

        showToast(t.registerSuccess);
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1000);
    });
}

if (document.readyState !== 'loading') {
    initRegisterPage();
    if (window.lucide) window.lucide.createIcons();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        initRegisterPage();
        if (window.lucide) window.lucide.createIcons();
    });
}
