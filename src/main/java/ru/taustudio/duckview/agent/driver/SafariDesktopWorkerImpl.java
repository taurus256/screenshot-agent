package ru.taustudio.duckview.agent.driver;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import pazone.ishot.IShot;
import pazone.ishot.Screenshot;
import pazone.ishot.ShootingStrategies;
import pazone.ishot.cutter.FixedCutStrategy;
import ru.taustudio.duckview.agent.screenshots.ScreenshotControlFeignClient;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@ConditionalOnProperty(value="driver",
        havingValue = "safari_desktop",
        matchIfMissing = false)
@Component
public class SafariDesktopWorkerImpl implements Worker {
    @Value("${operationSystem:linux}")
    String operationSystem;
    @Value("${browser:firefox}")
    String driverType;
    final Integer aShotTimeout = 1000;

    @Autowired
    ScreenshotControlFeignClient feignClient;

    @PostConstruct
    public void init(){
        System.out.println("AGENT STARTED FOR: ");
        System.out.println("operationSystem = " + operationSystem);
        System.out.println("driverType = " + driverType);
    }

    public void doScreenshot(Long jobId, String url, Integer width, Integer height) throws IOException, InterruptedException {
        System.out.println("Preparing render screenshot from url = " + url + ", save to " + System.getProperty("user.dir"));
        RemoteWebDriver driver = initDriver();
        driver.get(url);
        System.out.println("Setting size to " + width + " x " + height);
        driver.manage().window().setSize(new Dimension(width, height));
        System.out.println("Do screenshot ");
        Screenshot s = new IShot()
                .shootingStrategy(ShootingStrategies.viewportNonRetina(ShootingStrategies.simple(),aShotTimeout,new FixedCutStrategy(1,0)))
                .takeScreenshot(driver);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream is= new FileCacheImageOutputStream(os, new File("windows".equals(operationSystem) ? "C:\\Temp" : "/tmp" ));
        ImageIO.write(s.getImage(), "PNG", is);
        driver.quit();
        feignClient.sendResult(jobId, new ByteArrayResource(os.toByteArray()));
    }

    private RemoteWebDriver initDriver() {
        switch (operationSystem) {
            case "linux": {
                switch (driverType) {
                    case "firefox": {
                        System.setProperty("webdriver.gecko.driver", "linux/" + "geckodriver");
                        return new FirefoxDriver();
                    }
                    case "opera": {
                        System.setProperty("webdriver.opera.driver", "linux/" + "operadriver");
                        return new OperaDriver();
                    }
                    case "chrome": {
                        System.setProperty("webdriver.chrome.driver", "linux/" + "chromedriver");
                        return new ChromeDriver();
                    }
                }
            }
            case "windows": {
                switch (driverType) {
                    case "edge": {
                        System.setProperty("webdriver.edge.driver", "windows/" + "msedgedriver.exe");
                        return new EdgeDriver();
                    }
                    case "firefox": {
                        System.setProperty("webdriver.gecko.driver", "windows/" + "geckodriver.exe");
                        return new FirefoxDriver();
                    }
                    case "opera": {
                        System.setProperty("webdriver.opera.driver", "windows/" + "operadriver.exe");
                        return new OperaDriver();
                    }
                    case "chrome": {
                        System.setProperty("webdriver.chrome.driver", "windows/" + "chromedriver.exe");
                        return new ChromeDriver();
                    }
                }
            }
            case "macos":{
                switch (driverType) {
                    case "safari": {
                        return new SafariDriver();
                    }
                }
            }
        }
        throw new RuntimeException("DW: cannot find suitable driver for browser: " + driverType + " and OS: " + operationSystem);
    }
}
