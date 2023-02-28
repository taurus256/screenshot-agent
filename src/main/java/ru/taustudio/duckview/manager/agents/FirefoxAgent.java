package ru.taustudio.duckview.manager.agents;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.driver.SeleniumScreenshotWorkerImpl;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

@ConditionalOnExpression("'${agents}'.contains(\"LIN_FIREFOX\")")
@Component
public class FirefoxAgent extends Agent {
  public FirefoxAgent(ScreenshotControlFeignClient feignClient){
    //TODO test properly given driver!!
    super("lin_firefox", new SeleniumScreenshotWorkerImpl(()->new FirefoxDriver(),0,0,0,
        feignClient));
  }
}
