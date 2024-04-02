package ru.taustudio.duckview.manager.agents;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.AppiumWorkerImpl;
import ru.taustudio.duckview.manager.driver.Device;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"IOS_IPAD\")")
@Component
public class IosIpadAgent extends Agent {
  public IosIpadAgent(ScreenshotControlFeignClient feignClient){
    super("IOS_IPAD", new AppiumWorkerImpl(
            "4723",
            "macos",
        Device.IPAD,
        feignClient));
  }

  @Override
  public void initAgent(){
    System.out.println("Agent " + getAgentName() + " initialized");
  }

  @Scheduled(fixedRate = 10*60*1000)
  public void callRefreshContext(){
    ((AppiumWorkerImpl)getWorker()).watchForSession();
  }

  @Override
  public void destroy(){
    getWorker().destroy();
  }

  @Override
  protected void closeBrowser() throws IOException {
    System.out.println("Close operation for " + getAgentName() + " has not implemented");
  }
}
