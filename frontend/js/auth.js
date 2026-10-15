/**
 * DMI College of Engineering - Smart Canteen Management System
 * Authentication & Role Guard System
 */

import { CONFIG, ENDPOINTS } from './config.js';
import { ApiService } from './api.js';

export class AuthGuard {
    static getUser() {
        return ApiService.getUser();
    }

    static isAuthenticated() {
        return !!ApiService.getToken() && !!this.getUser();
    }

    /**
     * Enforces role access on frontend pages.
     * Redirects to login if unauthenticated, or to appropriate dashboard if role unauthorized.
     */
    static requireRole(allowedRoles = []) {
        const user = this.getUser();
        const token = ApiService.getToken();

        // Calculate relative prefix back to root
        const path = window.location.pathname.toLowerCase();
        const isSubDir = path.includes('/student/') || path.includes('/staff/') || path.includes('/management/');
        const rootPrefix = isSubDir ? '../' : './';

        if (!token || !user) {
            window.location.href = `${rootPrefix}login.html?unauth=true`;
            return null;
        }

        if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
            console.warn(`Access denied for role ${user.role}. Required:`, allowedRoles);
            this.redirectByUserRole(user, rootPrefix);
            return null;
        }

        return user;
    }

    static redirectUser(roleOrUser, rootPrefix = './') {
        if (!roleOrUser) {
            window.location.href = `${rootPrefix}login.html`;
            return;
        }
        const user = typeof roleOrUser === 'string' ? { role: roleOrUser } : roleOrUser;
        this.redirectByUserRole(user, rootPrefix);
    }

    static redirectByUserRole(user, rootPrefix = './') {
        if (!user) {
            window.location.href = `${rootPrefix}login.html`;
            return;
        }

        switch (user.role) {
            case CONFIG.ROLES.STUDENT:
                window.location.href = `${rootPrefix}student/dashboard.html`;
                break;
            case CONFIG.ROLES.STAFF:
                window.location.href = `${rootPrefix}staff/dashboard.html`;
                break;
            case CONFIG.ROLES.MANAGER:
                window.location.href = `${rootPrefix}management/dashboard.html`;
                break;
            default:
                window.location.href = `${rootPrefix}index.html`;
        }
    }

    static async logout() {
        try {
            await ApiService.post(ENDPOINTS.AUTH_LOGOUT, {});
        } catch (e) {
            console.log('Logout notice:', e.message);
        } finally {
            ApiService.clearSession();
            const path = window.location.pathname.toLowerCase();
            const rootPrefix = (path.includes('/student/') || path.includes('/staff/') || path.includes('/management/')) ? '../' : './';
            window.location.href = `${rootPrefix}login.html?logged_out=true`;
        }
    }

    /**
     * Injects user info into navbar and attaches logout handler
     */
    static setupNavbar() {
        const user = this.getUser();
        const userNameEl = document.getElementById('navUserName');
        const userRoleEl = document.getElementById('navUserRole');
        const logoutBtn = document.getElementById('navLogoutBtn');

        if (user) {
            if (userNameEl) userNameEl.textContent = user.fullName || user.username;
            if (userRoleEl) {
                userRoleEl.textContent = user.roleDisplay || (user.role === 'ROLE_STUDENT' ? 'Student' : user.role === 'ROLE_STAFF' ? 'Staff' : 'Management');
            }
        }

        if (logoutBtn) {
            logoutBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.logout();
            });
        }
    }
}
