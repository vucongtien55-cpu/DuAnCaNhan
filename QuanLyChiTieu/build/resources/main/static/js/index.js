// 4. JS controller for index.html (Login)

function initLoginPage() {
    const lang = state.language;
    const t = TRANSLATIONS[lang];

    // Update languages indicator
    document.getElementById('lang-indicator').innerText = lang.toUpperCase();

    // Translate static text
    document.getElementById('app-name-display').innerText = t.appName;
    document.getElementById('app-ver-display').innerText = t.appSubVersion;
    document.getElementById('banner-title').innerHTML = lang === 'vi' ? 'Kiểm soát dòng tiền,<br/>kiến tạo tương lai.' : 'Control cashflow,<br/>forge your future.';
    document.getElementById('banner-desc').innerText = lang === 'vi' ? 'Bảo mật hoàn toàn cục bộ trên máy của bạn, tải nhanh không cần cài đặt phức tạp.' : 'Fully local security on your machine, lightning fast load without complex setups.';

    document.getElementById('login-title').innerText = t.loginTitle;
    document.getElementById('login-sub').innerText = t.loginSubtitle;
    document.getElementById('email-label').innerText = t.emailLabel;
    document.getElementById('password-label').innerText = t.passwordLabel;
    document.getElementById('forgot-link').innerText = t.forgotPasswordLink;
    document.getElementById('login-btn-text').innerText = t.loginButton;
    document.getElementById('or-continue-label').innerText = t.orContinueWith;
    const googleLabel = document.getElementById('google-label');
    if (googleLabel) googleLabel.innerText = t.googleLogin;
    document.getElementById('phone-label').innerText = t.phoneLoginOption;
    document.getElementById('no-acc-label').innerText = t.noAccount;
    document.getElementById('reg-link-text').innerText = t.registerNow;
    document.getElementById('copyright-label').innerText = t.copyright;

    // Add login form submit listener
    const form = document.getElementById('login-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('auth-email').value.trim();
        const password = document.getElementById('auth-password').value;

        if (!email || !password) {
            showToast(lang === 'vi' ? 'Hãy điền đầy đủ email và mật khẩu' : 'Please fill in all email and password fields', 'error');
            return;
        }

        // Try Spring Boot authentication first!
        let backendSuccess = false;
        try {
            const loginRes = await fetch(`${BACKEND_API_URL}/users/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            if (loginRes.ok) {
                const userObj = await loginRes.json();
                backendSuccess = true;
            } else {
                if (loginRes.status === 401 || loginRes.status === 400) {
                    // Attempt registering if not found, to preserve the "auto-create" smooth experience
                    const regRes = await fetch(`${BACKEND_API_URL}/users/register`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email, password, name: email.split('@')[0] })
                    });
                    if (regRes.ok) {
                        backendSuccess = true;
                    } else {
                        showToast(lang === 'vi' ? 'Thông tin đăng nhập không hợp lệ!' : 'Invalid credentials!', 'error');
                        return;
                    }
                }
            }
        } catch (err) {
            console.log("Spring Boot login offline, falling back to local storage authentication.");
        }

        if (!backendSuccess) {
            // Local storage fallback
            const users = JSON.parse(localStorage.getItem('chi_tieu_users_list')) || {};
            if (!users[email]) {
                // Create user on the fly so it's simple to try out
                users[email] = { password: password, name: email.split('@')[0] };
                localStorage.setItem('chi_tieu_users_list', JSON.stringify(users));
            } else {
                if (users[email].password !== password) {
                    showToast(lang === 'vi' ? 'Mật khẩu không chính xác!' : 'Incorrect password!', 'error');
                    return;
                }
            }
        }

        // Store active user session
        localStorage.setItem('chi_tieu_user', email);
        state.user = email;

        showToast(t.loginSuccess);
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 800);
    });
}

// Simulated Social Google Sign in
function simulatedSocialLogin(provider) {
    const lang = state.language;
    const t = TRANSLATIONS[lang];
    const demoEmail = 'demo_user@gmail.com';

    // Create user database entry if not exists
    const users = JSON.parse(localStorage.getItem('chi_tieu_users_list')) || {};
    if (!users[demoEmail]) {
        users[demoEmail] = { password: 'password', name: 'Demo User' };
        localStorage.setItem('chi_tieu_users_list', JSON.stringify(users));
    }

    localStorage.setItem('chi_tieu_user', demoEmail);
    state.user = demoEmail;
    showToast(lang === 'vi' ? `Đăng nhập thành công bằng ${provider}!` : `Successfully signed in via ${provider}!`);

    setTimeout(() => {
        window.location.href = 'dashboard.html';
    }, 800);
}
window.simulatedSocialLogin = simulatedSocialLogin;

// Run initialization
if (document.readyState !== 'loading') {
    initLoginPage();
    initGoogleSignIn();
    if (window.lucide) window.lucide.createIcons();
} else {
    document.addEventListener('DOMContentLoaded', () => {
        initLoginPage();
        initGoogleSignIn();
        if (window.lucide) window.lucide.createIcons();
    });
}

function initGoogleSignIn() {
    if (!window.google || !google.accounts || !google.accounts.id) {
        console.warn('Google Identity Services is not ready yet.');
        return;
    }

    google.accounts.id.initialize({
        client_id: '1025345791854-9bb2pjb89srse1nngn20a22p406pme9t.apps.googleusercontent.com',
        callback: handleGoogleCredentialResponse
    });
}

function startGoogleLogin() {
    if (!window.google || !google.accounts || !google.accounts.id) {
        showToast(state.language === 'vi' ? 'Google chưa tải xong, vui lòng thử lại.' : 'Google is still loading, please try again.', 'error');
        return;
    }

    google.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
            showToast(state.language === 'vi' ? 'Trình duyệt đang chặn cửa sổ Google. Hãy thử lại hoặc kiểm tra cấu hình OAuth.' : 'Google sign-in prompt was blocked. Please check OAuth setup.', 'error');
        }
    });
}

function initGoogleSignIn() {
    if (!window.google || !google.accounts || !google.accounts.id) {
        setTimeout(initGoogleSignIn, 300);
        return;
    }

    const googleButton = document.getElementById('google-signin-button');
    if (!googleButton) {
        console.warn('google-signin-button not found.');
        return;
    }

    google.accounts.id.initialize({
        client_id: '1025345791854-9bb2pjb89srse1nngn20a22p406pme9t.apps.googleusercontent.com',
        callback: handleGoogleCredentialResponse
    });

    google.accounts.id.renderButton(googleButton, {
        theme: 'outline',
        size: 'large',
        type: 'standard',
        text: 'continue_with',
        shape: 'rectangular',
        width: 204
    });
}

async function handleGoogleCredentialResponse(response) {
    const lang = state.language;

    try {
        const res = await fetch(`${BACKEND_API_URL}/users/google-login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ credential: response.credential })
        });

        if (!res.ok) {
            showToast(lang === 'vi' ? 'Đăng nhập Google thất bại!' : 'Google sign-in failed!', 'error');
            return;
        }

        const user = await res.json();

        localStorage.setItem('chi_tieu_user', user.email);
        state.user = user.email;

        showToast(lang === 'vi' ? 'Đăng nhập Google thành công!' : 'Signed in with Google successfully!');

        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 800);
    } catch (err) {
        showToast(lang === 'vi' ? 'Backend chưa có API Google login.' : 'Google login backend API is missing.', 'error');
    }
}
