package ru.taustudio.duckview.manager.agents;

import com.netflix.discovery.EurekaClient;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"LIN_OPERA\")")
@Component
public class LinOperaAgent extends Agent {
  public LinOperaAgent(ScreenshotControlFeignClient feignClient, EurekaClient eurekaClient){
    super("LIN_OPERA", new SeleniumScreenshotWorkerImpl(
      "linux",
      () -> {
      return WebDriverManager.operadriver().create();
    }, 0, 0, 42, 16,
        feignClient, eurekaClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.operadriver().setup();
  }

  @Override
  protected void closeBrowser() throws IOException {
    Process process = Runtime.getRuntime().exec("killall -9 opera");
    new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(
        System.out::println);

  }
}
