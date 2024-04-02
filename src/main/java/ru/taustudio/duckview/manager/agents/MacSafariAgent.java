package ru.taustudio.duckview.manager.agents;

import com.netflix.discovery.EurekaClient;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SafariDesktopWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"MAC_SAFARI\")")
@Component
public class MacSafariAgent extends Agent {
  public MacSafariAgent(ScreenshotControlFeignClient feignClient, EurekaClient eurekaClient){
    super("MAC_SAFARI", new SafariDesktopWorkerImpl(
      "mac",
      () -> WebDriverManager.safaridriver().create(),
        1, 0, 0, 0,
        feignClient, eurekaClient));
  }

  @Override
  protected void initAgent() {
    WebDriverManager.safaridriver().setup();
  }

  @Override
  protected void closeBrowser() throws IOException {
    Process process = Runtime.getRuntime().exec("killall -9 Safari");
    new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(
        System.out::println);
  }
}
