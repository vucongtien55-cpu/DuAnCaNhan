import { api, clearSession, readSession, saveSession } from "./api.js";
import {
  authApi,
  cartApi,
  catalogApi,
  orderApi,
  userApi,
  workspaceApi
} from "./services.js";

const main = document.querySelector("#main");
const nav = document.querySelector("#main-nav");
const accountSlot = document.querySelector("#header-account");
const toastRegion = document.querySelector("#toast-region");
let session = readSession();
let activeController;
let renderVersion = 0;
let workspaceTab = "products";
let adminTab = "users";
let editingProduct;
let editingCategory;
let workspaceShopId = Number(sessionStorage.getItem("marketplace.shopId")) || null;
let workspaceShops = [];
let productFilters = {};

const roleNames = {
  ADMIN: "Quản trị viên",
  SHOP_OWNER: "Chủ cửa hàng",
  EMPLOYEE: "Nhân viên cửa hàng",
  CUSTOMER: "Khách hàng"
};

const money = (amount) => new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0
}).format(Number(amount || 0));

const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
})[character]);

const dateText = (value) => value
  ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value))
  : "Chưa cập nhật";

function newView() {
  activeController?.abort();
  activeController = new AbortController();
  return { signal: activeController.signal, version: ++renderVersion };
}

function isCurrent(view) {
  return !view.signal.aborted && view.version === renderVersion;
}

function notify(message, kind = "success") {
  const toast = document.createElement("div");
  toast.className = `toast toast-${kind}`;
  toast.textContent = message;
  toastRegion.append(toast);
  window.setTimeout(() => toast.remove(), 4200);
}

function setHeader() {
  const commonLinks = [
    ["#/", "Trang chủ"],
    ["#/shops", "Cửa hàng"],
    ["#/products", "Sản phẩm"]
  ];
  const userLinks = session ? [
    ...(session.user.role === "CUSTOMER" ? [["#/orders", "Đơn hàng"]] : []),
    ...(session.user.role === "ADMIN" || session.user.role === "SHOP_OWNER" || session.user.role === "EMPLOYEE"
      ? [["#/workspace", "Khu quản lý"]] : []),
    ...(session.user.role === "CUSTOMER" ? [["#/workspace", "Mở cửa hàng"]] : []),
    ["#/account", "Tài khoản"]
  ] : [];
  nav.innerHTML = [...commonLinks, ...userLinks]
    .map(([href, label]) => `<a href="${href}">${label}</a>`)
    .join("");
  document.querySelectorAll(".header-search").forEach((element) => element.remove());
  const headerSearch = `<form class="header-search" data-form="header-search" role="search">
    <label class="visually-hidden" for="header-search-input">Tìm sản phẩm</label>
    <input id="header-search-input" name="name" type="search" placeholder="Tìm sản phẩm, danh mục hoặc cửa hàng">
    <button class="button button-primary button-small" type="submit">Tìm kiếm</button>
  </form>`;
  nav.insertAdjacentHTML("afterend", headerSearch);
  accountSlot.innerHTML = session
    ? `<a class="cart-link" href="#/cart" aria-label="Giỏ hàng">Giỏ hàng</a>
       <span class="account-chip">${escapeHtml(session.user.fullName)} · ${escapeHtml(roleNames[session.user.role] || "Tài khoản")}</span>
       <button type="button" class="button button-quiet button-small" data-action="logout">Đăng xuất</button>`
    : `<a class="button button-quiet button-small" href="#/login">Đăng nhập</a>
       <a class="button button-primary button-small" href="#/register">Đăng ký</a>`;
}

function field(label, name, options = {}) {
  const { type = "text", value = "", required = false, placeholder = "", min, max, step, autocomplete, disabled = false } = options;
  const minimumAttribute = min !== undefined
    ? (type === "number" ? `min="${escapeHtml(min)}"` : `minlength="${escapeHtml(min)}"`) : "";
  const maximumAttribute = max !== undefined
    ? (["number", "date"].includes(type) ? `max="${escapeHtml(max)}"` : `maxlength="${escapeHtml(max)}"`) : "";
  return `<label class="field">
    <span>${escapeHtml(label)}</span>
    <input name="${escapeHtml(name)}" type="${escapeHtml(type)}" value="${escapeHtml(value)}"
      ${required ? "required" : ""} ${placeholder ? `placeholder="${escapeHtml(placeholder)}"` : ""}
      ${minimumAttribute}
      ${maximumAttribute}
      ${step !== undefined ? `step="${escapeHtml(step)}"` : ""}
      ${autocomplete ? `autocomplete="${escapeHtml(autocomplete)}"` : ""}
      ${disabled ? "disabled" : ""}>
  </label>`;
}

function selectField(label, name, choices, selected = "") {
  return `<label class="field"><span>${escapeHtml(label)}</span><select name="${escapeHtml(name)}">
    ${choices.map(([value, text]) => `<option value="${escapeHtml(value)}" ${String(selected) === String(value) ? "selected" : ""}>${escapeHtml(text)}</option>`).join("")}
  </select></label>`;
}

function pageHeading(title, description = "", action = "") {
  return `<div class="page-heading"><div><h1>${escapeHtml(title)}</h1>
    ${description ? `<p>${escapeHtml(description)}</p>` : ""}</div>${action}</div>`;
}

function emptyState(message, action = "") {
  return `<div class="empty-state"><span class="empty-icon" aria-hidden="true">—</span>
    <p>${escapeHtml(message)}</p>${action}</div>`;
}

function listState(items, renderItem, emptyMessage) {
  return Array.isArray(items) && items.length
    ? `<div class="card-list">${items.map(renderItem).join("")}</div>`
    : emptyState(emptyMessage);
}

function errorPage(error, retry) {
  return `<section class="error-panel">
    <h2>Chưa tải được dữ liệu</h2><p>${escapeHtml(error.message)}</p>
    <button class="button button-secondary" type="button" data-action="${escapeHtml(retry)}">Thử lại</button>
  </section>`;
}

async function loadPage(loader, render, retry, view) {
  try {
    const data = await loader(view.signal);
    if (isCurrent(view)) main.innerHTML = render(data);
  } catch (error) {
    if (error.name !== "AbortError" && isCurrent(view)) main.innerHTML = errorPage(error, retry);
  }
}

function routeInfo() {
  const [path = "/", query = ""] = location.hash.replace(/^#/, "").split("?");
  return { path: path || "/", params: new URLSearchParams(query) };
}

async function renderRoute() {
  setHeader();
  const view = newView();
  const { path, params } = routeInfo();
  main.innerHTML = `<div class="loading-state" role="status">Đang tải…</div>`;

  if (path === "/login") return renderLogin();
  if (path === "/register") return renderRegister();
  if (path === "/403") return renderStatusPage(403);
  if (path === "/404") return renderStatusPage(404);
  if (path === "/500") return renderStatusPage(500);
  if (path === "/forgot-password") return renderForgotPassword();
  if (path === "/reset-password") return renderPasswordReset(params.get("token"));
  if (path === "/shops") return renderShops(params.get("name") || "");
  if (path.startsWith("/shop/")) return renderShop(path.split("/")[2], view);
  if (path === "/products") return renderProducts(params, view);
  if (path.startsWith("/product/")) return renderProduct(path.split("/")[2], view);

  if (path === "/cart") {
    if (!requireRole(["CUSTOMER", "ADMIN"])) return;
    return renderCart(view);
  }
  if (path === "/orders") {
    if (!requireRole(["CUSTOMER", "ADMIN"])) return;
    return renderOrders(view);
  }
  if (path === "/account") {
    if (!requireRole(["ADMIN", "SHOP_OWNER", "EMPLOYEE", "CUSTOMER"])) return;
    return renderAccount(view);
  }
  if (path === "/workspace") {
    if (!requireRole(["ADMIN", "SHOP_OWNER", "EMPLOYEE", "CUSTOMER"])) return;
    if (session.user.role === "ADMIN") return renderAdmin(view);
    return renderShopWorkspace(view);
  }
  if (path === "/") return renderHome(view);
  return renderStatusPage(404);
}

function renderStatusPage(status) {
  const pages = {
    403: ["Không có quyền truy cập", "Tài khoản của bạn không được phép truy cập nội dung này."],
    404: ["Không tìm thấy trang", "Đường dẫn này không tồn tại hoặc nội dung đã được chuyển."],
    500: ["Máy chủ đang gặp sự cố", "Yêu cầu chưa thể hoàn tất. Vui lòng thử lại sau."]
  };
  const [title, description] = pages[status] || pages[404];
  main.innerHTML = `${pageHeading(`${status} · ${title}`)}
    ${emptyState(description, '<a class="button button-primary" href="#/">Về trang chủ</a>')}`;
}

function requireRole(roles) {
  if (!session) {
    location.hash = "#/login";
    notify("Vui lòng đăng nhập để tiếp tục.", "error");
    return false;
  }
  if (!roles.includes(session.user.role)) {
    main.innerHTML = `${pageHeading("Không có quyền truy cập")}
      ${emptyState("Tài khoản hiện tại không được phép mở trang này.", '<a class="button button-secondary" href="#/">Về trang chủ</a>')}`;
    return false;
  }
  return true;
}

function renderLogin() {
  main.innerHTML = `<section class="auth-layout">
    <div class="auth-intro"><span class="eyebrow">Chào mừng trở lại</span><h1>Mua sắm và quản lý cửa hàng trong một nơi.</h1>
      <p>Đăng nhập bằng tài khoản Marketplace của bạn để tiếp tục.</p></div>
    <form class="panel form-stack" data-form="login">
      <h2>Đăng nhập</h2>
      ${field("Email", "email", { type: "email", required: true, autocomplete: "email" })}
      <label class="field"><span>Mật khẩu</span><span class="password-field">
        <input name="password" type="password" required autocomplete="current-password">
        <button type="button" class="text-button" data-action="toggle-password">Hiện</button>
      </span></label>
      <p class="form-note"><a href="#/forgot-password">Quên mật khẩu?</a></p>
      <button class="button button-primary button-wide" type="submit">Đăng nhập</button>
      <p class="form-note">Chưa có tài khoản? <a href="#/register">Tạo tài khoản khách hàng</a></p>
    </form>
  </section>`;
}

function renderForgotPassword() {
  main.innerHTML = `<section class="auth-layout"><div class="auth-intro">
    <span class="eyebrow">Khôi phục tài khoản</span><h1>Nhận liên kết đặt lại mật khẩu qua email.</h1>
    <p>Vì lý do bảo mật, hệ thống không xác nhận email có gắn với tài khoản hay không.</p></div>
    <form class="panel form-stack" data-form="password-reset-request"><h2>Quên mật khẩu</h2>
      ${field("Email tài khoản", "email", { type: "email", required: true, max: 180, autocomplete: "email" })}
      <p class="form-note">Backend cần cấu hình SMTP để gửi email. Nếu SMTP chưa cấu hình, yêu cầu sẽ báo chưa khả dụng.</p>
      <button class="button button-primary button-wide" type="submit">Gửi yêu cầu khôi phục</button>
      <a href="#/login">Quay lại đăng nhập</a></form></section>`;
}

function renderPasswordReset(token) {
  main.innerHTML = `<section class="auth-layout"><div class="auth-intro">
    <span class="eyebrow">Bảo mật tài khoản</span><h1>Đặt mật khẩu mới.</h1>
    <p>Liên kết chỉ dùng một lần và hết hạn sau 30 phút.</p></div>
    <form class="panel form-stack" data-form="password-reset" data-token="${escapeHtml(token || "")}">
      <h2>Đặt lại mật khẩu</h2>
      ${field("Mật khẩu mới", "newPassword", { type: "password", required: true, min: 8, max: 72, autocomplete: "new-password" })}
      ${field("Xác nhận mật khẩu", "confirmPassword", { type: "password", required: true, min: 8, max: 72, autocomplete: "new-password" })}
      <button class="button button-primary button-wide" type="submit">Lưu mật khẩu mới</button>
    </form></section>`;
}

function renderRegister() {
  main.innerHTML = `<section class="auth-layout">
    <div class="auth-intro"><span class="eyebrow">Tham gia Marketplace</span><h1>Tạo tài khoản mua sắm của bạn.</h1>
      <p>Vai trò khách hàng được gán an toàn ở Backend khi đăng ký.</p></div>
    <form class="panel form-stack" data-form="register">
      <h2>Tạo tài khoản</h2>
      ${field("Họ và tên", "fullName", { required: true, max: 120, autocomplete: "name" })}
      ${field("Số điện thoại", "phone", { max: 20, autocomplete: "tel" })}
      ${field("Email", "email", { type: "email", required: true, max: 180, autocomplete: "email" })}
      ${field("Mật khẩu", "password", { type: "password", required: true, min: 8, max: 72, autocomplete: "new-password" })}
      ${field("Xác nhận mật khẩu", "confirmPassword", { type: "password", required: true, min: 8, max: 72, autocomplete: "new-password" })}
      <label class="check-field"><input name="terms" type="checkbox" required><span>Tôi đồng ý với điều khoản sử dụng.</span></label>
      <button class="button button-primary button-wide" type="submit">Đăng ký</button>
      <p class="form-note">Đã có tài khoản? <a href="#/login">Đăng nhập</a></p>
    </form>
  </section>`;
}

async function renderHome(view) {
  const [categoriesResult, productsResult] = await Promise.allSettled([
    catalogApi.categories(view.signal),
    catalogApi.searchCatalog({ page: 0, size: 8, sort: "newest" }, view.signal)
  ]);
  if (!isCurrent(view)) return;

  main.innerHTML = `
    <section class="market-hero">
      <div class="hero-copy">
        <span class="eyebrow">Sàn mua sắm nhiều cửa hàng</span>
        <h1>Mua hàng dễ hơn, chọn shop tin cậy hơn.</h1>
        <p>Khám phá sản phẩm mới, danh mục đang mở bán và các cửa hàng hoạt động trực tiếp từ hệ thống Marketplace.</p>
        <form class="home-search" data-form="home-search" role="search">
          <label class="visually-hidden" for="home-search-input">Tìm sản phẩm</label>
          <input id="home-search-input" name="name" type="search" placeholder="Bạn muốn tìm sản phẩm gì hôm nay?">
          <button class="button button-primary" type="submit">Tìm kiếm</button>
        </form>
        <div class="hero-actions">
          <a class="button button-secondary" href="#/products">Khám phá sản phẩm</a>
          <a class="button button-quiet" href="#/shops">Tìm cửa hàng</a>
        </div>
      </div>
      <div class="market-hero-panel" aria-label="Thông tin nổi bật">
        <span>Marketplace</span>
        <strong>Sản phẩm thật từ dữ liệu API</strong>
        <p>Danh mục và sản phẩm trên trang chủ được tải từ backend hiện có.</p>
      </div>
    </section>

    <section class="content-section home-section">
      <div class="section-heading">
        <div><span class="eyebrow">Danh mục</span><h2>Khám phá theo ngành hàng</h2></div>
        <a href="#/products">Xem tất cả <span aria-hidden="true">→</span></a>
      </div>
      ${homeCategories(categoriesResult)}
    </section>

    <section class="content-section home-section">
      <div class="section-heading">
        <div><span class="eyebrow">Sản phẩm mới</span><h2>Gợi ý từ Marketplace</h2></div>
        <a href="#/products?sort=newest">Xem thêm <span aria-hidden="true">→</span></a>
      </div>
      ${homeProducts(productsResult)}
    </section>

    <section class="content-section home-section">
      <div class="section-heading">
        <div><span class="eyebrow">Cửa hàng</span><h2>Tìm cửa hàng đang hoạt động</h2></div>
        <a href="#/shops">Tìm cửa hàng <span aria-hidden="true">→</span></a>
      </div>
      <div class="shop-discovery">
        <div>
          <strong>Chưa có API cửa hàng nổi bật công khai.</strong>
          <p>Backend hiện hỗ trợ tìm cửa hàng theo tên qua API thật. Trang chủ không hiển thị dữ liệu cửa hàng giả.</p>
        </div>
        <a class="button button-secondary" href="#/shops">Tra cứu cửa hàng</a>
      </div>
    </section>`;
}

function homeCategories(result) {
  if (result.status === "rejected") {
    return `<div class="error-panel"><h2>Chưa tải được danh mục</h2><p>${escapeHtml(result.reason?.message || "Vui lòng thử lại sau.")}</p></div>`;
  }
  const categories = result.value || [];
  return categories.length ? `<div class="category-grid home-category-grid">${categories.slice(0, 8).map((category) => `
    <a class="category-card home-category-card" href="#/products?category=${category.id}">
      <span class="category-symbol" aria-hidden="true">${escapeHtml((category.name || "?").slice(0, 1))}</span>
      <strong>${escapeHtml(category.name)}</strong>
      <span>${escapeHtml(category.description || "Xem sản phẩm trong danh mục")}</span>
    </a>`).join("")}</div>` : emptyState("Chưa có danh mục đang hoạt động.");
}

function homeProducts(result) {
  if (result.status === "rejected") {
    return `<div class="error-panel"><h2>Chưa tải được sản phẩm</h2><p>${escapeHtml(result.reason?.message || "Vui lòng thử lại sau.")}</p></div>`;
  }
  const products = result.value?.content || [];
  return products.length ? `<div class="product-grid home-product-grid">${products.map(homeProductCard).join("")}</div>`
    : emptyState("Chưa có sản phẩm đang được mở bán.");
}

function homeProductCard(product) {
  return `<article class="product-card home-product-card">
    <a class="product-image" href="#/product/${product.id}" aria-label="Xem ${escapeHtml(product.name)}">
      ${product.imageUrl ? `<img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" loading="lazy">`
        : `<span class="image-placeholder" aria-hidden="true">Ảnh sản phẩm</span>`}
    </a>
    <div class="product-card-body">
      <a class="product-title" href="#/product/${product.id}">${escapeHtml(product.name)}</a>
      <strong class="product-price">${money(product.price)}</strong>
      ${product.shopId ? `<span class="muted">Cửa hàng #${escapeHtml(product.shopId)}</span>` : ""}
      <span class="stock-label ${product.stockQuantity > 0 ? "" : "out-of-stock"}">
        ${product.stockQuantity > 0 ? `Còn ${escapeHtml(product.stockQuantity)} sản phẩm` : "Hết hàng"}
      </span>
      <a class="button button-secondary button-small" href="#/product/${product.id}">Xem chi tiết</a>
    </div>
  </article>`;
}

async function renderShops(initialName = "") {
  main.innerHTML = `${pageHeading("Cửa hàng", "Tìm cửa hàng đang hoạt động trên Marketplace.")}
    <form class="search-bar panel" data-form="shop-search">
      ${field("Tên cửa hàng", "name", { value: initialName, required: true, placeholder: "Ví dụ: cửa hàng công nghệ" })}
      <button class="button button-primary" type="submit">Tìm cửa hàng</button>
    </form>
    <div id="shop-results">${emptyState("Nhập tên cửa hàng để bắt đầu tìm kiếm.")}</div>`;
  if (initialName) await searchShops(initialName);
}

async function searchShops(name) {
  const results = document.querySelector("#shop-results");
  results.innerHTML = `<div class="loading-state">Đang tìm cửa hàng…</div>`;
  try {
    const shops = await catalogApi.searchShops(name, activeController?.signal);
    results.innerHTML = listState(shops, (shop) => `
      <article class="shop-card"><div class="shop-card-mark" aria-hidden="true">${escapeHtml(shop.name.slice(0, 1))}</div>
        <div class="shop-card-content"><h2>${escapeHtml(shop.name)}</h2>
          <p>${escapeHtml(shop.description || "Cửa hàng Marketplace")}</p>
          <span>${escapeHtml(shop.address || "Chưa cập nhật địa chỉ")}</span>
        </div><a class="button button-secondary" href="#/shop/${shop.id}">Xem sản phẩm</a>
      </article>`, "Không tìm thấy cửa hàng phù hợp.");
  } catch (error) {
    if (error.name !== "AbortError") results.innerHTML = errorPage(error, "shops");
  }
}

async function renderProducts(params = new URLSearchParams(), view) {
  let categories;
  try {
    categories = await catalogApi.categories(view.signal);
  } catch (error) {
    if (error.name !== "AbortError" && isCurrent(view)) main.innerHTML = errorPage(error, "products");
    return;
  }
  if (!isCurrent(view)) return;
  const selectedCategory = params.get("category") || "";
  main.innerHTML = `${pageHeading("Sản phẩm", "Tìm theo tên, danh mục hoặc khoảng giá.")}
    <section class="panel filter-panel">
      <form class="filter-form" data-form="product-search">
        ${field("Tên sản phẩm", "name", { value: params.get("name") || "", placeholder: "Nhập tên sản phẩm" })}
        ${selectField("Danh mục", "categoryId", [["", "Tất cả danh mục"], ...categories.map((item) => [item.id, item.name])], selectedCategory)}
        ${field("Giá từ (đ)", "minimum", { type: "number", min: 0, step: "1", value: params.get("minimum") || "" })}
        ${field("Đến giá (đ)", "maximum", { type: "number", min: 0, step: "1", value: params.get("maximum") || "" })}
        ${selectField("Sắp xếp", "sort",
          [["newest", "Mới nhất"], ["price-asc", "Giá tăng dần"], ["price-desc", "Giá giảm dần"]],
          params.get("sort") || "newest")}
        <button class="button button-primary" type="submit">Lọc sản phẩm</button>
      </form>
    </section>
    <div id="product-results">${emptyState("Nhập từ khóa, chọn danh mục hoặc khoảng giá để xem dữ liệu từ cửa hàng.")}</div>`;
  await searchProducts({
    name: params.get("name"),
    categoryId: selectedCategory,
    minimum: params.get("minimum"),
    maximum: params.get("maximum"),
    sort: params.get("sort") || "newest",
    page: Number(params.get("page") || 0)
  });
}

async function searchProducts(filters) {
  const results = document.querySelector("#product-results");
  results.innerHTML = `<div class="loading-state">Đang tải sản phẩm…</div>`;
  try {
    if (filters.minimum && filters.maximum && Number(filters.minimum) > Number(filters.maximum)) {
      throw new Error("Giá từ không được lớn hơn giá đến.");
    }
    const data = await catalogApi.searchCatalog({
      name: filters.name?.trim() || undefined,
      categoryId: filters.categoryId || undefined,
      minimum: filters.minimum || undefined,
      maximum: filters.maximum || undefined,
      page: Number(filters.page || 0),
      size: 12,
      sort: filters.sort || "newest"
    }, activeController?.signal);
    productFilters = { ...filters, page: data.page };
    results.innerHTML = `${productGrid(data.content)}
      ${data.totalPages > 1 ? `<nav class="pagination" aria-label="Phân trang sản phẩm">
        <button class="button button-secondary button-small" data-action="product-page" data-page="${data.page - 1}" ${data.first ? "disabled" : ""}>Trang trước</button>
        <span>Trang ${data.page + 1} / ${data.totalPages} · ${data.totalElements} sản phẩm</span>
        <button class="button button-secondary button-small" data-action="product-page" data-page="${data.page + 1}" ${data.last ? "disabled" : ""}>Trang sau</button>
      </nav>` : `<p class="result-count">${data.totalElements} sản phẩm</p>`}`;
  } catch (error) {
    if (error.name !== "AbortError") results.innerHTML = errorPage(error, "products");
  }
}

function productGrid(products) {
  return Array.isArray(products) && products.length
    ? `<div class="product-grid">${products.map(productCard).join("")}</div>`
    : emptyState("Không tìm thấy sản phẩm phù hợp.");
}

function productCard(product) {
  return `<article class="product-card">
    <a class="product-image" href="#/product/${product.id}" aria-label="Xem ${escapeHtml(product.name)}">
      ${product.imageUrl ? `<img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" loading="lazy">`
        : `<span class="image-placeholder" aria-hidden="true">Ảnh sản phẩm</span>`}
    </a>
    <div class="product-card-body"><a class="product-title" href="#/product/${product.id}">${escapeHtml(product.name)}</a>
      <strong class="product-price">${money(product.price)}</strong>
      <span class="stock-label ${product.stockQuantity > 0 ? "" : "out-of-stock"}">
        ${product.stockQuantity > 0 ? `Còn ${escapeHtml(product.stockQuantity)} sản phẩm` : "Hết hàng"}
      </span>
      ${session && ["CUSTOMER", "ADMIN"].includes(session.user.role) && product.stockQuantity > 0
        ? `<button class="button button-primary button-small" data-action="add-cart" data-product-id="${product.id}">Thêm vào giỏ</button>`
        : `<a class="button button-secondary button-small" href="#/product/${product.id}">Xem chi tiết</a>`}
    </div>
  </article>`;
}

async function renderProduct(productId, view) {
  try {
    const product = await catalogApi.product(productId, view.signal);
    if (!isCurrent(view)) return;
    let [reviews, shop] = await Promise.all([
      catalogApi.reviews(productId, null, view.signal).catch(() => []),
      catalogApi.shop(product.shopId, view.signal).catch(() => null)
    ]);
    if (!isCurrent(view)) return;
    main.innerHTML = `<div class="breadcrumbs"><a href="#/">Trang chủ</a><span>/</span><a href="#/shops">Cửa hàng</a><span>/</span><span>${escapeHtml(product.name)}</span></div>
      <section class="product-detail panel">
        <div class="detail-image">${product.imageUrl
          ? `<img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}">`
          : `<span class="image-placeholder">Ảnh sản phẩm</span>`}</div>
        <div class="detail-copy"><span class="eyebrow">${escapeHtml(shop?.name || "Marketplace")}</span>
          <h1>${escapeHtml(product.name)}</h1><p>${escapeHtml(product.description || "Chưa có mô tả sản phẩm.")}</p>
          <strong class="detail-price">${money(product.price)}</strong>
          <span class="stock-label ${product.stockQuantity > 0 ? "" : "out-of-stock"}">
            ${product.stockQuantity > 0 ? `Còn ${escapeHtml(product.stockQuantity)} sản phẩm` : "Hết hàng"}
          </span>
          ${shop ? `<p><strong>Cửa hàng:</strong> ${escapeHtml(shop.name)}</p>` : ""}
          ${session && ["CUSTOMER", "ADMIN"].includes(session.user.role) && product.stockQuantity > 0
            ? `<form class="inline-form" data-form="add-cart">
                <input type="hidden" name="productId" value="${product.id}">
                ${field("Số lượng", "quantity", { type: "number", value: 1, min: 1, max: product.stockQuantity, required: true })}
                <button class="button button-primary" type="submit">Thêm vào giỏ</button>
              </form>`
            : `<a class="button button-primary" href="#/login">Đăng nhập để mua hàng</a>`}
        </div>
      </section>
      <section class="content-section"><div class="section-heading"><div><span class="eyebrow">Cộng đồng</span><h2>Đánh giá sản phẩm</h2></div>
        <label class="compact-filter">Lọc số sao
          <select id="review-rating"><option value="">Tất cả</option>${[5, 4, 3, 2, 1].map((n) => `<option value="${n}">${n} sao</option>`).join("")}</select>
        </label>
      </div><div id="review-list">${reviewList(reviews)}</div>
      ${session?.user.role === "CUSTOMER" ? `<form class="panel form-stack review-form" data-form="review" data-product-id="${product.id}">
        <h3>Viết đánh giá</h3>${selectField("Số sao", "rating", [[5, "5 sao"], [4, "4 sao"], [3, "3 sao"], [2, "2 sao"], [1, "1 sao"]], 5)}
        <label class="field"><span>Nội dung</span><textarea name="comment" rows="3" maxlength="2000"></textarea></label>
        <p class="form-note">Backend chỉ chấp nhận đánh giá khi bạn đã nhận sản phẩm.</p>
        <button class="button button-secondary" type="submit">Gửi đánh giá</button>
      </form>` : ""}
      `;
    document.querySelector("#review-rating").addEventListener("change", async (event) => {
      try {
        const list = await catalogApi.reviews(productId, event.target.value || null, activeController?.signal);
        document.querySelector("#review-list").innerHTML = reviewList(list);
      } catch (error) { notify(error.message, "error"); }
    });
  } catch (error) {
    if (error.name !== "AbortError" && isCurrent(view)) main.innerHTML = errorPage(error, "product");
  }
}

function reviewList(reviews) {
  return Array.isArray(reviews) && reviews.length ? `<div class="review-list">${reviews.map((review) => `
    <article class="review-card"><div class="review-stars" aria-label="${review.rating} trên 5 sao">${"★".repeat(review.rating)}${"☆".repeat(5 - review.rating)}</div>
      <p>${escapeHtml(review.comment || "Khách hàng không để lại nhận xét.")}</p>
      <time>${dateText(review.createdAt)}</time></article>`).join("")}</div>`
    : emptyState("Sản phẩm chưa có đánh giá.");
}

async function renderShop(shopId, view) {
  try {
    const [shop, products] = await Promise.all([
      catalogApi.shop(shopId, view.signal),
      catalogApi.shopProducts(shopId, view.signal)
    ]);
    if (!isCurrent(view)) return;
    main.innerHTML = `<div class="breadcrumbs"><a href="#/">Trang chủ</a><span>/</span><a href="#/shops">Cửa hàng</a><span>/</span><span>${escapeHtml(shop.name)}</span></div>
      <section class="shop-profile panel"><div class="shop-card-mark" aria-hidden="true">${escapeHtml(shop.name.slice(0, 1))}</div>
        <div><span class="eyebrow">Cửa hàng Marketplace</span><h1>${escapeHtml(shop.name)}</h1>
          <p>${escapeHtml(shop.description || "Chưa có mô tả.")}</p>
          <span>${escapeHtml(shop.address || "Chưa cập nhật địa chỉ")}${shop.phone ? ` · ${escapeHtml(shop.phone)}` : ""}</span>
        </div></section>
      <section class="content-section"><div class="section-heading"><div><span class="eyebrow">Đang kinh doanh</span><h2>Sản phẩm của cửa hàng</h2></div></div>
        ${productGrid(products)}</section>`;
  } catch (error) {
    if (error.name !== "AbortError" && isCurrent(view)) main.innerHTML = errorPage(error, "shops");
  }
}

async function renderCart(view) {
  await loadPage((signal) => cartApi.list(signal), (items) => {
    const total = items.reduce((sum, item) => sum + Number(item.unitPrice) * item.quantity, 0);
    return `${pageHeading("Giỏ hàng", "Số lượng và giá hiển thị lấy từ Backend.")}
      ${items.length ? `<div class="cart-layout"><div class="card-list">${items.map((item) => `
        <article class="cart-item panel"><div><a class="product-title" href="#/product/${item.productId}">${escapeHtml(item.productName)}</a>
          <p class="muted">${money(item.unitPrice)} / sản phẩm</p></div>
          <form class="inline-form" data-form="cart-update" data-item-id="${item.id}">
            <label class="field"><span>Số lượng</span><input name="quantity" type="number" min="1" value="${item.quantity}" required></label>
            <button class="button button-secondary button-small" type="submit">Cập nhật</button>
          </form>
          <strong>${money(Number(item.unitPrice) * item.quantity)}</strong>
          <button class="button button-danger-quiet button-small" data-action="remove-cart" data-item-id="${item.id}">Xóa</button>
        </article>`).join("")}</div>
        <aside class="panel summary-panel"><h2>Tóm tắt</h2><div class="summary-row"><span>Tạm tính</span><strong>${money(total)}</strong></div>
          <p class="form-note">Phí vận chuyển và tổng tiền cuối cùng do Backend tính khi đặt hàng.</p>
          <button class="button button-primary button-wide" data-action="checkout">Tiến hành đặt hàng</button>
          <button class="text-button" data-action="clear-cart">Xóa toàn bộ giỏ hàng</button>
        </aside></div>` : emptyState("Giỏ hàng đang trống.", '<a class="button button-primary" href="#/products">Tiếp tục tìm sản phẩm</a>')}`;
  }, "cart", view);
}

async function renderOrders(view) {
  await loadPage((signal) => orderApi.list(null, signal), (orders) => `
    ${pageHeading("Đơn hàng của tôi", "Chỉ hiển thị đơn thuộc tài khoản đang đăng nhập.")}
    ${listState(orders, (order) => `<article class="order-card panel">
      <div class="order-head"><div><span class="eyebrow">Mã đơn</span><strong>${escapeHtml(order.orderNumber)}</strong></div>
        <span class="status-pill">${escapeHtml(statusText(order.status))}</span></div>
      <div class="order-summary"><span>${dateText(order.createdAt)}</span><span>${order.shopOrders?.length || 0} cửa hàng</span><strong>${money(order.totalAmount)}</strong></div>
      ${(order.shopOrders || []).map((shopOrder) => `<div class="suborder-row">
        <span>Đơn cửa hàng #${shopOrder.id}</span><span class="status-pill">${escapeHtml(statusText(shopOrder.status))}</span>
        <span>${money(shopOrder.totalAmount)}</span></div>
        <div class="order-lines">${(shopOrder.items || []).map((item) => `
          <div><span>${escapeHtml(item.productName)} × ${item.quantity}</span>
            <span>${money(item.lineTotal)}</span></div>`).join("")}</div>`).join("")}
      <div class="order-actions"><button class="button button-secondary button-small" data-action="payment" data-order-id="${order.id}">Thanh toán</button>
        ${order.status === "PENDING" ? `<button class="button button-danger-quiet button-small" data-action="cancel-order" data-order-id="${order.id}">Hủy đơn</button>` : ""}
      </div></article>`, "Bạn chưa có đơn hàng.", '<a class="button button-primary" href="#/products">Khám phá sản phẩm</a>')}
    `, "orders", view);
}

async function renderAccount(view) {
  await loadPage((signal) => userApi.profile(signal), (user) => `
    ${pageHeading("Thông tin tài khoản", "Hồ sơ cá nhân của phiên đăng nhập hiện tại.")}
    <form class="panel form-stack narrow-panel" data-form="profile">
      ${field("Họ và tên", "fullName", { value: user.fullName, required: true, max: 120 })}
      ${field("Email", "email", { value: user.email, type: "email", disabled: true })}
      ${field("Số điện thoại", "phone", { value: user.phone || "", max: 20 })}
      ${field("Đường dẫn ảnh đại diện", "avatarUrl", { value: user.avatarUrl || "", max: 255 })}
      <div class="profile-role"><span>Vai trò</span><strong>${escapeHtml(roleNames[user.role] || user.role)}</strong></div>
      <p class="form-note">Email chỉ được cập nhật sau khi Backend hỗ trợ xác minh địa chỉ mới. Ảnh đại diện phải dùng đường dẫn ảnh công khai.</p>
      <button class="button button-primary" type="submit">Lưu hồ sơ</button>
    </form>
    <form class="panel form-stack narrow-panel password-change-form" data-form="password-change">
      <h2>Đổi mật khẩu</h2>
      ${field("Mật khẩu hiện tại", "currentPassword", { type: "password", required: true, autocomplete: "current-password" })}
      ${field("Mật khẩu mới", "newPassword", { type: "password", required: true, min: 8, max: 72, autocomplete: "new-password" })}
      ${field("Xác nhận mật khẩu mới", "confirmPassword", { type: "password", required: true, min: 8, max: 72, autocomplete: "new-password" })}
      <button class="button button-secondary" type="submit">Đổi mật khẩu</button>
    </form>
  `, "account", view);
}

async function renderShopWorkspace(view) {
  const role = session.user.role;
  const shopsLoader = role === "EMPLOYEE" ? workspaceApi.assignedShops : workspaceApi.ownedShops;
  await loadPage(shopsLoader, (shops) => {
    workspaceShops = shops;
    if (!shops.length) {
      return `${pageHeading(role === "SHOP_OWNER" || role === "CUSTOMER" ? "Cửa hàng của tôi" : "Cửa hàng được phân công")}
        ${role === "SHOP_OWNER" || role === "CUSTOMER"
          ? `<form class="panel form-stack narrow-panel" data-form="shop-create"><h2>Tạo cửa hàng</h2>
              ${field("Tên cửa hàng", "name", { required: true, max: 150 })}
              ${field("Email cửa hàng", "email", { type: "email", max: 180 })}
              ${field("Số điện thoại", "phone", { max: 20 })}
              ${field("Địa chỉ", "address", { max: 300 })}
              <label class="field"><span>Mô tả</span><textarea name="description" maxlength="2000" rows="3"></textarea></label>
              <button class="button button-primary" type="submit">Tạo cửa hàng</button></form>`
          : emptyState("Tài khoản chưa được phân công vào cửa hàng nào.")}`;
    }
    const selected = shops.find((shop) => shop.id === workspaceShopId) || shops[0];
    workspaceShopId = selected.id;
    sessionStorage.setItem("marketplace.shopId", String(selected.id));
    return `${pageHeading(role === "SHOP_OWNER" ? "Quản lý cửa hàng" : "Khu vực nhân viên",
      "Dữ liệu giới hạn theo cửa hàng mà tài khoản được phép truy cập.")}
      <section class="workspace-select panel">
        ${selectField("Cửa hàng", "workspaceShop", shops.map((shop) => [shop.id, shop.name]), selected.id)}
        <span class="status-pill">${escapeHtml(statusText(selected.status))}</span>
      </section>
      <nav class="tab-nav" aria-label="Chức năng quản lý">
        ${[["products", "Sản phẩm"], ["orders", "Đơn hàng"],
          ...(role === "SHOP_OWNER" ? [["staff", "Nhân viên"], ["transactions", "Giao dịch"], ["details", "Thông tin cửa hàng"]] : []),
          ...(role === "SHOP_OWNER" || role === "EMPLOYEE"
            ? [["customers", "Khách hàng"], ["appointments", "Lịch hẹn"], ["notes", "Ghi chú"]] : [])]
          .map(([key, label]) => `<button type="button" class="${workspaceTab === key ? "active" : ""}" data-action="workspace-tab" data-tab="${key}">${label}</button>`).join("")}
      </nav>
      <section id="workspace-content" class="workspace-content"><div class="loading-state">Đang tải dữ liệu cửa hàng…</div></section>`;
  }, "workspace", view);
  if (!isCurrent(view) || session.user.role === "ADMIN") return;
  if (!document.querySelector("#workspace-content")) return;
  await renderWorkspaceTab(view);
}

async function renderWorkspaceTab(view) {
  const target = document.querySelector("#workspace-content");
  if (!target) return;
  try {
    const shopId = workspaceShopId;
    if (workspaceTab === "products") {
      const products = await workspaceApi.products(shopId, view.signal);
      if (!isCurrent(view)) return;
      const categories = session.user.role === "SHOP_OWNER"
        ? await catalogApi.categories(view.signal) : [];
      target.innerHTML = `${session.user.role === "SHOP_OWNER" ? productManagementForm(categories) : ""}
        ${listState(products, (product) => `<article class="manage-card panel">
          <div><a class="product-title" href="#/product/${product.id}">${escapeHtml(product.name)}</a>
            <p>${money(product.price)} · Tồn kho: ${product.stockQuantity}</p><span class="status-pill">${product.active ? "Đang bán" : "Đã ngừng bán"}</span></div>
          ${session.user.role === "SHOP_OWNER" ? `<div class="inline-actions"><button class="button button-secondary button-small" data-action="edit-product" data-product="${encodeURIComponent(JSON.stringify(product))}">Sửa</button>
            ${product.active ? `<button class="button button-danger-quiet button-small" data-action="deactivate-product" data-id="${product.id}">Ngừng bán</button>` : ""}
          </div>` : ""}
          </article>`, "Cửa hàng chưa có sản phẩm.")}
        `;
    } else if (workspaceTab === "orders") {
      const orders = session.user.role === "EMPLOYEE"
        ? await workspaceApi.assignedOrders(shopId, "", view.signal)
        : await workspaceApi.orders(shopId, "", view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = listState(orders, (order) => `<article class="order-card panel">
        <div class="order-head"><div><span class="eyebrow">Đơn cửa hàng #${order.id}</span><strong>${escapeHtml(statusText(order.status))}</strong></div>
          <strong>${money(order.totalAmount)}</strong></div>
        <p>Đơn tổng #${order.orderId} · Nhân viên xử lý: ${order.assignedStaffId ?? "Chưa phân công"}</p>
        <div class="order-lines">${(order.items || []).map((item) => `<div>${escapeHtml(item.productName)} × ${item.quantity}<span>${money(item.lineTotal)}</span></div>`).join("")}</div>
        ${session.user.role === "SHOP_OWNER" ? `<form class="inline-form assign-staff-form" data-form="assign-staff" data-order-id="${order.id}">
          ${field("Mã nhân viên", "staffId", { type: "number", min: 1 })}
          <button class="button button-secondary button-small" type="submit">Giao nhân viên</button>
        </form>` : ""}
        <div class="inline-actions">${nextStatusOptions(order.status).map((status) => `<button class="button button-secondary button-small" data-action="order-status" data-order-id="${order.id}" data-status="${status}">${statusText(status)}</button>`).join("")}</div>
      </article>`, "Chưa có đơn hàng của cửa hàng.");
    } else if (workspaceTab === "staff") {
      const staff = await workspaceApi.staff(shopId, view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = `<form class="panel inline-form staff-form" data-form="staff-add">
        ${field("Mã người dùng đã đăng ký", "userId", { type: "number", min: 1, required: true })}
        <button class="button button-primary" type="submit">Thêm nhân viên</button>
        <p class="form-note">Thêm nhân viên bằng mã tài khoản Marketplace đã đăng ký.</p>
      </form>${listState(staff, (member) => `<article class="manage-card panel">
        <div><strong>Người dùng #${member.userId}</strong><p>Vai trò: ${escapeHtml(roleNames[member.role] || member.role)}</p></div>
        <span class="status-pill">${member.active ? "Đang hoạt động" : "Đã ngừng"}</span>
      </article>`, "Cửa hàng chưa có nhân viên.")}`;
    } else if (workspaceTab === "customers") {
      const customers = await workspaceApi.customers(shopId, "", view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = listState(customers, (customer) => `<article class="manage-card panel">
        <div><strong>Khách hàng #${customer.customerId}</strong>
          <p>Trạng thái: ${escapeHtml(statusText(customer.status))} · Theo dõi từ ${dateText(customer.createdAt)}</p></div>
        <div class="inline-actions">
          <button class="button button-secondary button-small" data-action="customer-history" data-customer-id="${customer.customerId}">Lịch sử ghi chú</button>
          <button class="button button-quiet button-small" data-action="customer-status" data-customer-id="${customer.customerId}" data-status="${customer.status === "ACTIVE" ? "INACTIVE" : "ACTIVE"}">${customer.status === "ACTIVE" ? "Ngừng theo dõi" : "Theo dõi lại"}</button>
        </div>
        <form class="form-stack customer-note-form" data-form="customer-note">
          <input type="hidden" name="customerId" value="${customer.customerId}">
          ${field("Ghi chú chăm sóc", "note", { required: true, max: 2000 })}
          ${field("Lịch hẹn (không bắt buộc)", "appointmentAt", { type: "datetime-local" })}
          <button class="button button-secondary button-small" type="submit">Lưu ghi chú</button>
        </form>
      </article>`, "Chưa có khách hàng liên kết với cửa hàng.");
    } else if (workspaceTab === "appointments") {
      const from = new Date();
      const to = new Date(from.getTime() + 30 * 24 * 60 * 60 * 1000);
      const appointments = await workspaceApi.appointments(
        shopId, from.toISOString().slice(0, 19), to.toISOString().slice(0, 19), view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = listState(appointments, (appointment) => `<article class="manage-card panel">
        <div><strong>${dateText(appointment.appointmentAt)}</strong><p>Khách hàng #${appointment.customerId}</p>
          <p>${escapeHtml(appointment.note)}</p></div>
      </article>`, "Trong 30 ngày tới chưa có lịch hẹn.");
    } else if (workspaceTab === "notes") {
      const notes = session.user.role === "EMPLOYEE"
        ? await workspaceApi.assignedNotes(shopId, view.signal)
        : await workspaceApi.createdNotes(shopId, view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = listState(notes, (note) => `<article class="manage-card panel">
        <div><strong>Khách hàng #${note.customerId}</strong><p>${escapeHtml(note.note)}</p>
          <span class="muted">${note.appointmentAt ? `Lịch hẹn: ${dateText(note.appointmentAt)}` : `Ghi lúc: ${dateText(note.createdAt)}`}</span></div>
      </article>`, session.user.role === "EMPLOYEE"
        ? "Chưa có ghi chú nào được giao cho bạn."
        : "Bạn chưa ghi nhận hoạt động chăm sóc khách hàng.");
    } else if (workspaceTab === "details" && session.user.role === "SHOP_OWNER") {
      const shop = workspaceShops.find((item) => item.id === shopId);
      if (!shop) throw new Error("Không tìm thấy cửa hàng đang chọn.");
      target.innerHTML = `<form class="panel form-stack narrow-panel" data-form="shop-update" data-shop-id="${shop.id}">
        <h2>Thông tin cửa hàng</h2>
        ${field("Tên cửa hàng", "name", { value: shop.name, required: true, max: 150 })}
        ${field("Email cửa hàng", "email", { type: "email", value: shop.email || "", max: 180 })}
        ${field("Số điện thoại", "phone", { value: shop.phone || "", max: 20 })}
        ${field("Địa chỉ", "address", { value: shop.address || "", max: 300 })}
        ${field("Đường dẫn logo", "logoUrl", { value: shop.logoUrl || "", max: 255 })}
        <label class="field"><span>Mô tả</span><textarea name="description" maxlength="2000" rows="3">${escapeHtml(shop.description || "")}</textarea></label>
        <div class="inline-actions"><button class="button button-primary" type="submit">Lưu cửa hàng</button>
          ${shop.status === "ACTIVE"
            ? `<button class="button button-danger-quiet" type="button" data-action="deactivate-shop" data-shop-id="${shop.id}">Ngừng hoạt động cửa hàng</button>`
            : `<span class="form-note">Cửa hàng đang ngừng hoạt động; API chưa hỗ trợ kích hoạt lại.</span>`}</div>
      </form>`;
    } else {
      const transactions = await workspaceApi.transactions(shopId, view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = `${session.user.role === "SHOP_OWNER" ? `<form class="panel form-stack transaction-form" data-form="transaction-create">
        <h3>Tạo giao dịch chờ xử lý</h3>
        ${field("Mã đơn hàng cửa hàng (không bắt buộc)", "shopOrderId", { type: "number", min: 1 })}
        ${selectField("Loại giao dịch", "type", [["SALE", "Bán hàng"], ["REFUND", "Hoàn trả"], ["PAYOUT", "Chi trả"], ["FEE", "Phí"], ["ADJUSTMENT", "Điều chỉnh"]])}
        ${field("Số tiền", "amount", { type: "number", min: "0.01", step: "0.01", required: true })}
        ${field("Mã tham chiếu", "transactionReference", { max: 120 })}
        <p class="form-note">Backend chỉ ghi giao dịch ở trạng thái chờ; chưa có API xác nhận thanh toán/chi trả.</p>
        <button class="button button-primary" type="submit">Tạo giao dịch</button>
      </form>` : ""}
      ${listState(transactions, (transaction) => `<article class="manage-card panel">
        <div><strong>${escapeHtml(statusText(transaction.type))} · ${money(transaction.amount)}</strong>
          <p>${escapeHtml(transaction.transactionReference || "Không có mã tham chiếu")}</p></div>
        <span class="status-pill">${escapeHtml(statusText(transaction.status))}</span>
      </article>`, "Chưa có giao dịch.")}`;
    }
  } catch (error) {
    if (error.name !== "AbortError" && isCurrent(view)) target.innerHTML = errorPage(error, "workspace");
  }
}

function productManagementForm(categories, product = null) {
  const categoryOptions = [["", "Không chọn danh mục"], ...categories.map((category) => [category.id, category.name])];
  return `<form class="panel form-stack management-form" data-form="product-save">
    <h2>${product ? "Cập nhật sản phẩm" : "Thêm sản phẩm"}</h2>
    <input type="hidden" name="productId" value="${product?.id || ""}">
    ${field("Tên sản phẩm", "name", { value: product?.name || "", required: true, max: 180 })}
    <label class="field"><span>Mô tả</span><textarea name="description" maxlength="4000" rows="3">${escapeHtml(product?.description || "")}</textarea></label>
    <div class="form-grid">
      ${field("Giá bán", "price", { type: "number", value: product?.price || "", min: "0.01", step: "0.01", required: true })}
      ${field("Số lượng tồn kho", "stockQuantity", { type: "number", value: product?.stockQuantity ?? 0, min: 0, step: 1, required: true })}
      ${selectField("Danh mục", "categoryId", categoryOptions, product?.categoryId || "")}
      ${field("Đường dẫn ảnh", "imageUrl", { value: product?.imageUrl || "", max: 255 })}
    </div>
    <div class="inline-actions"><button class="button button-primary" type="submit">${product ? "Lưu thay đổi" : "Thêm sản phẩm"}</button>
      ${product ? `<button class="button button-quiet" type="button" data-action="cancel-edit-product">Hủy sửa</button>` : ""}
    </div></form>`;
}

function nextStatusOptions(current) {
  const transitions = {
    PENDING: ["CONFIRMED", "CANCELLED"],
    CONFIRMED: ["PROCESSING", "CANCELLED"],
    PROCESSING: ["SHIPPED", "CANCELLED"],
    SHIPPED: ["DELIVERED"],
    DELIVERED: [],
    CANCELLED: []
  };
  return transitions[current] || [];
}

function statusText(status) {
  return ({
    PENDING: "Chờ xử lý",
    CONFIRMED: "Đã xác nhận",
    PROCESSING: "Đang xử lý",
    SHIPPED: "Đang giao",
    DELIVERED: "Đã giao",
    CANCELLED: "Đã hủy",
    PARTIALLY_CANCELLED: "Đã hủy một phần",
    PAID: "Đã thanh toán",
    FAILED: "Thanh toán thất bại",
    ACTIVE: "Đang hoạt động",
    INACTIVE: "Đã ngừng hoạt động",
    SALE: "Bán hàng",
    REFUND: "Hoàn trả",
    PAYOUT: "Chi trả",
    FEE: "Phí",
    ADJUSTMENT: "Điều chỉnh",
  })[status] || status;
}

function paymentMethodText(method) {
  return ({
    COD: "Thanh toán khi nhận hàng",
    CARD: "Thẻ",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
    E_WALLET: "Ví điện tử"
  })[method] || method;
}

async function renderAdmin(view) {
  if (!["users", "locked", "shops", "orders", "payments", "categories"].includes(adminTab)) adminTab = "users";
  const tabs = [
    ["users", "Tài khoản"], ["locked", "Tài khoản bị khóa"], ["shops", "Cửa hàng"],
    ["orders", "Đơn hàng"], ["payments", "Thanh toán"], ["categories", "Danh mục"]
  ];
  main.innerHTML = `${pageHeading("Quản trị nền tảng", "Các danh sách bên dưới lấy từ API thật; Backend chưa cung cấp API thống kê.")}
    <nav class="tab-nav">${tabs.map(([key, label]) => `<button class="${adminTab === key ? "active" : ""}" data-action="workspace-tab" data-tab="${key}">${label}</button>`).join("")}</nav>
    <section id="admin-content" class="workspace-content"><div class="loading-state">Đang tải dữ liệu quản trị…</div></section>`;
  await renderAdminTab(view);
}

async function renderAdminTab(view) {
  const target = document.querySelector("#admin-content");
  if (!target) return;
  try {
    if (adminTab === "users") {
      const users = await workspaceApi.adminUsers("", "", view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = `<div class="panel filter-panel"><form class="filter-form" data-form="admin-user-filter">
        ${selectField("Vai trò", "role", [["", "Tất cả"], ["ADMIN", "Quản trị viên"], ["SHOP_OWNER", "Chủ cửa hàng"], ["EMPLOYEE", "Nhân viên"], ["CUSTOMER", "Khách hàng"]])}
        ${selectField("Tài khoản", "enabled", [["", "Tất cả"], ["true", "Đang hoạt động"], ["false", "Đã vô hiệu hóa"]])}
        <button class="button button-secondary" type="submit">Lọc</button>
      </form></div>${renderAdminUsers(users)}`;
    } else if (adminTab === "locked") {
      const users = await workspaceApi.lockedUsers(view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = listState(users, (user) => `<article class="manage-card panel">
        <div><strong>${escapeHtml(user.fullName)}</strong><p>${escapeHtml(user.email)} · ${escapeHtml(user.role)}</p>
          <span class="muted">Số lần sai: ${user.failedLoginAttempts ?? 0}</span></div>
        <button class="button button-primary button-small" data-action="unlock-user" data-user-id="${user.id}">Mở khóa</button>
      </article>`, "Hiện không có tài khoản nào bị khóa.");
    } else if (adminTab === "shops") {
      const shops = await workspaceApi.adminShops("ACTIVE", view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = `<form class="filter-form panel" data-form="admin-shop-filter">
        ${selectField("Trạng thái", "status", [["ACTIVE", "Đang hoạt động"], ["INACTIVE", "Đã ngừng hoạt động"]], "ACTIVE")}
        <button class="button button-secondary" type="submit">Lọc cửa hàng</button>
      </form>${listState(shops, (shop) => `<article class="manage-card panel"><div><strong>${escapeHtml(shop.name)}</strong>
        <p>${escapeHtml(shop.email || "Chưa có email")} · Chủ sở hữu #${shop.ownerId}</p></div><span class="status-pill">${escapeHtml(statusText(shop.status))}</span></article>`, "Không có cửa hàng ở trạng thái này.")}`;
    } else if (adminTab === "orders") {
      const orders = await workspaceApi.adminOrders("PENDING", view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = `<form class="filter-form panel" data-form="admin-order-filter">
        ${selectField("Trạng thái thanh toán", "paymentStatus",
          [["PENDING", "Chờ thanh toán"], ["PAID", "Đã thanh toán"], ["FAILED", "Thất bại"], ["CANCELLED", "Đã hủy"]], "PENDING")}
        <button class="button button-secondary" type="submit">Lọc đơn</button>
      </form>${listState(orders, (order) => `<article class="order-card panel">
        <div class="order-head"><strong>${escapeHtml(order.orderNumber)}</strong><span class="status-pill">${escapeHtml(statusText(order.status))}</span></div>
        <div class="order-summary"><span>Khách #${order.customerId}</span><span>${dateText(order.createdAt)}</span><strong>${money(order.totalAmount)}</strong></div>
      </article>`, "Không có đơn hàng ở trạng thái thanh toán này.")}`;
    } else if (adminTab === "payments") {
      const payments = await workspaceApi.adminPayments("PENDING", view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = `<form class="filter-form panel" data-form="admin-payment-filter">
        ${selectField("Trạng thái", "status", [["PENDING", "Đang chờ"], ["PAID", "Đã thanh toán"], ["FAILED", "Thất bại"], ["CANCELLED", "Đã hủy"]], "PENDING")}
        <button class="button button-secondary" type="submit">Lọc thanh toán</button>
      </form>${listState(payments, (payment) => `<article class="manage-card panel">
        <div><strong>${money(payment.amount)} · ${escapeHtml(paymentMethodText(payment.method))}</strong><p>Đơn #${payment.orderId} · ${dateText(payment.createdAt)}</p></div>
        <span class="status-pill">${escapeHtml(statusText(payment.status))}</span>
      </article>`, "Không có thanh toán ở trạng thái này.")}`;
    } else {
      const categories = await loadAdminCategories(view.signal);
      if (!isCurrent(view)) return;
      target.innerHTML = `${categoryManagementForm(null, categories)}
        ${listState(categories, (category) => `<article class="manage-card panel">
          <div><strong>${"— ".repeat(category.depth)}${escapeHtml(category.name)}</strong><p>${escapeHtml(category.slug)}</p></div>
          <div class="inline-actions"><button class="button button-secondary button-small" data-action="edit-category" data-category="${encodeURIComponent(JSON.stringify(category))}">Sửa</button>
            <button class="button button-danger-quiet button-small" data-action="deactivate-category" data-id="${category.id}">Ngừng hoạt động</button></div>
        </article>`, "Chưa có danh mục gốc.")}`;
    }
  } catch (error) {
    if (error.name !== "AbortError" && isCurrent(view)) target.innerHTML = errorPage(error, "workspace");
  }
}

async function loadAdminCategories(signal) {
  const roots = await workspaceApi.rootCategories(signal);
  const result = [];
  const visited = new Set();
  async function addChildren(category, depth) {
    if (visited.has(category.id)) return;
    visited.add(category.id);
    result.push({ ...category, depth });
    const children = await workspaceApi.adminCategoryChildren(category.id, signal);
    for (const child of children) await addChildren(child, depth + 1);
  }
  for (const root of roots) await addChildren(root, 0);
  return result;
}

function renderAdminUsers(users) {
  return listState(users, (user) => `<article class="manage-card panel">
    <div><strong>${escapeHtml(user.fullName)}</strong><p>#${user.id} · ${escapeHtml(user.email)} · ${escapeHtml(roleNames[user.role] || user.role)}</p></div>
    <span class="status-pill">${user.locked ? "Đã khóa" : user.enabled ? "Đang hoạt động" : "Đã vô hiệu hóa"}</span>
  </article>`, "Không tìm thấy tài khoản.");
}

function categoryManagementForm(category = null, categories = []) {
  const parentChoices = [["", "Không có danh mục cha"],
    ...categories.filter((item) => item.id !== category?.id)
      .map((item) => [item.id, `${"— ".repeat(item.depth)}${item.name}`])];
  return `<form class="panel form-stack management-form" data-form="category-save">
    <h2>${category ? "Cập nhật danh mục" : "Thêm danh mục gốc"}</h2>
    <input type="hidden" name="categoryId" value="${category?.id || ""}">
    ${field("Tên danh mục", "name", { value: category?.name || "", required: true, max: 120 })}
    ${field("Đường dẫn", "slug", { value: category?.slug || "", required: true, max: 160, placeholder: "vi-du-danh-muc" })}
    ${selectField("Danh mục cha", "parentCategoryId", parentChoices, category?.parentCategoryId || "")}
    ${field("Đường dẫn ảnh", "imageUrl", { value: category?.imageUrl || "", max: 255 })}
    <label class="field"><span>Mô tả</span><textarea name="description" maxlength="1000" rows="2">${escapeHtml(category?.description || "")}</textarea></label>
    <div class="inline-actions"><button class="button button-primary" type="submit">${category ? "Lưu danh mục" : "Tạo danh mục"}</button>
      ${category ? `<button class="button button-quiet" type="button" data-action="cancel-edit-category">Hủy sửa</button>` : ""}
    </div></form>`;
}

async function handleSubmit(event) {
  const form = event.target.closest("form[data-form]");
  if (!form) return;
  event.preventDefault();
  if (!form.reportValidity()) return;
  const values = Object.fromEntries(new FormData(form).entries());
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  try {
    switch (form.dataset.form) {
      case "login": {
        const result = await authApi.login({ email: values.email, password: values.password });
        saveSession(result);
        session = readSession();
        sessionStorage.removeItem("marketplace.shopId");
        workspaceShopId = null;
        workspaceShops = [];
        workspaceTab = "products";
        adminTab = "users";
        notify("Đăng nhập thành công.");
        location.hash = session.user.role === "CUSTOMER" ? "#/" : "#/workspace";
        break;
      }
      case "register": {
        if (values.password !== values.confirmPassword) throw new Error("Mật khẩu xác nhận chưa khớp.");
        await authApi.register({
          fullName: values.fullName.trim(),
          email: values.email.trim(),
          password: values.password,
          phone: values.phone.trim() || null
        });
        notify("Tạo tài khoản thành công. Vui lòng đăng nhập.");
        location.hash = "#/login";
        break;
      }
      case "password-reset-request":
        await authApi.requestPasswordReset({ email: values.email.trim() });
        notify("Nếu email phù hợp, hướng dẫn khôi phục sẽ được gửi. Hệ thống không xác nhận trạng thái tài khoản.", "info");
        break;
      case "password-reset":
        if (!form.dataset.token) throw new Error("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        if (values.newPassword !== values.confirmPassword) throw new Error("Mật khẩu xác nhận chưa khớp.");
        await authApi.resetPassword({
          token: form.dataset.token,
          newPassword: values.newPassword
        });
        notify("Đã thay đổi mật khẩu. Vui lòng đăng nhập.");
        location.hash = "#/login";
        break;
      case "header-search":
      case "home-search": {
        const keyword = values.name?.trim();
        location.hash = keyword ? `#/products?name=${encodeURIComponent(keyword)}` : "#/products";
        break;
      }
      case "shop-search":
        await searchShops(values.name.trim());
        break;
      case "product-search":
        await searchProducts(values);
        break;
      case "add-cart": {
        await cartApi.add({ productId: Number(values.productId), quantity: Number(values.quantity || 1) });
        notify("Đã thêm sản phẩm vào giỏ hàng.");
        if (routeInfo().path === "/cart") await renderRoute();
        break;
      }
      case "cart-update":
        await cartApi.update(Number(form.dataset.itemId), { quantity: Number(values.quantity) });
        notify("Đã cập nhật số lượng.");
        await renderRoute();
        break;
      case "profile":
        await userApi.updateProfile({
          fullName: values.fullName.trim(),
          phone: values.phone.trim() || null,
          avatarUrl: values.avatarUrl.trim() || null
        });
        notify("Đã cập nhật hồ sơ.");
        await renderRoute();
        break;
      case "password-change":
        if (values.newPassword !== values.confirmPassword) throw new Error("Mật khẩu xác nhận chưa khớp.");
        await userApi.changePassword({
          currentPassword: values.currentPassword,
          newPassword: values.newPassword
        });
        clearSession();
        session = null;
        sessionStorage.removeItem("marketplace.shopId");
        workspaceShopId = null;
        workspaceShops = [];
        setHeader();
        notify("Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
        location.hash = "#/login";
        break;
      case "checkout": {
        const order = await orderApi.checkout({
          shippingAddress: values.shippingAddress.trim(),
          recipientPhone: values.recipientPhone.trim(),
          customerNote: values.customerNote.trim() || null
        });
        form.closest("dialog")?.close();
        notify(`Đặt hàng thành công · ${order.orderNumber}`);
        location.hash = "#/orders";
        break;
      }
      case "payment-create": {
        const payment = await orderApi.createPayment(
          Number(form.dataset.orderId), { method: values.method });
        form.closest("dialog")?.close();
        notify(`Đã tạo yêu cầu thanh toán · ${statusText(payment.status)}`, "info");
        await renderRoute();
        break;
      }
      case "shop-create":
        await workspaceApi.createShop({
          name: values.name.trim(), email: values.email.trim() || null,
          phone: values.phone.trim() || null, address: values.address.trim() || null,
          description: values.description.trim() || null, logoUrl: null
        });
        notify("Đã tạo cửa hàng.");
        if (session.user.role === "CUSTOMER") {
          clearSession();
          session = null;
          sessionStorage.removeItem("marketplace.shopId");
          workspaceShopId = null;
          workspaceShops = [];
          setHeader();
          notify("Cửa hàng đã tạo. Đăng nhập lại để làm mới quyền chủ cửa hàng.", "info");
          location.hash = "#/login";
        } else {
          await renderRoute();
        }
        break;
      case "shop-update":
        await workspaceApi.updateShop(Number(form.dataset.shopId), {
          name: values.name.trim(),
          email: values.email.trim() || null,
          phone: values.phone.trim() || null,
          address: values.address.trim() || null,
          logoUrl: values.logoUrl.trim() || null,
          description: values.description.trim() || null
        });
        notify("Đã cập nhật cửa hàng.");
        await renderRoute();
        break;
      case "product-save": {
        const data = {
          name: values.name.trim(), description: values.description.trim() || null,
          price: Number(values.price), stockQuantity: Number(values.stockQuantity),
          imageUrl: values.imageUrl.trim() || null,
          categoryId: values.categoryId ? Number(values.categoryId) : null
        };
        if (values.productId) await workspaceApi.updateProduct(Number(values.productId), data);
        else await workspaceApi.createProduct(workspaceShopId, data);
        editingProduct = null;
        notify("Đã lưu sản phẩm.");
        await renderRoute();
        break;
      }
      case "staff-add":
        await workspaceApi.addStaff(workspaceShopId, Number(values.userId));
        notify("Đã thêm nhân viên vào cửa hàng.");
        await renderRoute();
        break;
      case "customer-note":
        await workspaceApi.createCustomerNote(workspaceShopId, {
          customerId: Number(values.customerId),
          note: values.note.trim(),
          appointmentAt: values.appointmentAt ? values.appointmentAt : null
        });
        notify("Đã lưu ghi chú chăm sóc khách hàng.");
        await renderRoute();
        break;
      case "assign-staff":
        if (!values.staffId) throw new Error("Nhập mã nhân viên cần giao đơn.");
        await workspaceApi.assignStaff(
          workspaceShopId, Number(form.dataset.orderId), Number(values.staffId));
        notify("Đã giao đơn cho nhân viên.");
        await renderRoute();
        break;
      case "transaction-create":
        await workspaceApi.createTransaction(workspaceShopId, {
            shopOrderId: values.shopOrderId ? Number(values.shopOrderId) : null,
            type: values.type,
            amount: Number(values.amount),
            transactionReference: values.transactionReference.trim() || null
        });
        notify("Đã tạo giao dịch ở trạng thái chờ.");
        await renderRoute();
        break;
      case "review":
        await catalogApi.addReview(Number(form.dataset.productId), {
          rating: Number(values.rating),
          comment: values.comment.trim() || null
        });
        notify("Đã gửi đánh giá.");
        await renderRoute();
        break;
      case "admin-user-filter": {
        const users = await workspaceApi.adminUsers(values.role, values.enabled);
        document.querySelector("#admin-content").innerHTML = form.outerHTML + renderAdminUsers(users);
        break;
      }
      case "admin-shop-filter": {
        const shops = await workspaceApi.adminShops(values.status);
        document.querySelector("#admin-content").innerHTML = form.outerHTML + listState(
          shops,
          (shop) => `<article class="manage-card panel"><div><strong>${escapeHtml(shop.name)}</strong><p>${escapeHtml(shop.email || "Chưa có email")} · Chủ sở hữu #${shop.ownerId}</p></div><span class="status-pill">${escapeHtml(shop.status)}</span></article>`,
          "Không có cửa hàng ở trạng thái này."
        );
        break;
      }
      case "admin-order-filter": {
        const orders = await workspaceApi.adminOrders(values.paymentStatus);
        document.querySelector("#admin-content").innerHTML = form.outerHTML + listState(
          orders,
          (order) => `<article class="order-card panel"><div class="order-head"><strong>${escapeHtml(order.orderNumber)}</strong><span class="status-pill">${escapeHtml(order.status)}</span></div><div class="order-summary"><span>Khách #${order.customerId}</span><span>${dateText(order.createdAt)}</span><strong>${money(order.totalAmount)}</strong></div></article>`,
          "Không có đơn hàng ở trạng thái này."
        );
        break;
      }
      case "admin-payment-filter": {
        const payments = await workspaceApi.adminPayments(values.status);
        document.querySelector("#admin-content").innerHTML = form.outerHTML + listState(
          payments,
          (payment) => `<article class="manage-card panel"><div><strong>${money(payment.amount)} · ${escapeHtml(payment.method)}</strong><p>Đơn #${payment.orderId} · ${dateText(payment.createdAt)}</p></div><span class="status-pill">${escapeHtml(payment.status)}</span></article>`,
          "Không có thanh toán ở trạng thái này."
        );
        break;
      }
      case "category-save": {
        const data = {
          name: values.name.trim(), slug: values.slug.trim(),
          description: values.description.trim() || null,
          imageUrl: values.imageUrl.trim() || null,
          parentCategoryId: values.parentCategoryId ? Number(values.parentCategoryId) : null
        };
        if (values.categoryId) await workspaceApi.updateCategory(Number(values.categoryId), data);
        else await workspaceApi.createCategory(data);
        editingCategory = null;
        notify("Đã lưu danh mục.");
        await renderRoute();
        break;
      }
      default:
        break;
    }
  } catch (error) {
    if (error.name !== "AbortError") notify(error.message, "error");
  } finally {
    if (submitButton?.isConnected) submitButton.disabled = false;
  }
}

async function handleClick(event) {
  const control = event.target.closest("[data-action]");
  if (!control) return;
  if (control.closest(".error-panel")) {
    await renderRoute();
    return;
  }
  const { action } = control.dataset;
  try {
    if (action === "logout") {
      activeController?.abort();
      clearSession();
      session = null;
      sessionStorage.removeItem("marketplace.shopId");
      workspaceShopId = null;
      workspaceShops = [];
      workspaceTab = "products";
      adminTab = "users";
      notify("Đã đăng xuất.");
      location.hash = "#/";
    } else if (action === "toggle-password") {
      const input = control.closest(".password-field").querySelector("input");
      input.type = input.type === "password" ? "text" : "password";
      control.textContent = input.type === "password" ? "Hiện" : "Ẩn";
    } else if (action === "close-dialog") {
      control.closest("dialog")?.close();
    } else if (action === "add-cart") {
      if (!session) {
        notify("Đăng nhập để thêm sản phẩm vào giỏ.", "error");
        location.hash = "#/login";
      } else {
        await cartApi.add({ productId: Number(control.dataset.productId), quantity: 1 });
        notify("Đã thêm sản phẩm vào giỏ hàng.");
      }
    } else if (action === "remove-cart") {
      if (window.confirm("Bạn có chắc muốn xóa sản phẩm này khỏi giỏ?")) {
        await cartApi.remove(Number(control.dataset.itemId));
        notify("Đã xóa sản phẩm khỏi giỏ.");
        await renderRoute();
      }
    } else if (action === "clear-cart") {
      if (window.confirm("Xóa toàn bộ sản phẩm trong giỏ hàng?")) {
        await cartApi.clear();
        notify("Đã làm trống giỏ hàng.");
        await renderRoute();
      }
    } else if (action === "product-page") {
      await searchProducts({ ...productFilters, page: Number(control.dataset.page) });
    } else if (action === "checkout") {
      showCheckout();
    } else if (action === "cancel-order") {
      if (window.confirm("Bạn có chắc muốn gửi yêu cầu hủy đơn hàng này?")) {
        await orderApi.cancel(Number(control.dataset.orderId));
        notify("Đã hủy đơn hàng.");
        await renderRoute();
      }
    } else if (action === "payment") {
      const orderId = Number(control.dataset.orderId);
      let payment;
      try {
        payment = await orderApi.payment(orderId);
      } catch (error) {
        if (error.status !== 404) throw error;
        showPayment(orderId);
        return;
      }
      notify(`Thanh toán: ${payment.status} · ${money(payment.amount)}`, "info");
      await renderRoute();
    } else if (action === "workspace-tab") {
      if (session.user.role === "ADMIN") {
        adminTab = control.dataset.tab;
        await renderAdmin(newView());
      }
      else {
        workspaceTab = control.dataset.tab;
        const view = newView();
        await renderShopWorkspace(view);
      }
    } else if (action === "workspace-retry") {
      await renderRoute();
    } else if (action === "order-status") {
      const status = control.dataset.status;
      if (status === "CANCELLED" && !window.confirm("Xác nhận hủy đơn hàng của cửa hàng?")) return;
      await workspaceApi.updateOrder(workspaceShopId, Number(control.dataset.orderId), status);
      notify("Đã cập nhật trạng thái đơn.");
      await renderRoute();
    } else if (action === "customer-status") {
      await workspaceApi.updateCustomer(
        workspaceShopId, Number(control.dataset.customerId), control.dataset.status);
      notify("Đã cập nhật trạng thái khách hàng.");
      await renderRoute();
    } else if (action === "customer-history") {
      const history = await workspaceApi.customerNotes(
        workspaceShopId, Number(control.dataset.customerId));
      const dialog = document.createElement("dialog");
      dialog.className = "checkout-dialog";
      dialog.innerHTML = `<div class="dialog-heading"><h2>Lịch sử chăm sóc · Khách hàng #${Number(control.dataset.customerId)}</h2>
        <button class="icon-button" type="button" data-action="close-dialog" aria-label="Đóng">×</button></div>
        ${listState(history, (note) => `<article class="manage-card panel">
          <p>${escapeHtml(note.note)}</p><span class="muted">${note.appointmentAt
            ? `Lịch hẹn: ${dateText(note.appointmentAt)}` : `Ghi lúc: ${dateText(note.createdAt)}`}</span>
        </article>`, "Chưa có ghi chú chăm sóc.")}`;
      document.body.append(dialog);
      dialog.addEventListener("close", () => dialog.remove(), { once: true });
      dialog.showModal();
    } else if (action === "deactivate-product") {
      if (window.confirm("Ngừng kinh doanh sản phẩm này?")) {
        await workspaceApi.deactivateProduct(Number(control.dataset.id));
        notify("Đã ngừng kinh doanh sản phẩm.");
        await renderRoute();
      }
    } else if (action === "deactivate-shop") {
      if (window.confirm("Ngừng hoạt động cửa hàng này? Khách hàng sẽ không còn thấy cửa hàng trong danh mục công khai.")) {
        await workspaceApi.deactivateShop(Number(control.dataset.shopId));
        workspaceShopId = null;
        sessionStorage.removeItem("marketplace.shopId");
        notify("Đã ngừng hoạt động cửa hàng.");
        await renderRoute();
      }
    } else if (action === "edit-product") {
      editingProduct = JSON.parse(decodeURIComponent(control.dataset.product));
      await renderRoute();
      const categories = await catalogApi.categories();
      const target = document.querySelector("#workspace-content");
      if (!target) return;
      const products = await workspaceApi.products(workspaceShopId);
      target.innerHTML = `${productManagementForm(categories, editingProduct)}
        ${listState(products, (product) => `<article class="manage-card panel"><div><a class="product-title" href="#/product/${product.id}">${escapeHtml(product.name)}</a><p>${money(product.price)} · Tồn kho: ${product.stockQuantity}</p></div><button class="button button-secondary button-small" data-action="edit-product" data-product="${encodeURIComponent(JSON.stringify(product))}">Sửa</button></article>`, "Cửa hàng chưa có sản phẩm.")}`;
    } else if (action === "cancel-edit-product") {
      editingProduct = null;
      await renderRoute();
    } else if (action === "unlock-user") {
      if (window.confirm("Mở khóa tài khoản này và đặt lại số lần đăng nhập sai?")) {
        await workspaceApi.unlockUser(Number(control.dataset.userId));
        notify("Đã mở khóa tài khoản.");
        await renderRoute();
      }
    } else if (action === "edit-category") {
      editingCategory = JSON.parse(decodeURIComponent(control.dataset.category));
      const target = document.querySelector("#admin-content");
      const categories = await loadAdminCategories();
      target.innerHTML = `${categoryManagementForm(editingCategory, categories)}
        ${listState(categories, (category) => `<article class="manage-card panel"><div><strong>${escapeHtml(category.name)}</strong><p>${escapeHtml(category.slug)}</p></div><button class="button button-secondary button-small" data-action="edit-category" data-category="${encodeURIComponent(JSON.stringify(category))}">Sửa</button></article>`, "Chưa có danh mục gốc.")}`;
    } else if (action === "cancel-edit-category") {
      editingCategory = null;
      await renderRoute();
    } else if (action === "deactivate-category") {
      if (window.confirm("Ngừng hoạt động danh mục này?")) {
        await workspaceApi.deactivateCategory(Number(control.dataset.id));
        notify("Đã ngừng hoạt động danh mục.");
        await renderRoute();
      }
    } else if (action === "shops") {
      location.hash = "#/shops";
    } else if (action === "products") {
      location.hash = "#/products";
    } else if (action === "product") {
      location.hash = `#/product/${control.dataset.id}`;
    } else if (["cart", "orders", "account", "product", "shops"].includes(action)) {
      await renderRoute();
    }
  } catch (error) {
    if (error.name !== "AbortError") notify(error.message, "error");
  }
}

function showCheckout() {
  const dialog = document.createElement("dialog");
  dialog.className = "checkout-dialog";
  dialog.innerHTML = `<form class="form-stack" data-form="checkout" method="dialog">
    <div class="dialog-heading"><h2>Thông tin nhận hàng</h2>
      <button class="icon-button" type="button" data-action="close-dialog" aria-label="Đóng">×</button></div>
    ${field("Địa chỉ giao hàng", "shippingAddress", { required: true, max: 300, autocomplete: "street-address" })}
    ${field("Số điện thoại người nhận", "recipientPhone", { required: true, max: 20, autocomplete: "tel" })}
    <label class="field"><span>Ghi chú</span><textarea name="customerNote" maxlength="1000" rows="3"></textarea></label>
    <p class="form-note">Giá, tồn kho và tổng tiền sẽ được Backend kiểm tra lại khi đặt hàng.</p>
    <button class="button button-primary" type="submit">Xác nhận đặt hàng</button>
  </form>`;
  dialog.addEventListener("close", () => dialog.remove());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });
  document.body.append(dialog);
  dialog.showModal();
}

function showPayment(orderId) {
  const dialog = document.createElement("dialog");
  dialog.className = "checkout-dialog";
  dialog.innerHTML = `<form class="form-stack" data-form="payment-create" data-order-id="${orderId}">
    <div class="dialog-heading"><h2>Chọn phương thức thanh toán</h2>
      <button class="icon-button" type="button" data-action="close-dialog" aria-label="Đóng">×</button></div>
    ${selectField("Phương thức", "method", [
      ["COD", "Thanh toán khi nhận hàng"],
      ["CARD", "Thẻ"],
      ["BANK_TRANSFER", "Chuyển khoản ngân hàng"],
      ["E_WALLET", "Ví điện tử"]
    ])}
    <p class="form-note">API hiện chỉ tạo yêu cầu thanh toán chờ xử lý; chưa kết nối cổng thanh toán thật.</p>
    <button class="button button-primary" type="submit">Tạo yêu cầu thanh toán</button>
  </form>`;
  dialog.addEventListener("close", () => dialog.remove());
  document.body.append(dialog);
  dialog.showModal();
}

document.addEventListener("submit", handleSubmit);
document.addEventListener("click", handleClick);
document.addEventListener("error", (event) => {
  const image = event.target.closest?.("img");
  if (!image) return;
  const placeholder = document.createElement("span");
  placeholder.className = "image-placeholder";
  placeholder.textContent = "Không tải được ảnh";
  image.replaceWith(placeholder);
}, true);
document.addEventListener("change", (event) => {
  if (event.target.name === "workspaceShop") {
    workspaceShopId = Number(event.target.value);
    sessionStorage.setItem("marketplace.shopId", String(workspaceShopId));
    renderRoute();
  }
});
window.addEventListener("hashchange", renderRoute);
window.addEventListener("marketplace:unauthorized", () => {
  session = null;
  sessionStorage.removeItem("marketplace.shopId");
  workspaceShopId = null;
  workspaceShops = [];
  notify("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", "error");
  if (!["/login", "/register"].includes(routeInfo().path)) location.hash = "#/login";
  setHeader();
});
document.querySelector(".mobile-menu-button").addEventListener("click", (event) => {
  const expanded = event.currentTarget.getAttribute("aria-expanded") === "true";
  event.currentTarget.setAttribute("aria-expanded", String(!expanded));
  nav.classList.toggle("is-open", !expanded);
});

if (!location.hash) location.hash = "#/";
renderRoute();
