package com.veggo.app.core.ui;

public class ViewState<T> {
    public enum Status { LOADING, SUCCESS, ERROR, EMPTY }

    private final Status status;
    private final T data;
    private final String message;

    private ViewState(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> ViewState<T> loading() { return new ViewState<>(Status.LOADING, null, null); }
    public static <T> ViewState<T> success(T data) { return new ViewState<>(Status.SUCCESS, data, null); }
    public static <T> ViewState<T> error(String message) { return new ViewState<>(Status.ERROR, null, message); }
    public static <T> ViewState<T> empty() { return new ViewState<>(Status.EMPTY, null, null); }

    public Status getStatus() { return status; }
    public T getData() { return data; }
    public String getMessage() { return message; }
}
