package ru.taustudio.duckview.manager.agents;

import com.netflix.discovery.EurekaClient;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"WIN_FIREFOX\")")
@Component
public class WinFirefoxAgent extends Agent {
  public WinFirefoxAgent(ScreenshotControlFeignClient feignClient, EurekaClient eurekaClient){
    super("WIN_FIREFOX", new SeleniumScreenshotWorkerImpl(
      "windows",
      () -> {
      FirefoxOptions options = new FirefoxOptions();
      return WebDriverManager.firefoxdriver().capabilities(options).create();
      }, 0, 0, 12, 12,
        feignClient, eurekaClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.firefoxdriver().setup();
  }
}
