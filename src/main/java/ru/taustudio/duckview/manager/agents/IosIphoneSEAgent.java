package ru.taustudio.duckview.manager.agents;

import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.AppiumWorkerImpl;
import ru.taustudio.duckview.manager.driver.Device;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"IOS_IPHONESE\")")
@Component
public class IosIphoneSEAgent extends Agent {


  public IosIphoneSEAgent(ScreenshotControlFeignClient feignClient){
    super("IOS_IPHONESE", new AppiumWorkerImpl(
            "4723",
            "macos",
        Device.IPHONE_SE,
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
