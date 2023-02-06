package ru.taustudio.duckview.agent.aop;

public class TooManyAttemptsException extends Exception {
    public TooManyAttemptsException(String message){
        super(message);
    }
}
