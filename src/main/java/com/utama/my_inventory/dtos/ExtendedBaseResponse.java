package com.utama.my_inventory.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtendedBaseResponse<T>(
        boolean error,
        int code,
        String status,
        String message,
        T data
) {

    public static <T> ExtendedBaseResponse<T> of(BaseResponse base, T data) {
        return new ExtendedBaseResponse<>(
                base.error(),
                base.code(),
                base.statusName(),
                base.message(),
                data
        );
    }

    public static <T> ExtendedBaseResponse<T> ok(T data, String message) {
        return of(BaseResponse.ok(message), data);
    }

    public static <T> ExtendedBaseResponse<T> created(T data, String message) {
        return of(BaseResponse.created(message), data);
    }

    public static <T> ExtendedBaseResponse<T> notFound(String message) {
        return of(BaseResponse.notFound(message), null);
    }

    public static <T> ExtendedBaseResponse<T> conflict(String message) {
        return of(BaseResponse.conflict(message), null);
    }

    public ResponseEntity<ExtendedBaseResponse<T>> toResponseEntity() {
        return ResponseEntity.status(code).body(this);
    }
}