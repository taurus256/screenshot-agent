package ru.taustudio.duckview.manager.agents;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"WIN_EDGE\")")
@Component
public class WinEdgeAgent extends Agent {
  public WinEdgeAgent(ScreenshotControlFeignClient feignClient){
    super("WIN_EDGE", new SeleniumScreenshotWorkerImpl(
      "windows",
      () -> WebDriverManager.edgedriver().create(),
        0, 0, 71, 15,
        feignClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.edgedriver().setup();
  }
}
