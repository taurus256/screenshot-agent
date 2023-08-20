package ru.taustudio.duckview.manager.agents;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"LIN_FIREFOX\")")
@Component
public class LinFirefoxAgent extends Agent {
  public LinFirefoxAgent(ScreenshotControlFeignClient feignClient){
    super("LIN_FIREFOX", new SeleniumScreenshotWorkerImpl(
      "linux",
      () -> {
      FirefoxOptions options = new FirefoxOptions();
      options.addPreference("dom.disable_scrollbars", true);
      return WebDriverManager.firefoxdriver().capabilities(options).create();
      }, 0, 0, 0, 0,
        feignClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.firefoxdriver().setup();
  }
}