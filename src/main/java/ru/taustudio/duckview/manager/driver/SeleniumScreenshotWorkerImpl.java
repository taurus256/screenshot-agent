package ru.taustudio.duckview.manager.driver;

import java.util.function.Supplier;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import pazone.ashot.AShot;
import pazone.ashot.Screenshot;
import pazone.ashot.ShootingStrategies;
import pazone.ashot.cutter.FixedCutStrategy;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@ConditionalOnProperty(value="driver",
        havingValue = "selenium",
        matchIfMissing = false)
public class SeleniumScreenshotWorkerImpl implements Worker {
    String operationSystem;

    final Integer aShotTimeout = 1000;


    private Supplier<WebDriver> driverSupplier;
    private int headerToCut;
    private int footerToCut;
    private int rightScrollToCut;
    private int correctDesiredWidthOn;
    private ScreenshotControlFeignClient feignClient;

    public SeleniumScreenshotWorkerImpl(String operationSystem, Supplier<WebDriver> driverSupplier, int headerToCut,
        int footerToCut, int correctDesiredWidthOn, int rightScrollToCut,
        ScreenshotControlFeignClient feignClient){
        this.operationSystem = operationSystem;
        this.driverSupplier = driverSupplier;
        this.headerToCut = headerToCut;
        this.footerToCut = footerToCut;
        this.correctDesiredWidthOn = correctDesiredWidthOn;
        this.rightScrollToCut = rightScrollToCut;
        this.feignClient = feignClient;
    }

    @PostConstruct
    public void init(){
        System.out.println("WORKER STARTED");
    }

    public void doScreenshot(String jobUUID, String url, Integer width, Integer height) throws IOException, InterruptedException {
        System.out.println("Preparing render screenshot from url = " + url + ", save to " + System.getProperty("user.dir"));
        WebDriver driver = initDriver();
        driver.manage().window().setSize(new Dimension(width + rightScrollToCut + correctDesiredWidthOn, height + rightScrollToCut + correctDesiredWidthOn));
        driver.get(url);

//        Dimension win_size = driver.manage().window().getSize();
//        WebElement html = driver.findElement(By.tagName("html"));
//        int inner_width = Integer.parseInt(html.getAttribute("clientWidth"));
//        int inner_height = Integer.parseInt(html.getAttribute("clientHeight"));
//
//// set the inner size of the window to 400 x 400 (scrollbar excluded)
//        driver.manage().window().setSize(new Dimension(
//            win_size.width + (width - inner_width) + correctDesiredWidthOn,
//            win_size.height + (height - inner_height)
//        ));

        System.out.println("Setting size to " + driver.manage().window().getSize().getWidth() + " x " + driver.manage().window().getSize().getHeight());

        System.out.println("Do screenshot ");
        Screenshot s = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(aShotTimeout))
                .takeScreenshot(driver);
//        Screenshot s = new AShot()
//            .shootingStrategy(
//                pazone.ashot.ShootingStrategies.viewportNonRetina(ShootingStrategies.simple(),aShotTimeout,new FixedCutStrategy(headerToCut,footerToCut)))
//            .takeScreenshot(driver);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream is= new FileCacheImageOutputStream(os, new File("windows".equals(operationSystem) ? "C:\\Temp" : "/tmp" ));
        ImageIO.write(s.getImage().getSubimage(0, 0, s.getImage().getWidth() - rightScrollToCut, s.getImage().getHeight()), "PNG", is);
        driver.quit();
        feignClient.sendResult(jobUUID, new ByteArrayResource(os.toByteArray()));
    }

    WebDriver initDriver() {
        return driverSupplier.get();
//        switch (operationSystem) {
//            case "linux": {
//                switch (driverType) {
//                    case "firefox": {
//                        System.setProperty("webdriver.gecko.driver", "linux/" + "geckodriver");
//                        return new FirefoxDriver();
//                    }
//                    case "opera": {
//                        System.setProperty("webdriver.opera.driver", "linux/" + "operadriver");
//                        return new OperaDriver();
//                    }
//                    case "chrome": {
//                        System.setProperty("webdriver.chrome.driver", "linux/" + "chromedriver");
//                        return new ChromeDriver();
//                    }
//                }
//            }
//            case "windows": {
//                switch (driverType) {
//                    case "edge": {
//                        System.setProperty("webdriver.edge.driver", "windows/" + "msedgedriver.exe");
//                        return new EdgeDriver();
//                    }
//                    case "firefox": {
//                        System.setProperty("webdriver.gecko.driver", "windows/" + "geckodriver.exe");
//                        return new FirefoxDriver();
//                    }
//                    case "opera": {
//                        System.setProperty("webdriver.opera.driver", "windows/" + "operadriver.exe");
//                        return new OperaDriver();
//                    }
//                    case "chrome": {
//                        System.setProperty("webdriver.chrome.driver", "windows/" + "chromedriver.exe");
//                        return new ChromeDriver();
//                    }
//                }
//            }
//            case "macos":{
//                switch (driverType) {
//                    case "safari": {
//                        return new SafariDriver();
//                    }
//                }
//            }
//        }
//        throw new RuntimeException("DW: cannot find suitable driver for browser: " + driverType + " and OS: " + operationSystem);
    }
}
