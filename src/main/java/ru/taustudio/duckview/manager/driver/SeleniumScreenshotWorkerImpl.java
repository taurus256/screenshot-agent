package ru.taustudio.duckview.manager.driver;

import java.util.Map;
import java.util.function.Supplier;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import pazone.ashot.AShot;
import pazone.ashot.Screenshot;
import pazone.ashot.ShootingStrategies;
import ru.taustudio.duckview.manager.RenderException;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import ru.taustudio.duckview.shared.JobStatus;

@ConditionalOnProperty(value="driver",
        havingValue = "selenium",
        matchIfMissing = false)
public class SeleniumScreenshotWorkerImpl implements Worker {
    String operationSystem;

    final Integer aShotTimeout = 3000;

    private static String TEST_PAGE = "static/test_page.html";

    private final Supplier<WebDriver> driverSupplier;
    private int diff = 0;
    private int headerToCut;
    private int footerToCut;
    private int rightScrollToCut;
    private int correctDesiredWidthOn;
    private ScreenshotControlFeignClient feignClient;
    private final EurekaClient eurekaClient;

    public SeleniumScreenshotWorkerImpl(String operationSystem, Supplier<WebDriver> driverSupplier, int headerToCut,
        int footerToCut, int correctDesiredWidthOn, int rightScrollToCut,
        ScreenshotControlFeignClient feignClient, EurekaClient eurekaClient) {
        this.operationSystem = operationSystem;
        this.driverSupplier = driverSupplier;
        this.headerToCut = headerToCut;
        this.footerToCut = footerToCut;
        this.correctDesiredWidthOn = correctDesiredWidthOn;
        this.rightScrollToCut = rightScrollToCut;
        this.feignClient = feignClient;
        this.eurekaClient = eurekaClient;
    }

    @PostConstruct
    public void init(){
        System.out.println("WORKER STARTED");
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
        return null;
    }

    public void doScreenshot(String jobUUID, String url, Integer width, Integer height) throws RenderException{
        System.out.println("Preparing render screenshot from url = " + url + ", save to " + System.getProperty("user.dir"));

        try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            feignClient.changeJobStatus(jobUUID, JobStatus.IN_PROGRESS);
            WebDriver driver = initDriver();
            System.out.println("DIFF = " + diff);
            driver.manage().window().setSize(new Dimension(width + diff, height));
            driver.get(url);
            Thread.sleep(aShotTimeout);

            System.out.println("Do screenshot ");
            Screenshot s = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(aShotTimeout))
                .takeScreenshot(driver);

            ImageOutputStream is = new FileCacheImageOutputStream(os,
                new File("windows".equals(operationSystem) ? "C:\\Temp" : "/tmp"));
            ImageIO.write(s.getImage().getSubimage(0, 0, width, s.getImage().getHeight()), "PNG",
                is);
            driver.quit();
            feignClient.sendResult(jobUUID, new ByteArrayResource(os.toByteArray()));
        } catch (Throwable err){
            feignClient.changeJobStatus(jobUUID, JobStatus.ERROR, Map.of("description",
                StringUtils.defaultString(err.getMessage())));
            System.out.println("ERROR: " + err.getMessage());
            throw new RenderException(err.getMessage());
        }
    }

    WebDriver initDriver() {
        return driverSupplier.get();
    }
}
