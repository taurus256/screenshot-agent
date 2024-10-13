package ru.taustudio.duckview.manager.agents;

import com.netflix.discovery.EurekaClient;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.openqa.selenium.edge.EdgeOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"WIN_EDGE\")")
@Component
public class WinEdgeAgent extends Agent {
  public WinEdgeAgent(ScreenshotControlFeignClient feignClient, EurekaClient eurekaClient){
    super("WIN_EDGE", new SeleniumScreenshotWorkerImpl(
      "windows",
            () -> {
              EdgeOptions edgeOptions = new EdgeOptions();
              edgeOptions.setExperimentalOption("useAutomationExtension", false);
              edgeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation", "enable-logging"});
              return WebDriverManager.edgedriver().capabilities(edgeOptions).create();
            },
        0, 0, 0, 15,
        feignClient, eurekaClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.edgedriver().setup();
  }

  @Override
  protected void closeBrowser() throws IOException {
    Process process = Runtime.getRuntime().exec("taskkill /f /IM msedge.exe");
    new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(
        System.out::println);

  }
}
