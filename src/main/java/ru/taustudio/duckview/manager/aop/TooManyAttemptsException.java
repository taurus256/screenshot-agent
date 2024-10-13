package ru.taustudio.duckview.manager.aop;

public class TooManyAttemptsException extends Exception {
    public TooManyAttemptsException(String message){
        super(message);
    }
}
