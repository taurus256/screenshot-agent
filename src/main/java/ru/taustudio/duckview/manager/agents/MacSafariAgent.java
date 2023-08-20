package ru.taustudio.duckview.manager.agents;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SafariDesktopWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"MAC_SAFARI\")")
@Component
public class MacSafariAgent extends Agent {
  public MacSafariAgent(ScreenshotControlFeignClient feignClient){
    super("MAC_SAFARI", new SafariDesktopWorkerImpl(
      "mac",
      () -> WebDriverManager.safaridriver().create(),
        1, 0, 0, 0,
        feignClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.safaridriver().setup();
  }
}
