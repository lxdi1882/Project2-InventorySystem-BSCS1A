//app.js — shared utilities for all pages

const API = 'http://localhost:8080/api';

//  Session helpers 
const Session = {
  set(user) { sessionStorage.setItem('user', JSON.stringify(user)); },
  get()     { const u = sessionStorage.getItem('user'); return u ? JSON.parse(u) : null; },
  clear()   { sessionStorage.removeItem('user'); },
};

//  Auth guard (call on every protected page) 
function requireAuth() {
  const user = Session.get();
  if (!user) { window.location.href = '/pages/login.html'; return null; }
  return user;
}

//  Inject sidebar + topbar into a page
function initShell(activePage) {
  const user = requireAuth();
  if (!user) return;

  // Topbar username
  const nameEl = document.getElementById('topbar-name');
  if (nameEl) nameEl.textContent = user.full_name;

  // Sidebar active link
  document.querySelectorAll('.sidebar a').forEach(a => {
    if (a.dataset.page === activePage) a.classList.add('active');
  });

  // Logout button
  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      if (!confirm('Are you sure you want to log out?')) return;
      await fetch(`${API}/auth/logout`, { method: 'POST' }).catch(() => {});
      Session.clear();
      window.location.href = '/pages/login.html';
    });
  }
}

//Toast notifications
function toast(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

//API fetch helper
async function api(endpoint, options = {}) {
  const res = await fetch(`${API}${endpoint}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'Request failed');
  return data;
}

//Status badge HTML
function statusBadge(status) {
  const cls = {
    'In Stock':    'badge-in-stock',
    'Low Stock':   'badge-low-stock',
    'Out of Stock':'badge-out-stock',
  }[status] || '';
  return `<span class="badge ${cls}">${status}</span>`;
}

// Forrmat currency 
function peso(amount) {
  return '₱' + Number(amount).toLocaleString('en-PH', { minimumFractionDigits: 2 });
}

//Modal helpers 
function openModal(id) {
  document.getElementById(id)?.classList.add('open');
}
function closeModal(id) {
  document.getElementById(id)?.classList.remove('open');
}
