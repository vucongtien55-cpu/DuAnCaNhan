// 6. JS controller for forgot.html

function initForgotPage() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Update languages indicator
    document.getElementById('lang-indicator').innerText = lang.toUpperCase();

    // Translate static text
    document.getElementById('app-name-display').innerText = t.appName;
    document.getElementById('app-ver-display').innerText = t.appSubVersion;
    document.getElementById('banner-title').innerHTML = lang === 'vi' ? 'Kiểm soát dòng tiền,<br/>kiến tạo tương lai.' : 'Control cashflow,<br/>forge your future.';
    document.getElementById('banner-desc').innerText = lang === 'vi' ? 'Bảo mật hoàn toàn cục bộ trên máy của bạn, tải nhanh không cần cài đặt phức tạp.' : 'Fully local security on your machine, lightning fast load without complex setups.';

    document.getElementById('forgot-title').innerText = t.forgotTitle;
    document.getElementById('forgot-sub').innerText = t.forgotSubtitle;
    document.getElementById('email-label').innerText = t.emailLabel;
    document.getElementById('forgot-btn-text').innerText = t.sendResetLink;
    document.getElementById('back-to-login-text').innerText = t.backToLogin;
    document.getElementById('copyright-label').innerText = t.copyright;

    // Form submit handler
    const form = document.getElementById('forgot-form');
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const email = document.getElementById('forgot-email').value.trim();

        if (!email) {
            showToast(lang === 'vi' ? 'Hãy nhập email của bạn' : 'Please enter your email address', 'error');
            return;
        }

        // Simulate sending recovery email
        showToast(t.resetSuccess);
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 2500);
    });
}

if (document.readyState !== 'loading') {
    initForgotPage();
    if (window.lucide) window.lucide.createIcons();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        initForgotPage();
        if (window.lucide) window.lucide.createIcons();
    });
}
