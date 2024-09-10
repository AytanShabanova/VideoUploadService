package com.example.videoupload.exceptions;

public class VideoTooSmallException extends RuntimeException {
    public VideoTooSmallException(String message) {
        super(message);
    }
}
