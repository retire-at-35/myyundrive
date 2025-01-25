package com.pan.exception;

public class BussinessException extends RuntimeException{
    public BussinessException() {
    }

    public BussinessException(String msg) {
        super(msg);
    }
}
