/**
 * NovaMart - Global Navbar & Footer Component
 */

function renderNavbar() {
    const headerContainer = document.getElementById('siteHeader');
    if (!headerContainer) return;

    const user = getUser();
    const isLogged = isLoggedIn();
    const isAdmin = hasRole('ROLE_ADMIN');
    const isSeller = hasRole('ROLE_SELLER');

    let userMenuHtml = '';
    if (isLogged && user) {
        userMenuHtml = `
            <div class="user-dropdown" id="userDropdown">
                <button class="user-profile-btn" onclick="toggleDropdown(event)">
                    <div class="user-avatar-circle">${(user.fullName || user.email).charAt(0).toUpperCase()}</div>
                    <span>${user.fullName || user.email}</span>
                    <span style="font-size: 10px;">▼</span>
                </button>
                <div class="dropdown-menu" id="dropdownMenu">
                    <a href="/profile.html" class="dropdown-item">👤 <span data-i18n="profile">Hồ sơ cá nhân</span></a>
                    <a href="/orders.html" class="dropdown-item">📦 <span data-i18n="my_orders">Đơn hàng của tôi</span></a>
                    <div class="dropdown-divider"></div>
                    ${isSeller ? `<a href="/seller-dashboard.html" class="dropdown-item">🏪 <span data-i18n="seller_portal">Kênh Người Bán</span></a>` : `<a href="/seller-apply.html" class="dropdown-item">✨ <span data-i18n="seller_register">Đăng ký bán hàng</span></a>`}
                    ${isAdmin ? `<a href="/admin-dashboard.html" class="dropdown-item">🛡️ <span data-i18n="admin_portal">Admin Portal</span></a>` : ''}
                    <div class="dropdown-divider"></div>
                    <a href="javascript:void(0)" onclick="logout()" class="dropdown-item" style="color: var(--danger);">🚪 <span data-i18n="logout">Đăng xuất</span></a>
                </div>
            </div>
        `;
    } else {
        userMenuHtml = `
            <a href="/login.html" class="btn btn-outline btn-sm" data-i18n="login">Đăng nhập</a>
            <a href="/register.html" class="btn btn-primary btn-sm" data-i18n="register">Đăng ký</a>
        `;
    }

    let portalBadges = '';
    if (isAdmin) {
        portalBadges += `<a href="/admin-dashboard.html" class="admin-portal-badge">🛡️ ADMIN PORTAL</a>`;
    }
    if (isSeller) {
        portalBadges += `<a href="/seller-dashboard.html" class="seller-portal-btn" data-i18n="seller_portal">Kênh Người Bán</a>`;
    } else if (isLogged) {
        portalBadges += `<a href="/seller-apply.html" class="nav-link" style="font-weight: 600;" data-i18n="seller_register">Đăng ký bán hàng</a>`;
    }

    headerContainer.innerHTML = `
        <div class="container">
            <div class="navbar-inner">
                <a href="/index.html" class="logo-brand">
                    <span>NovaMart</span>
                    <span class="logo-dot">▪</span>
                </a>

                <ul class="nav-menu">
                    <li><a href="/index.html" class="nav-link" data-i18n="home">Trang chủ</a></li>
                    <li><a href="/products.html" class="nav-link" data-i18n="products">Sản phẩm</a></li>
                    <li><a href="/products.html?category=gom-thu-cong" class="nav-link" data-i18n="ceramics">Gốm thủ công</a></li>
                    <li><a href="/products.html?category=do-da-cao-cap" class="nav-link" data-i18n="leather">Đồ da cao cấp</a></li>
                    <li><a href="/products.html?category=thiet-bi-am-thanh" class="nav-link" data-i18n="audio">Thiết bị âm thanh</a></li>
                </ul>

                <div class="nav-actions">
                    <button class="btn-lang" onclick="toggleLanguage()" title="Change Language">
                        🌐 <span id="langBtnText">${currentLang === 'vi' ? 'Tiếng Việt' : 'English'}</span>
                    </button>

                    ${portalBadges}

                    <a href="/cart.html" class="cart-icon-btn" title="Giỏ hàng">
                        🛒
                        <span class="cart-count-badge" id="cartCountBadge">0</span>
                    </a>

                    ${userMenuHtml}
                </div>
            </div>
        </div>
    `;

    applyTranslations();
    updateCartCount();
}

function toggleDropdown(e) {
    e.stopPropagation();
    const dropdown = document.getElementById('userDropdown');
    if (dropdown) dropdown.classList.toggle('active');
}

document.addEventListener('click', () => {
    const dropdown = document.getElementById('userDropdown');
    if (dropdown) dropdown.classList.remove('active');
});

function toggleLanguage() {
    const nextLang = currentLang === 'vi' ? 'en' : 'vi';
    setLanguage(nextLang);
}

async function updateCartCount() {
    const badge = document.getElementById('cartCountBadge');
    if (!badge || !isLoggedIn()) return;

    try {
        const res = await apiRequest('/cart');
        if (res && res.data && res.data.shopGroups) {
            let total = 0;
            res.data.shopGroups.forEach(group => {
                if (group.items) total += group.items.length;
            });
            badge.innerText = total;
            badge.style.display = total > 0 ? 'flex' : 'none';
        }
    } catch (e) {
        // Silently fail if guest or cart empty
    }
}

function renderFooter() {
    const footerContainer = document.getElementById('siteFooter');
    if (!footerContainer) return;

    footerContainer.innerHTML = `
        <div class="container">
            <div class="footer-grid">
                <div>
                    <div class="footer-logo">NovaMart ▪</div>
                    <p class="footer-desc">Tuyển chọn nghệ nhân & thương hiệu thủ công tinh hoa Việt Nam. Mỗi món đồ là một câu chuyện chế tác giàu bản sắc.</p>
                </div>
                <div>
                    <h4 class="footer-heading">Khám phá</h4>
                    <ul class="footer-links">
                        <li class="footer-link-item"><a href="/products.html">Tất cả sản phẩm</a></li>
                        <li class="footer-link-item"><a href="/products.html?category=gom-thu-cong">Gốm Bát Tràng</a></li>
                        <li class="footer-link-item"><a href="/products.html?category=do-da-cao-cap">Đồ da Veg-tan</a></li>
                        <li class="footer-link-item"><a href="/products.html?category=thiet-bi-am-thanh">Âm thanh cổ điển</a></li>
                    </ul>
                </div>
                <div>
                    <h4 class="footer-heading">Dành cho đối tác</h4>
                    <ul class="footer-links">
                        <li class="footer-link-item"><a href="/seller-apply.html">Mở gian hàng kinh doanh</a></li>
                        <li class="footer-link-item"><a href="/seller-dashboard.html">Kênh Người Bán</a></li>
                        <li class="footer-link-item"><a href="/admin-dashboard.html">Admin Portal</a></li>
                    </ul>
                </div>
                <div>
                    <h4 class="footer-heading">Hỗ trợ khách hàng</h4>
                    <ul class="footer-links">
                        <li class="footer-link-item"><a href="#">Chính sách giao nhận & COD</a></li>
                        <li class="footer-link-item"><a href="#">Bảo hành & Đổi trả 7 ngày</a></li>
                        <li class="footer-link-item"><a href="#">Hotline: 1900 8888</a></li>
                    </ul>
                </div>
            </div>
            <div class="footer-bottom">
                <div>© 2026 NovaMart. Tất cả quyền được bảo lưu.</div>
                <div>Sàn Thương Mại Điện Tử Nghệ Nhân & Xưởng Chế Tác</div>
            </div>
        </div>
    `;
    applyTranslations();
}

document.addEventListener('DOMContentLoaded', () => {
    renderNavbar();
    renderFooter();
});

