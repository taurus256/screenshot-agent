package ru.taustudio.duckview.agent.driver;

public interface Worker {
    void doScreenshot(Long jobId, String url, Integer width, Integer height) throws Exception;
}
