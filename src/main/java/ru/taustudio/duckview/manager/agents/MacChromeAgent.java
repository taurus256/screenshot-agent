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

@ConditionalOnExpression("'${agents}'.contains(\"MAC_CHROME\")")
@Component
public class MacChromeAgent extends Agent {
  public MacChromeAgent(ScreenshotControlFeignClient feignClient, EurekaClient eurekaClient){
    super("MAC_CHROME", new SeleniumScreenshotWorkerImpl(
      "mac",
      () -> WebDriverManager.chromedriver().create(),
        0, 0, 16, 16,
        feignClient, eurekaClient));
  }

  @Override
  protected void initAgent() {
    System.out.println("INIT DRIVER FOR " + getAgentName());
    WebDriverManager.chromedriver().setup();
  }

  @Override
  protected void closeBrowser() throws IOException {
    Process process = Runtime.getRuntime().exec("killall -9 \"Google Chrome\"");
    new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(
        System.out::println);
  }
}