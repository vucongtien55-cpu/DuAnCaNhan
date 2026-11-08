/**
 * NovaMart - API Client & Session Manager
 */

const API_BASE = '/api/v1';

// Session Storage Helpers
function getToken() {
    return localStorage.getItem('novamart_token');
}

function setToken(token) {
    if (token) localStorage.setItem('novamart_token', token);
    else localStorage.removeItem('novamart_token');
}

function getUser() {
    const userStr = localStorage.getItem('novamart_user');
    try {
        return userStr ? JSON.parse(userStr) : null;
    } catch (e) {
        return null;
    }
}

function setUser(user) {
    if (user) localStorage.setItem('novamart_user', JSON.stringify(user));
    else localStorage.removeItem('novamart_user');
}

function isLoggedIn() {
    return !!getToken();
}

function hasRole(roleName) {
    const user = getUser();
    if (!user || !user.roles) return false;
    return user.roles.includes(roleName);
}

function logout() {
    localStorage.removeItem('novamart_token');
    localStorage.removeItem('novamart_user');
    window.location.href = '/login.html';
}

// API Call Wrapper
async function apiRequest(endpoint, method = 'GET', data = null, requiresAuth = false) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    if (requiresAuth || getToken()) {
        const token = getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
    }

    const config = {
        method,
        headers
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        config.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, config);
        const resData = await response.json().catch(() => null);

        if (!response.ok) {
            if (response.status === 401 && requiresAuth) {
                showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'error');
                logout();
                return null;
            }
            const errMsg = (resData && resData.message) ? resData.message : `Lỗi hệ thống (${response.status})`;
            throw new Error(errMsg);
        }

        return resData;
    } catch (error) {
        console.error(`API Error [${method} ${endpoint}]:`, error);
        throw error;
    }
}

// Toast Notifications
function showToast(message, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type === 'error' ? 'toast-error' : (type === 'success' ? 'toast-success' : '')}`;
    toast.innerHTML = `
        <span>${message}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// Format Currency
function formatPrice(amount) {
    if (amount === undefined || amount === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

