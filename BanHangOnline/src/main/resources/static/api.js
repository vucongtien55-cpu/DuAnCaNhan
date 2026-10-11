const TOKEN_KEY = "marketplace.accessToken";
const USER_KEY = "marketplace.user";
const API_BASE = document.querySelector('meta[name="api-base"]')?.content.replace(/\/$/, "") || "";

export function readSession() {
  const token = sessionStorage.getItem(TOKEN_KEY);
  const serializedUser = sessionStorage.getItem(USER_KEY);
  if (!token || !serializedUser) return null;
  try {
    return { token, user: JSON.parse(serializedUser) };
  } catch {
    clearSession();
    return null;
  }
}

export function saveSession(authResponse) {
  sessionStorage.setItem(TOKEN_KEY, authResponse.accessToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
}

export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export async function api(path, { method = "GET", body, signal, authenticated = true } = {}) {
  const session = readSession();
  const requestToken = session?.token || null;
  const headers = new Headers({ Accept: "application/json" });
  if (body !== undefined) headers.set("Content-Type", "application/json");
  if (authenticated && session?.token) headers.set("Authorization", `Bearer ${session.token}`);

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error("Không thể kết nối máy chủ. Hãy kiểm tra Backend đang chạy rồi thử lại.");
  }
  if (authenticated && requestToken !== (readSession()?.token || null)) {
    throw new DOMException("Phiên đăng nhập đã thay đổi.", "AbortError");
  }

  const text = response.status === 204 ? "" : await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    if (response.status === 401 && authenticated && session?.token) {
      clearSession();
      window.dispatchEvent(new CustomEvent("marketplace:unauthorized"));
    }
    const detail = typeof data === "object" && data
      ? data.detail || data.message || data.title
      : null;
    const messages = {
      400: "Thông tin gửi lên chưa hợp lệ.",
      401: "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.",
      403: "Tài khoản của bạn không có quyền thực hiện thao tác này.",
      404: "Không tìm thấy dữ liệu yêu cầu.",
      409: "Dữ liệu bị trùng hoặc đang xung đột.",
      423: "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.",
      500: "Máy chủ đang gặp sự cố. Vui lòng thử lại sau."
    };
    const error = new Error(detail || messages[response.status] || "Yêu cầu chưa thực hiện được.");
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}
