/**
 * DMI College of Engineering - Smart Canteen Management System
 * Global API and configuration constants
 */

export const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api',
    COLLEGE_NAME: 'Campus Canteen',
    CANTEEN_NAME: 'Campus Canteen Management System',
    COLLEGE_LOCATION: 'Main Campus',
    FALLBACK_QR_IMAGE: '../assets/upi-qr.png',
    STORAGE_KEYS: {
        TOKEN: 'dmi_canteen_token',
        USER: 'dmi_canteen_user',
        THEME: 'dmi_canteen_theme',
        CART: 'dmi_canteen_cart'
    },
    ROLES: {
        STUDENT: 'ROLE_STUDENT',
        STAFF: 'ROLE_STAFF',
        MANAGER: 'ROLE_MANAGER'
    }
};

export const ENDPOINTS = {
    // Auth
    AUTH_CAPTCHA: `${CONFIG.API_BASE_URL}/auth/captcha`,
    AUTH_LOGIN: `${CONFIG.API_BASE_URL}/auth/login`,
    AUTH_REGISTER: `${CONFIG.API_BASE_URL}/auth/register`,
    AUTH_LOGOUT: `${CONFIG.API_BASE_URL}/auth/logout`,
    USERS_ME: `${CONFIG.API_BASE_URL}/users/me`,

    // Public / Catalog
    MENU_TODAY: `${CONFIG.API_BASE_URL}/menu/today`,
    CATEGORIES: `${CONFIG.API_BASE_URL}/categories`,
    COUNTERS: `${CONFIG.API_BASE_URL}/counters`,

    // Student
    STUDENT_ORDERS: `${CONFIG.API_BASE_URL}/student/orders`,
    STUDENT_BILLS: `${CONFIG.API_BASE_URL}/student/bills`,
    STUDENT_PAYMENT_UTR: `${CONFIG.API_BASE_URL}/student/payments/upi-utr`,
    STUDENT_UPI_CONFIG: `${CONFIG.API_BASE_URL}/student/upi-config`,
    STUDENT_FEEDBACK: `${CONFIG.API_BASE_URL}/student/feedback`,

    // Staff
    STAFF_ORDERS: `${CONFIG.API_BASE_URL}/staff/orders`,
    STAFF_PAYMENTS_PENDING: `${CONFIG.API_BASE_URL}/staff/payments/pending`,
    STAFF_PAYMENTS_VERIFY: (id) => `${CONFIG.API_BASE_URL}/staff/payments/${id}/verify`,
    STAFF_PAYMENTS_REJECT: (id) => `${CONFIG.API_BASE_URL}/staff/payments/${id}/reject`,
    STAFF_ORDER_STATUS: (id) => `${CONFIG.API_BASE_URL}/staff/orders/${id}/status`,
    STAFF_INVENTORY: `${CONFIG.API_BASE_URL}/staff/inventory`,
    STAFF_INVENTORY_RESTOCK: (id) => `${CONFIG.API_BASE_URL}/staff/inventory/${id}/restock`,
    STAFF_INVENTORY_UPDATE: (id) => `${CONFIG.API_BASE_URL}/staff/inventory/${id}/update`,
    STAFF_MENU_DAILY: `${CONFIG.API_BASE_URL}/staff/menu/daily`,
    STAFF_MENU_TOGGLE: `${CONFIG.API_BASE_URL}/staff/menu/toggle`,
    STAFF_FOOD: `${CONFIG.API_BASE_URL}/staff/food`,
    STAFF_FOOD_ITEM: (id) => `${CONFIG.API_BASE_URL}/staff/food/${id}`,
    STAFF_FOOD_REACTIVATE: (id) => `${CONFIG.API_BASE_URL}/staff/food/${id}/reactivate`,
    STAFF_FOOD_TOGGLE: (id) => `${CONFIG.API_BASE_URL}/staff/food/${id}/toggle-status`,
    STAFF_FOOD_UPLOAD_IMAGE: `${CONFIG.API_BASE_URL}/staff/food/upload-image`,
    STAFF_BILLS: `${CONFIG.API_BASE_URL}/staff/bills`,
    STAFF_BILLS_PENDING: `${CONFIG.API_BASE_URL}/staff/bills/pending`,
    STAFF_BILLS_VERIFY: (id) => `${CONFIG.API_BASE_URL}/staff/bills/${id}/verify`,
    STAFF_FEEDBACK: `${CONFIG.API_BASE_URL}/staff/feedback`,

    // Management
    MANAGEMENT_DASHBOARD: `${CONFIG.API_BASE_URL}/management/dashboard`,
    MANAGEMENT_REPORTS: `${CONFIG.API_BASE_URL}/management/reports/sales`,
    MANAGEMENT_EXPORT_CSV: `${CONFIG.API_BASE_URL}/management/reports/export-csv`,
    MANAGEMENT_FEEDBACK: `${CONFIG.API_BASE_URL}/management/feedback`,

    // AI Chatbot
    CHATBOT_MESSAGE: `${CONFIG.API_BASE_URL}/chatbot/message`
};
