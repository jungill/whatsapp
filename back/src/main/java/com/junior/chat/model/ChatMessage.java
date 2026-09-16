package com.junior.chat.model;

public record ChatMessage(String id, String from, String to, String content) {}