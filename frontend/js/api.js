/**
 * DMI College of Engineering - Smart Canteen Management System
 * Fetch API Wrapper with JWT authorization and error handling
 */

import { CONFIG } from './config.js';

export class ApiService {
    static getToken() {
        return localStorage.getItem(CONFIG.STORAGE_KEYS.TOKEN);
    }

    static setToken(token) {
        if (token) {
            localStorage.setItem(CONFIG.STORAGE_KEYS.TOKEN, token);
        } else {
            localStorage.removeItem(CONFIG.STORAGE_KEYS.TOKEN);
        }
    }

    static getUser() {
        const userJson = localStorage.getItem(CONFIG.STORAGE_KEYS.USER);
        try {
            return userJson ? JSON.parse(userJson) : null;
        } catch (e) {
            return null;
        }
    }

    static setUser(user) {
        if (user) {
            localStorage.setItem(CONFIG.STORAGE_KEYS.USER, JSON.stringify(user));
        } else {
            localStorage.removeItem(CONFIG.STORAGE_KEYS.USER);
        }
    }

    static saveSession(token, user) {
        if (token) this.setToken(token);
        if (user) this.setUser(user);
    }

    static clearSession() {
        localStorage.removeItem(CONFIG.STORAGE_KEYS.TOKEN);
        localStorage.removeItem(CONFIG.STORAGE_KEYS.USER);
    }

    static async request(url, options = {}) {
        const headers = {
            'Accept': 'application/json',
            ...(options.headers || {})
        };

        if (!(options.body instanceof FormData)) {
            if (!headers['Content-Type']) {
                headers['Content-Type'] = 'application/json';
            }
        }

        const token = this.getToken();
        if (token && !headers['Authorization']) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(url, {
                ...options,
                headers
            });

            // Handle 401 Unauthorized
            if (response.status === 401) {
                this.clearSession();
                const currentPath = window.location.pathname.toLowerCase();
                if (!currentPath.includes('login') && !currentPath.includes('register') && currentPath !== '/' && !currentPath.endsWith('index.html')) {
                    const depthPrefix = currentPath.includes('/student/') || currentPath.includes('/staff/') || currentPath.includes('/management/') ? '../' : './';
                    window.location.href = `${depthPrefix}login.html?expired=true`;
                }
                throw new Error('Your session has expired. Please log in again.');
            }

            // Check if response is HTML or text (e.g. printable receipts)
            const contentType = response.headers.get('content-type') || '';
            if (contentType.includes('text/html') || contentType.includes('text/csv')) {
                if (!response.ok) {
                    throw new Error(`Request failed with status ${response.status}`);
                }
                return await response.text();
            }

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                let errorMessage = 'Request failed';
                if (data) {
                    if (data.message) {
                        errorMessage = data.message;
                    } else if (data.error) {
                        errorMessage = data.error;
                    }
                    if (data.details && Array.isArray(data.details) && data.details.length > 0) {
                        errorMessage += ': ' + data.details.join(', ');
                    }
                }
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            // Dual compatibility: ensure both res.data and direct res iterate/work cleanly
            if (data && typeof data === 'object' && 'success' in data) {
                const payload = data.data;
                if (Array.isArray(payload)) {
                    payload.success = data.success;
                    payload.message = data.message;
                    payload.data = payload;
                    return payload;
                }
                if (payload !== null && typeof payload === 'object') {
                    payload.success = data.success;
                    payload.message = data.message;
                    payload.data = payload;
                    return payload;
                }
            }

            return data;
        } catch (error) {
            console.error(`API Error on [${options.method || 'GET'}] ${url}:`, error);
            throw error;
        }
    }

    static get(url, headers = {}) {
        return this.request(url, { method: 'GET', headers });
    }

    static post(url, body = {}, headers = {}) {
        return this.request(url, {
            method: 'POST',
            headers,
            body: body instanceof FormData ? body : JSON.stringify(body)
        });
    }

    static put(url, body = {}, headers = {}) {
        return this.request(url, {
            method: 'PUT',
            headers,
            body: body instanceof FormData ? body : JSON.stringify(body)
        });
    }

    static patch(url, body = {}, headers = {}) {
        return this.request(url, {
            method: 'PATCH',
            headers,
            body: body instanceof FormData ? body : JSON.stringify(body)
        });
    }

    static delete(url, headers = {}) {
        return this.request(url, { method: 'DELETE', headers });
    }

    static uploadFile(url, fileOrBlob, fieldName = 'file') {
        const formData = new FormData();
        formData.append(fieldName, fileOrBlob);
        return this.request(url, {
            method: 'POST',
            body: formData
        });
    }
}
