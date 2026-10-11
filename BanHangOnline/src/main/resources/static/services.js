import { api } from "./api.js";

const call = (path, options, signal) => api(path, { ...options, signal });
const query = (values) => new URLSearchParams(
  Object.entries(values).filter(([, value]) => value !== undefined && value !== null && value !== "")
).toString();

export const authApi = {
  login: (data) => call("/api/auth/login", { method: "POST", body: data, authenticated: false }),
  register: (data) => call("/api/users/register", { method: "POST", body: data, authenticated: false }),
  requestPasswordReset: (data) => call(
    "/api/auth/password-reset-requests", { method: "POST", body: data, authenticated: false }),
  resetPassword: (data) => call(
    "/api/auth/password-resets", { method: "POST", body: data, authenticated: false })
};

export const userApi = {
  profile: (signal) => call("/api/users/me", {}, signal),
  updateProfile: (data, signal) => call("/api/users/me", { method: "PUT", body: data }, signal),
  changePassword: (data, signal) => call("/api/users/me/password", { method: "PUT", body: data }, signal)
};

export const catalogApi = {
  categories: (signal) => call("/api/categories", {}, signal),
  category: (slug, signal) => call(`/api/categories/slug/${encodeURIComponent(slug)}`, {}, signal),
  categoryProducts: (id, signal) => call(`/api/categories/${id}/products`, {}, signal),
  searchCategories: (name, signal) => call(`/api/categories/search?${query({ name })}`, {}, signal),
  searchProducts: (name, signal) => call(`/api/products?${query({ name })}`, {}, signal),
  productsByPrice: (minimum, maximum, signal) =>
    call(`/api/products/by-price?${query({ minimum, maximum })}`, {}, signal),
  searchCatalog: (filters, signal) => call(`/api/products/search?${query(filters)}`, {}, signal),
  product: (id, signal) => call(`/api/products/${id}`, {}, signal),
  shop: (id, signal) => call(`/api/shops/${id}`, {}, signal),
  searchShops: (name, signal) => call(`/api/shops?${query({ name })}`, {}, signal),
  shopProducts: (id, signal) => call(`/api/shops/${id}/products`, {}, signal),
  reviews: (id, rating, signal) =>
    call(`/api/products/${id}/reviews?${query({ rating })}`, {}, signal),
  addReview: (id, data, signal) =>
    call(`/api/products/${id}/reviews`, { method: "POST", body: data }, signal)
};

export const cartApi = {
  list: (signal) => call("/api/cart", {}, signal),
  add: (data, signal) => call("/api/cart/items", { method: "POST", body: data }, signal),
  update: (id, data, signal) => call(`/api/cart/items/${id}`, { method: "PUT", body: data }, signal),
  remove: (id, signal) => call(`/api/cart/items/${id}`, { method: "DELETE" }, signal),
  clear: (signal) => call("/api/cart", { method: "DELETE" }, signal)
};

export const orderApi = {
  list: (status, signal) => call(`/api/orders?${query({ status })}`, {}, signal),
  get: (id, signal) => call(`/api/orders/${id}`, {}, signal),
  checkout: (data, signal) => call("/api/orders/checkout", { method: "POST", body: data }, signal),
  cancel: (id, signal) => call(`/api/orders/${id}/cancel`, { method: "POST" }, signal),
  payment: (id, signal) => call(`/api/orders/${id}/payment`, {}, signal),
  createPayment: (id, data, signal) =>
    call(`/api/orders/${id}/payment`, { method: "POST", body: data }, signal)
};

export const workspaceApi = {
  ownedShops: (signal) => call("/api/shops/mine", {}, signal),
  assignedShops: (signal) => call("/api/shops/assigned-to-me", {}, signal),
  createShop: (data, signal) => call("/api/shops", { method: "POST", body: data }, signal),
  updateShop: (id, data, signal) => call(`/api/shops/${id}`, { method: "PUT", body: data }, signal),
  deactivateShop: (id, signal) => call(`/api/shops/${id}`, { method: "DELETE" }, signal),
  products: (id, signal) => call(`/api/shops/${id}/manage/products`, {}, signal),
  createProduct: (shopId, data, signal) =>
    call(`/api/shops/${shopId}/products`, { method: "POST", body: data }, signal),
  updateProduct: (id, data, signal) => call(`/api/products/${id}`, { method: "PUT", body: data }, signal),
  deactivateProduct: (id, signal) => call(`/api/products/${id}`, { method: "DELETE" }, signal),
  orders: (shopId, status, signal) =>
    call(`/api/shops/${shopId}/orders?${query({ status })}`, {}, signal),
  assignedOrders: (shopId, status, signal) =>
    call(`/api/shops/${shopId}/orders/assigned-to-me?${query({ status })}`, {}, signal),
  updateOrder: (shopId, orderId, status, signal) =>
    call(`/api/shops/${shopId}/orders/${orderId}/status`, { method: "PATCH", body: { status } }, signal),
  assignStaff: (shopId, orderId, staffId, signal) =>
    call(`/api/shops/${shopId}/orders/${orderId}/staff`, { method: "PUT", body: { staffId } }, signal),
  staff: (shopId, signal) => call(`/api/shops/${shopId}/staff`, {}, signal),
  addStaff: (shopId, userId, signal) =>
    call(`/api/shops/${shopId}/staff`, { method: "POST", body: { userId } }, signal),
  transactions: (shopId, signal) => call(`/api/shops/${shopId}/transactions`, {}, signal),
  createTransaction: (shopId, data, signal) =>
    call(`/api/shops/${shopId}/transactions`, { method: "POST", body: data }, signal),
  createCategory: (data, signal) => call("/api/categories", { method: "POST", body: data }, signal),
  updateCategory: (id, data, signal) =>
    call(`/api/categories/${id}`, { method: "PUT", body: data }, signal),
  deactivateCategory: (id, signal) => call(`/api/categories/${id}`, { method: "DELETE" }, signal),
  adminUsers: (role, enabled, signal) =>
    call(`/api/admin/users?${query({ role, enabled })}`, {}, signal),
  lockedUsers: (signal) => call("/api/admin/users/locked", {}, signal),
  unlockUser: (id, signal) => call(`/api/admin/users/${id}/unlock`, { method: "PUT" }, signal),
  adminShops: (status, signal) => call(`/api/admin/shops?${query({ status })}`, {}, signal),
  adminOrders: (paymentStatus, signal) =>
    call(`/api/admin/orders?${query({ paymentStatus })}`, {}, signal),
  adminPayments: (status, signal) =>
    call(`/api/admin/payments?${query({ status })}`, {}, signal),
  customers: (shopId, status, signal) =>
    call(`/api/shops/${shopId}/customers?${query({ status })}`, {}, signal),
  addCustomer: (shopId, customerId, signal) =>
    call(`/api/shops/${shopId}/customers`, { method: "POST", body: { customerId } }, signal),
  updateCustomer: (shopId, customerId, status, signal) =>
    call(`/api/shops/${shopId}/customers/${customerId}`, { method: "PUT", body: { status } }, signal),
  customerNotes: (shopId, customerId, signal) =>
    call(`/api/shops/${shopId}/customers/${customerId}/notes`, {}, signal),
  createdNotes: (shopId, signal) =>
    call(`/api/shops/${shopId}/customer-notes/created-by-me`, {}, signal),
  assignedNotes: (shopId, signal) =>
    call(`/api/shops/${shopId}/customer-notes/assigned-to-me`, {}, signal),
  appointments: (shopId, from, to, signal) =>
    call(`/api/shops/${shopId}/appointments?${query({ from, to })}`, {}, signal),
  createCustomerNote: (shopId, data, signal) =>
    call(`/api/shops/${shopId}/customer-notes`, { method: "POST", body: data }, signal),
  rootCategories: (signal) => call("/api/admin/categories/roots", {}, signal),
  adminCategoryChildren: (id, signal) => call(`/api/admin/categories/${id}/children`, {}, signal)
};
