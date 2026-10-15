package com.dmi.canteen.chatbot;

import java.util.List;

public class ChatbotResponse {
    private String reply;
    private List<String> quickReplies;
    private String intent;

    public ChatbotResponse() {}

    public ChatbotResponse(String reply, List<String> quickReplies, String intent) {
        this.reply = reply;
        this.quickReplies = quickReplies;
        this.intent = intent;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getQuickReplies() {
        return quickReplies;
    }

    public void setQuickReplies(List<String> quickReplies) {
        this.quickReplies = quickReplies;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}
