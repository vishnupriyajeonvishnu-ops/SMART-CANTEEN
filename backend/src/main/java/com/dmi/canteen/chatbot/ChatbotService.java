package com.dmi.canteen.chatbot;

public interface ChatbotService {
    ChatbotResponse processMessage(String message, Long userId, String role);
}
