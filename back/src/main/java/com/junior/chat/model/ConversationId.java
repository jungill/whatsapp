package com.junior.chat.model;

public final class ConversationId {

    private ConversationId() {}

    public static String of(String a, String b) {
        return a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a;
    }
}