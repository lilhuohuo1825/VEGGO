package com.veggo.app.domain.model;

import com.veggo.app.data.remote.dto.ChatSuggestedProductDto;
import com.veggo.app.data.remote.dto.ChatSuggestedRecipeDto;

import java.util.Collections;
import java.util.List;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_WELCOME = 3;
    public static final int TYPE_LOADING = 4;
    public static final int TYPE_RECIPE_LIST = 5;
    public static final int TYPE_PRODUCT_LIST = 6;

    private final String message;
    private final int type;
    private final long timestamp;
    private final List<ChatSuggestedProductDto> products;
    private final List<ChatSuggestedRecipeDto> recipes;

    public ChatMessage(String message, int type) {
        this(message, type, Collections.emptyList(), Collections.emptyList());
    }

    public ChatMessage(
            String message,
            int type,
            List<ChatSuggestedProductDto> products,
            List<ChatSuggestedRecipeDto> recipes
    ) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.products = products != null ? products : Collections.emptyList();
        this.recipes = recipes != null ? recipes : Collections.emptyList();
    }

    public static ChatMessage loading() {
        return new ChatMessage("Đang soạn câu trả lời...", TYPE_LOADING);
    }

    public static ChatMessage productList(List<ChatSuggestedProductDto> products) {
        return new ChatMessage("", TYPE_PRODUCT_LIST, products, Collections.emptyList());
    }

    public static ChatMessage recipeList(List<ChatSuggestedRecipeDto> recipes) {
        return new ChatMessage("", TYPE_RECIPE_LIST, Collections.emptyList(), recipes);
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<ChatSuggestedProductDto> getProducts() {
        return products;
    }

    public List<ChatSuggestedRecipeDto> getRecipes() {
        return recipes;
    }
}
