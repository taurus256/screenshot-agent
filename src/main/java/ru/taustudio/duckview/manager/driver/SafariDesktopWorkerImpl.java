package ru.taustudio.duckview.manager.driver;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import pazone.ashot.AShot;
import pazone.ashot.Screenshot;
import pazone.ashot.ShootingStrategies;
import pazone.ashot.cutter.FixedCutStrategy;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;
import ru.taustudio.duckview.manager.aop.RetrytOnFailure;
import io.github.bonigarcia.wdm.WebDriverManager;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import static pazone.ashot.ShootingStrategies.cutting;
import static pazone.ashot.ShootingStrategies.viewportPasting;

public class SafariDesktopWorkerImpl implements Worker {

    public static final int MAX_TRY_ATTEMPTS = 2;
    private final Supplier<WebDriver> driverSupplier;
    private final int headerToCut;
    private final int footerToCut;
    private final int correctDesiredWidthOn;
    private final int rightScrollToCut;
    String operationSystem;
    final Integer aShotTimeout = 1000;
    int tryCounter;
    ScreenshotControlFeignClient feignClient;

    public SafariDesktopWorkerImpl(String operationSystem, Supplier<WebDriver> driverSupplier, int headerToCut,
                                        int footerToCut, int correctDesiredWidthOn, int rightScrollToCut,
                                        ScreenshotControlFeignClient feignClient){
        this.operationSystem = operationSystem;
        this.driverSupplier = driverSupplier;
        this.headerToCut = headerToCut;
        this.footerToCut = footerToCut;
        this.correctDesiredWidthOn = correctDesiredWidthOn;
        this.rightScrollToCut = rightScrollToCut;
        this.feignClient = feignClient;
        tryCounter = MAX_TRY_ATTEMPTS;
    }

    @PostConstruct
    public void init(){
        System.out.println("AGENT STARTED FOR: " + operationSystem);
    }

    public void doScreenshot(String jobUUID, String url, Integer width, Integer height) throws IOException, InterruptedException {
        try {
            if (tryCounter > 0){
                tryScreenshot(jobUUID, url, width, height);
                tryCounter = MAX_TRY_ATTEMPTS;
            } else {
                tryCounter = MAX_TRY_ATTEMPTS;
            }
        } catch (Throwable trw){
            tryCounter--;
            System.out.println("RETRYING, has " + tryCounter + " attempts");
            tryScreenshot(jobUUID, url, width, height);
        }
    }

    private void tryScreenshot(String jobUUID, String url, Integer width, Integer height) throws IOException, InterruptedException {
        System.out.println("Preparing render screenshot from url = " + url + ", save to " + System.getProperty("user.dir"));
        WebDriver driver = initDriver();
        driver.get(url);
        System.out.println("Setting size to " + width + " x " + height);
        driver.manage().window().setSize(new Dimension(width, height));
        System.out.println("Do screenshot ");
        Screenshot s = new AShot()
                .shootingStrategy(viewportPasting(ShootingStrategies.cutting(1,0), aShotTimeout,10))
                .takeScreenshot(driver);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream is= new FileCacheImageOutputStream(os, new File("windows".equals(operationSystem) ? "C:\\Temp" : "/tmp" ));
        ImageIO.write(s.getImage(), "PNG", is);
        driver.quit();
        feignClient.sendResult(jobUUID, new ByteArrayResource(os.toByteArray()));
    }

    private WebDriver initDriver() {
        return driverSupplier.get();
    }
}
