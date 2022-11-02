package ru.taustudio.duckview.agent.driver;

import org.springframework.core.io.ByteArrayResource;

public interface ScreenshotDriverService {
    void doScreenshot(Long jobId, String url, Integer width, Integer height) throws Exception;
}
