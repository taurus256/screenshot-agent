package ru.taustudio.duckview.manager.driver;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import java.util.Map;

import feign.FeignException;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariDriverService;
import org.springframework.core.io.ByteArrayResource;
import pazone.ashot.AShot;
import pazone.ashot.Screenshot;
import ru.taustudio.duckview.manager.SafariViewportPastingDecorator;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import ru.taustudio.duckview.shared.JobStatus;

import static pazone.ashot.ShootingStrategies.simple;

public class SafariDesktopWorkerImpl implements Worker {

    public static final int MAX_TRY_ATTEMPTS = 2;
    private final Supplier<WebDriver> driverSupplier;

    private final int headerToCut;
    private final int footerToCut;
    private final int correctDesiredWidthOn;
    private final int rightScrollToCut;
    String operationSystem;
    final Integer aShotTimeout = 3000;
    private static String TEST_PAGE = "static/test_page.html";
    private int diff = 0;
    int tryCounter;
    ScreenshotControlFeignClient feignClient;
    private final EurekaClient eurekaClient;

    public SafariDesktopWorkerImpl(String operationSystem, Supplier<WebDriver> driverSupplier, int headerToCut,
                                        int footerToCut, int correctDesiredWidthOn, int rightScrollToCut,
                                        ScreenshotControlFeignClient feignClient,
        EurekaClient eurekaClient){
        this.operationSystem = operationSystem;
        this.driverSupplier = driverSupplier;
        this.headerToCut = headerToCut;
        this.footerToCut = footerToCut;
        this.correctDesiredWidthOn = correctDesiredWidthOn;
        this.rightScrollToCut = rightScrollToCut;
        this.feignClient = feignClient;
      this.eurekaClient = eurekaClient;
      tryCounter = MAX_TRY_ATTEMPTS;
    }

    @PostConstruct
    public void init(){
        System.out.println("AGENT STARTED FOR: " + operationSystem);
        setScreenDiff();
    }

    private void setScreenDiff(){
        int width = 1024;
        int height = 768;

        WebDriver driver = initDriver();
        driver.manage().window().setSize(new Dimension(width, height ));

        driver.get(getControlAppUrl() + TEST_PAGE);

        WebElement html = driver.findElement(By.tagName("html"));
        int inner_width = html.getRect().getWidth();

        this.diff = width - inner_width;
        driver.quit();
    }

    private String getControlAppUrl(){
        for (Application app : eurekaClient.getApplications().getRegisteredApplications()) {
            if ("CONTROL-APP".equals(app.getName())) {
                return app.getInstances().get(0).getHomePageUrl();
            }
        }
        throw new IllegalStateException("Cannot get CONTROL-APP address from Eureka!");
    }

    public void doScreenshot(String jobUUID, String url, Integer width, Integer height) {
        try {
            if (tryCounter > 0){
                tryScreenshot(jobUUID, url, width, height);
            } else {
                tryCounter = MAX_TRY_ATTEMPTS;
                feignClient.changeJobStatus(jobUUID, JobStatus.ERROR,
                    Map.of("description", "ERROR: max attempts count exceed!"));
                System.out.println("ERROR: max attempts count exceed!");
            }
        } catch (Throwable trw){
            tryCounter--;
            System.out.println("RETRYING, " + tryCounter + " attempts left");
            doScreenshot(jobUUID, url, width, height);
        }
    }

    private void tryScreenshot(String jobUUID, String url, Integer width, Integer height) throws IOException {
        try {
            System.out.println("Preparing render screenshot from url = " + url + ", save to " + System.getProperty("user.dir"));
            feignClient.changeJobStatus(jobUUID, JobStatus.IN_PROGRESS);
            WebDriver driver = initDriver();
            driver.get(url);
            System.out.println("Setting size to " + width + " x " + height + " with diff " + diff);
            driver.manage().window().setSize(new Dimension(width + diff, height));
            System.out.println("Do screenshot ");
            Screenshot s = new AShot()
                    .shootingStrategy(new SafariViewportPastingDecorator(simple())
                            .withScrollTimeout(aShotTimeout)
                            .withIntersection(15))
                    .takeScreenshot(driver);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageOutputStream is = new FileCacheImageOutputStream(os, new File("windows".equals(operationSystem) ? "C:\\Temp" : "/tmp"));
            ImageIO.write(s.getImage().getSubimage(0, 0, width, s.getImage().getHeight()), "PNG", is);
            driver.quit();
            feignClient.sendResult(jobUUID, new ByteArrayResource(os.toByteArray()));
        } catch (FeignException.BadRequest brex) {
                System.out.println("SERVER REQUEST ERROR: " + brex.getMessage());
                System.out.println("The job is probably outdated and has been deleted");
        }
    }

    private WebDriver initDriver() {
        return driverSupplier.get();
//        SafariDriverService service = new SafariDriverService.Builder()
//                .withLogging(true)
//                .build();
//
//        return new SafariDriver(service);
    }
}
