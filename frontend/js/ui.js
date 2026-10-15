/**
 * DMI College of Engineering - Smart Canteen Management System
 * UI Utilities: Toasts, Modals, Formatters, Theme Manager, and Cart State
 */

import { CONFIG } from './config.js';

export class UI {
    /**
     * Display a modern floating toast notification
     */
    static showToast(message, type = 'success', durationMs = 4000) {
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            container.style.zIndex = '1100';
            document.body.appendChild(container);
        }

        const toastId = 'toast_' + Date.now();
        const iconMap = {
            success: 'bi-check-circle-fill text-success',
            danger: 'bi-exclamation-triangle-fill text-danger',
            warning: 'bi-exclamation-circle-fill text-warning',
            info: 'bi-info-circle-fill text-primary'
        };
        const icon = iconMap[type] || iconMap.info;

        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center border-0 shadow-lg" role="alert" aria-live="assertive" aria-atomic="true" style="border-radius: 12px; overflow: hidden;">
                <div class="d-flex">
                    <div class="toast-body d-flex align-items-center gap-2">
                        <i class="bi ${icon} fs-5"></i>
                        <span class="fw-medium">${message}</span>
                    </div>
                    <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;

        container.insertAdjacentHTML('beforeend', toastHtml);
        const toastEl = document.getElementById(toastId);
        if (window.bootstrap && window.bootstrap.Toast) {
            const bsToast = new window.bootstrap.Toast(toastEl, { delay: durationMs });
            bsToast.show();
            toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
        } else {
            setTimeout(() => toastEl.remove(), durationMs);
        }
    }

    static formatCurrency(amount) {
        const val = Number(amount) || 0;
        return '₹' + val.toFixed(2);
    }

    static formatDate(dateStr) {
        if (!dateStr) return 'N/A';
        try {
            const d = new Date(dateStr);
            return d.toLocaleDateString('en-IN', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            return dateStr;
        }
    }

    static initTheme() {
        const savedTheme = localStorage.getItem(CONFIG.STORAGE_KEYS.THEME) || 'light';
        document.documentElement.setAttribute('data-bs-theme', savedTheme);
        this.updateThemeIcon(savedTheme);

        const toggleBtn = document.getElementById('themeToggleBtn');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                const current = document.documentElement.getAttribute('data-bs-theme');
                const next = current === 'dark' ? 'light' : 'dark';
                document.documentElement.setAttribute('data-bs-theme', next);
                localStorage.setItem(CONFIG.STORAGE_KEYS.THEME, next);
                this.updateThemeIcon(next);
            });
        }
    }

    static updateThemeIcon(theme) {
        const icon = document.getElementById('themeToggleIcon');
        if (icon) {
            if (theme === 'dark') {
                icon.className = 'bi bi-sun-fill text-warning';
            } else {
                icon.className = 'bi bi-moon-stars-fill text-primary';
            }
        }
    }

    // ==========================================
    // CART STATE MANAGEMENT (Stored in localStorage)
    // ==========================================

    static getCart() {
        const json = localStorage.getItem(CONFIG.STORAGE_KEYS.CART);
        try {
            return json ? JSON.parse(json) : [];
        } catch (e) {
            return [];
        }
    }

    static saveCart(cart) {
        localStorage.setItem(CONFIG.STORAGE_KEYS.CART, JSON.stringify(cart));
        this.updateCartBadge();
    }

    static addToCart(foodItem, quantity = 1) {
        const cart = this.getCart();
        const existing = cart.find(i => i.id === foodItem.id);
        if (existing) {
            existing.quantity += quantity;
        } else {
            cart.push({
                id: foodItem.id,
                name: foodItem.name,
                price: Number(foodItem.price),
                veg: foodItem.veg,
                counterName: foodItem.counterName || 'Food Counter',
                quantity: quantity
            });
        }
        this.saveCart(cart);
        this.showToast(`Added ${foodItem.name} to cart`, 'success', 2000);
    }

    static updateCartQty(foodId, newQty) {
        let cart = this.getCart();
        if (newQty <= 0) {
            cart = cart.filter(i => i.id !== foodId);
        } else {
            const item = cart.find(i => i.id === foodId);
            if (item) item.quantity = newQty;
        }
        this.saveCart(cart);
    }

    static clearCart() {
        localStorage.removeItem(CONFIG.STORAGE_KEYS.CART);
        this.updateCartBadge();
    }

    static getCartTotal() {
        return this.getCart().reduce((sum, i) => sum + (i.price * i.quantity), 0);
    }

    static getCartItemCount() {
        return this.getCart().reduce((sum, i) => sum + i.quantity, 0);
    }

    static updateCartBadge() {
        const badges = document.querySelectorAll('.cart-badge');
        const count = this.getCartItemCount();
        badges.forEach(b => {
            b.textContent = count;
            b.style.display = count > 0 ? 'inline-block' : 'none';
        });
    }
}
