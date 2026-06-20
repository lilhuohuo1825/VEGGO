package com.veggo.app.data.remote.dto;

public class AiRecognitionResponseDto {
    private String name;
    private Double quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
}
