package com.veggo.app.data.remote.dto;

public class AiRecognitionRequestDto {
    private String imageBase64;

    public AiRecognitionRequestDto(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
