package com.veggo.app.data.remote.dto;

import java.util.List;

public class ApiListResponseDto<T> {
    private boolean success;
    private List<T> data;

    public boolean isSuccess() {
        return success;
    }

    public List<T> getData() {
        return data;
    }
}
