package ru.taustudio.duckview.manager.driver;

import org.openqa.selenium.remote.RemoteWebDriver;

public interface Worker {
    default void init(){};
    void doScreenshot(Long jobId, String url, Integer width, Integer height) throws Exception;
}
