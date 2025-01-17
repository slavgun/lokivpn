package com.lokivpn.DTO;

import java.util.List;

public class NotificationRequest {
    private Long chatId;
    private String message;
    private String photoUrl;
    private List<String> buttonTexts;
    private List<String> buttonUrls;

    // Getters and setters
    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public List<String> getButtonTexts() {
        return buttonTexts;
    }

    public void setButtonTexts(List<String> buttonTexts) {
        this.buttonTexts = buttonTexts;
    }

    public List<String> getButtonUrls() {
        return buttonUrls;
    }

    public void setButtonUrls(List<String> buttonUrls) {
        this.buttonUrls = buttonUrls;
    }
}
