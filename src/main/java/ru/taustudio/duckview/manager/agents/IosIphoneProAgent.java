package ru.taustudio.duckview.manager.agents;

import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.AppiumWorkerImpl;
import ru.taustudio.duckview.manager.driver.Device;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"IOS_IPHONEPRO\")")
@Component
public class IosIphoneProAgent extends Agent {

  public IosIphoneProAgent(ScreenshotControlFeignClient feignClient){
    super("IOS_IPHONEPRO", new AppiumWorkerImpl(
            "4723",
            "macos",
        Device.IPHONE_PRO,
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
    getWorker().returnBrowserToInitialState();
  }
}
