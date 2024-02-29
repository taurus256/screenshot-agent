package ru.taustudio.duckview.manager.agents;

import com.netflix.discovery.EurekaClient;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"WIN_OPERA\")")
@Component
public class WinOperaAgent extends Agent {
  public WinOperaAgent(ScreenshotControlFeignClient feignClient, EurekaClient eurekaClient){
    super("WIN_OPERA", new SeleniumScreenshotWorkerImpl(
      "windows",
      () -> WebDriverManager.operadriver().create(),
        0, 0, 76, 15,
        feignClient, eurekaClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.operadriver().setup();
  }
}
