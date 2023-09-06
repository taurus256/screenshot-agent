package ru.taustudio.duckview.manager.agents;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"WIN_CHROME\")")
@Component
public class WinChromeAgent extends Agent {
  public WinChromeAgent(ScreenshotControlFeignClient feignClient){
    super("WIN_CHROME", new SeleniumScreenshotWorkerImpl(
      "windows",
      () -> WebDriverManager.chromedriver().create(),
        0, 0, 0, 0,
        feignClient));
  }

  @Override
  protected void initAgent() {
    System.out.println("INIT DRIVER FOR " + getAgentName());
    WebDriverManager.chromedriver().setup();
  }
}