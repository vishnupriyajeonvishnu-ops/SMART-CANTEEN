/**
 * DMI College of Engineering - Smart Canteen Management System
 * Floating AI Chatbot Widget with role-aware quick-reply chips
 */

import { ENDPOINTS } from './config.js';
import { ApiService } from './api.js';

export class ChatbotWidget {
    static init() {
        if (document.getElementById('canteenChatbotWidget')) return;

        const widgetHtml = `
            <div id="canteenChatbotWidget" class="chatbot-widget">
                <button id="chatbotToggleBtn" class="chatbot-btn" aria-label="Open Canteen Assistant">
                    <i class="bi bi-chat-dots-fill"></i>
                </button>
                <div id="chatbotPanel" class="chatbot-panel">
                    <div class="chatbot-header">
                        <div class="d-flex align-items-center gap-2">
                            <div class="bg-white rounded-circle p-1 d-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                                <i class="bi bi-robot text-primary fs-6"></i>
                            </div>
                            <div>
                                <h6 class="mb-0 fw-bold fs-6">CampusCanteen AI</h6>
                                <small class="text-white-50" style="font-size: 11px;">Instant Live Assistance</small>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-1">
                            <button id="chatbotClearBtn" class="btn btn-sm btn-link text-white text-decoration-none p-1" title="Clear Chat">
                                <i class="bi bi-arrow-counterclockwise fs-6"></i>
                            </button>
                            <button id="chatbotCloseBtn" class="btn btn-sm btn-link text-white text-decoration-none p-1" title="Close">
                                <i class="bi bi-x-lg fs-6"></i>
                            </button>
                        </div>
                    </div>
                    <div id="chatbotMessages" class="chatbot-messages">
                        <!-- Messages injected here -->
                    </div>
                    <div id="chatbotQuickChips" class="p-2 border-top d-flex gap-1 flex-wrap" style="background: var(--bg-surface); max-height: 80px; overflow-y: auto;">
                        <!-- Chips injected here -->
                    </div>
                    <div class="p-2 border-top" style="background: var(--bg-surface);">
                        <form id="chatbotForm" class="d-flex gap-2">
                            <input type="text" id="chatbotInput" class="form-control form-control-sm rounded-pill" placeholder="Ask about menu, prices, order..." autocomplete="off">
                            <button type="submit" class="btn btn-primary btn-sm rounded-circle d-flex align-items-center justify-content-center" style="width: 34px; height: 34px;">
                                <i class="bi bi-send-fill fs-6"></i>
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', widgetHtml);
        this.bindEvents();
        this.sendInitialGreeting();
    }

    static bindEvents() {
        const toggleBtn = document.getElementById('chatbotToggleBtn');
        const closeBtn = document.getElementById('chatbotCloseBtn');
        const clearBtn = document.getElementById('chatbotClearBtn');
        const panel = document.getElementById('chatbotPanel');
        const form = document.getElementById('chatbotForm');
        const input = document.getElementById('chatbotInput');

        toggleBtn.addEventListener('click', () => {
            panel.classList.toggle('open');
            if (panel.classList.contains('open')) {
                input.focus();
            }
        });

        closeBtn.addEventListener('click', () => {
            panel.classList.remove('open');
        });

        clearBtn.addEventListener('click', () => {
            const messagesContainer = document.getElementById('chatbotMessages');
            messagesContainer.innerHTML = '';
            this.sendInitialGreeting();
        });

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const text = input.value.trim();
            if (!text) return;
            input.value = '';
            this.handleUserMessage(text);
        });
    }

    static async sendInitialGreeting() {
        try {
            const res = await ApiService.post(ENDPOINTS.CHATBOT_MESSAGE, { message: '' });
            if (res) {
                this.appendBotMessage(res.reply);
                this.renderQuickChips(res.quickReplies || []);
            }
        } catch (e) {
            this.appendBotMessage("👋 Hello! Welcome to Campus Canteen. How can I help you today?");
            this.renderQuickChips(["Today's Menu", "Picks under 50", "How to pay via UPI"]);
        }
    }

    static async handleUserMessage(message) {
        this.appendUserMessage(message);
        this.showTypingIndicator();

        try {
            const res = await ApiService.post(ENDPOINTS.CHATBOT_MESSAGE, { message });
            this.removeTypingIndicator();
            if (res && res.reply) {
                this.appendBotMessage(res.reply);
                this.renderQuickChips(res.quickReplies || []);
            } else {
                this.appendBotMessage("I couldn't process that question right now. Please try asking about today's menu or prices!");
            }
        } catch (e) {
            this.removeTypingIndicator();
            this.appendBotMessage("Sorry, I encountered a temporary connection issue. Please try again shortly.");
        }
    }

    static appendUserMessage(text) {
        const container = document.getElementById('chatbotMessages');
        const el = document.createElement('div');
        el.className = 'chat-bubble user';
        el.textContent = text;
        container.appendChild(el);
        this.scrollToBottom();
    }

    static appendBotMessage(markdownText) {
        const container = document.getElementById('chatbotMessages');
        const el = document.createElement('div');
        el.className = 'chat-bubble bot';
        el.innerHTML = this.formatMarkdown(markdownText);
        container.appendChild(el);
        this.scrollToBottom();
    }

    static showTypingIndicator() {
        const container = document.getElementById('chatbotMessages');
        const el = document.createElement('div');
        el.id = 'chatbotTyping';
        el.className = 'chat-bubble bot d-flex align-items-center gap-1';
        el.innerHTML = `
            <span class="spinner-grow spinner-grow-sm text-primary" role="status" style="width: 8px; height: 8px;"></span>
            <span class="spinner-grow spinner-grow-sm text-primary" role="status" style="width: 8px; height: 8px; animation-delay: 0.15s;"></span>
            <span class="spinner-grow spinner-grow-sm text-primary" role="status" style="width: 8px; height: 8px; animation-delay: 0.3s;"></span>
        `;
        container.appendChild(el);
        this.scrollToBottom();
    }

    static removeTypingIndicator() {
        const el = document.getElementById('chatbotTyping');
        if (el) el.remove();
    }

    static renderQuickChips(chips = []) {
        const container = document.getElementById('chatbotQuickChips');
        if (!container) return;
        container.innerHTML = '';
        chips.forEach(chip => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'quick-reply-chip';
            btn.textContent = chip;
            btn.addEventListener('click', () => {
                this.handleUserMessage(chip);
            });
            container.appendChild(btn);
        });
    }

    static scrollToBottom() {
        const container = document.getElementById('chatbotMessages');
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    }

    static formatMarkdown(text) {
        if (!text) return '';
        let html = text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/^• (.*?)$/gm, '<li style="margin-left: 14px;">$1</li>')
            .replace(/\n/g, '<br>');
        return html;
    }
}

/**
 * Convenient standalone initializer called across all portal pages
 */
export function initChatbot() {
    return ChatbotWidget.init();
}

export default ChatbotWidget;

