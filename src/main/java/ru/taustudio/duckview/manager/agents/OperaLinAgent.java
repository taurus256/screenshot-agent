package ru.taustudio.duckview.manager.agents;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"LIN_OPERA\")")
@Component
public class OperaLinAgent extends Agent {
  public OperaLinAgent(ScreenshotControlFeignClient feignClient){
    super("LIN_OPERA", new SeleniumScreenshotWorkerImpl(() -> {
      return WebDriverManager.operadriver().create();
    }, 0, 0, 42, 16,
        feignClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.operadriver().setup();
  }
}
