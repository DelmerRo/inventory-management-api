package com.utama.my_inventory.dtos;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record BaseResponse(
        boolean error,
        HttpStatus status,
        String message
) {


    public static BaseResponse ok(String message) {
        return new BaseResponse(false, HttpStatus.OK, message);
    }

    public static BaseResponse created(String message) {
        return new BaseResponse(false, HttpStatus.CREATED, message);
    }

    public static BaseResponse badRequest(String message) {
        return new BaseResponse(true, HttpStatus.BAD_REQUEST, message);
    }

    public static BaseResponse notFound(String message) {
        return new BaseResponse(true, HttpStatus.NOT_FOUND, message);
    }

    public static BaseResponse conflict(String message) {
        return new BaseResponse(true, HttpStatus.CONFLICT, message);
    }

    public static BaseResponse unauthorized(String message) {
        return new BaseResponse(true, HttpStatus.UNAUTHORIZED, message);
    }

    public static BaseResponse forbidden(String message) {
        return new BaseResponse(true, HttpStatus.FORBIDDEN, message);
    }

    public static BaseResponse internalError(String message) {
        return new BaseResponse(true, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }


    public int code() {
        return status.value();
    }

    public String statusName() {
        return status.getReasonPhrase();
    }

    public ResponseEntity<BaseResponse> toResponseEntity() {
        return ResponseEntity.status(status).body(this);
    }
}