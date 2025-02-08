package ru.taustudio.duckview.manager.driver;

import static pazone.ashot.ShootingStrategies.cutting;
import static pazone.ashot.ShootingStrategies.scaling;
import static pazone.ashot.ShootingStrategies.simple;
import static pazone.ashot.ShootingStrategies.viewportPasting;

import feign.FeignException;
import io.appium.java_client.ios.IOSDriver;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Scheduled;
import io.appium.java_client.ios.options.XCUITestOptions;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


import pazone.ashot.AShot;
import pazone.ashot.Screenshot;
import pazone.ashot.ShootingStrategy;
import pazone.ashot.cutter.CutStrategy;
import pazone.ashot.cutter.FixedCutStrategy;
import ru.taustudio.duckview.manager.aop.RetrytOnFailure;
import ru.taustudio.duckview.manager.screenshots.ScreenshotControlFeignClient;
import ru.taustudio.duckview.shared.JobStatus;

@Slf4j
public class AppiumWorkerImpl implements Worker {

    public static final String NATIVE_APP = "NATIVE_APP";
    public static final int INTERSECTION = 40;
    String appiumPort;
    String operationSystem;
    Device device;

    private IOSDriver driver;
    Set<String> initialHandles;

    @Autowired
    ScreenshotControlFeignClient feignClient;

    private final AtomicLong lastCommandTime = new AtomicLong(Instant.now().getEpochSecond());

    public AppiumWorkerImpl(String appiumPort, String operationSystem, Device device, ScreenshotControlFeignClient feignClient) {
        this.appiumPort = appiumPort;
        this.operationSystem = operationSystem;
        this.device = device;
        this.feignClient = feignClient;
    }

    public void init() {
        try {
            initDriver();
        } catch (InterruptedException iex){
            System.out.println("Application was interrupted");
            System.exit(0);
        }
    }

    @Scheduled(fixedRate = 10*60*1000)
    public void watchForSession(){
        log.info("CALLING REFRESH");
        if (NATIVE_APP.equals(driver.getContext())){
            log.info("REFRESH DELAYING. WRONG CONTEXT.");
        }
        if (Instant.now().minusSeconds(lastCommandTime.get()).getEpochSecond() > 10*60) {
            try {
                driver.get("about:blank");
                log.info("REFRESH OK");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @RetrytOnFailure(3)
    private void initDriver() throws InterruptedException {
        System.out.println("Device: " + device);
        System.out.println("INITIALIZATION...");

        XCUITestOptions options = new XCUITestOptions()
                .setPlatformName("iOS")
                .setPlatformVersion("16.4")
                .setAutomationName("XCUITest")
                .setDeviceName(device.getSystemName());
        options.setCapability("appium:usePreinstalledWDA", true);
        options.setCapability("appium:webviewConnectTimeout", 120000);

        options.setCapability("wdaStartupRetries", "10");
        options.setCapability("iosInstallPause", "30000");
        options.setCapability("wdaStartupRetryInterval", "20000");
        // время, в течение которого держится сессия
        options.setCapability("newCommandTimeout", "3600");
        options.setCapability("browserName", "Safari");
        URL url;
        try {
            url = new URL("http://127.0.0.1:".concat(appiumPort));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        driver = new IOSDriver(url, options);
        System.out.println("CONFIGURE BROWSER...");
        initialHandles = driver.getContextHandles();
        enterPrivateMode();
        setNewTabContext(driver.getContextHandles());
        System.out.println("READY TO WORK");
    }

    @Override
    public void destroy() {
        System.out.println("Destroying worker for " + device.name());
        //close the app.
        if (driver != null) {
            driver.quit();
        }
    }

    @Override
    public void doScreenshot(String jobUUID, String url, Integer width, Integer height){
        lastCommandTime.set(Instant.now().getEpochSecond());
        try {
            feignClient.changeJobStatus(jobUUID, JobStatus.IN_PROGRESS);
            System.out.println(
                    "Preparing render screenshot from url = " + url + ", save to " + System.getProperty(
                            "user.dir"));
            ((WebDriver) driver).get(url);
            System.out.println("Do screenshot ");

            Screenshot s = new AShot()
                    .shootingStrategy(getStrategyForDevice(device))
                    .takeScreenshot(driver);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageOutputStream is = new FileCacheImageOutputStream(os,
                    new File("windows".equals(operationSystem) ? "C:\\Temp" : "/tmp"));
            ImageIO.write(s.getImage(), "PNG", is);
            System.out.println("Screenshot was taken");
            feignClient.sendResult(jobUUID, new ByteArrayResource(os.toByteArray()));
            System.out.println("Screenshot was sent");
        } catch (FeignException.BadRequest brex) {
            System.out.println("SERVER REQUEST ERROR: " + brex.getMessage());
            System.out.println("The job is probably outdated and has been deleted");
        } catch (Exception ex){
            feignClient.changeJobStatus(jobUUID, JobStatus.ERROR,
                Map.of("description", StringUtils.defaultString(ex.getMessage())));
            System.out.println("ERROR: " + ex.getMessage());
            closePrivateTab();
        }
    }

    private ShootingStrategy getStrategyForDevice(Device device) {
        switch(device){
            case IPHONE_SE: return iPhoneSEShootingStrategy();
            case IPAD: return iPadAirShootingStrategy();
            case IPHONE_PRO: return iPhoneProMaxShootingStrategy();
            }
        throw new IllegalArgumentException("Cannot select strategy for device " + device);
    }

    //iPhone SE (3rd generation)
    private ShootingStrategy iPhoneSEShootingStrategy(){
        return viewportRetinaIntersect(500,
                new FixedCutStrategy(20,0), 2f);
    }

    //iPad Air (5th generation)
    private ShootingStrategy iPadAirShootingStrategy(){
        return viewportRetinaIntersect(3000, 76, 0, 2.0f);
    }

    //iPhone Pro Max
    private ShootingStrategy iPhoneProMaxShootingStrategy(){
        return viewportRetinaIntersect(500, new FixedCutStrategy(59,122), 3f);
    }

    public void enterPrivateMode() throws InterruptedException {
        driver.context("NATIVE_APP").switchTo();
        clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
        clickElement("//XCUIElementTypeButton[@name=\"TabGroupsButton\"]");
        clickElement("//XCUIElementTypeCell[@name=\"TabGroupCell?Title=Private&isPrivate=true\"]");
        clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
    }

    public void closePrivateTab() {
        try {
        printContext();
        System.out.println("Start closing private tab. Prepare to set native context");
        System.out.println("Contexts:" + driver.getContextHandles());
        System.out.println("Current:" + driver.getContext());
        if (!"NATIVE_APP".equals(driver.getContext())) {
            driver.context(NATIVE_APP).switchTo();
        }

        System.out.println("New current context is:" + driver.getContext());
//        if (driver.findElements(By.xpath("//XCUIElementTypeOther[@name=\"CapsuleViewController\"]/XCUIElementTypeOther[2]")).size() > 0){
//            clickElement("//XCUIElementTypeOther[@name=\"CapsuleViewController\"]/XCUIElementTypeOther[2]");
//        }

        clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
        clickElement("//XCUIElementTypeButton[@name=\"Close\"]");
        clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
        System.out.println("Tab was closed. Try to set browser context");
        setNewTabContext(driver.getContextHandles());
        System.out.println("Browser context has been set");
        printContext();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void clickElement(String elementPath ) throws InterruptedException {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(120))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(elementPath)));
        element.click();
    }

    private void setNewTabContext(Set<String> newHandles){
        printContext();
        System.out.println("Start setting new browser context, available:" + driver.getContextHandles());
        System.out.println("Current:" + driver.getContext());
        System.out.println("Initial handles:" + initialHandles);
        final String newContextName;
        if (newHandles.size() != initialHandles.size()) {
            System.out.println("New web context appears, switching to it");
            newHandles.removeAll(initialHandles);
            if (newHandles.size() > 1){
                System.out.println("Too many web contexts, cannot select proper between them");
                throw new IllegalArgumentException();
            }
            newContextName = newHandles.stream().findAny().orElseThrow(
                () -> new IllegalArgumentException(
                    "Try switching to new context, but no one was detected"));
        } else {
            System.out.println("No new web context detected, try switching to existing one");
            newContextName = newHandles.stream().filter((ctxName) -> !"NATIVE_APP".equals(ctxName)).findAny().orElseThrow(
                () -> new IllegalArgumentException(
                    "Try switching to existing context, but no one was detected"));
        }
        driver.context(newContextName);
        printContext();
    }

    private void printContext() {
        System.out.println("Current context is " + driver.getContext());
    }

    // переопределенные
    public static ShootingStrategy viewportNonRetinaIntersect(ShootingStrategy shootingStrategy, int scrollTimeout,
        CutStrategy cutStrategy) {
        return viewportPasting(cutting(shootingStrategy, cutStrategy), scrollTimeout, INTERSECTION);
    }

    public static ShootingStrategy viewportRetinaIntersect(ShootingStrategy shootingStrategy, int scrollTimeout,
        CutStrategy cutStrategy, float dpr) {
        ShootingStrategy scalingDecorator = scaling(shootingStrategy, dpr);
        return viewportNonRetinaIntersect(scalingDecorator, scrollTimeout, cutStrategy);
    }


    public static ShootingStrategy viewportRetinaIntersect(int scrollTimeout, int headerToCut, int footerToCut, float dpr) {
        return viewportRetinaIntersect(simple(), scrollTimeout, new FixedCutStrategy(headerToCut, footerToCut), dpr);
    }

    public static ShootingStrategy viewportRetinaIntersect(int scrollTimeout, CutStrategy cutStrategy, float dpr) {
        return viewportRetinaIntersect(simple(), scrollTimeout, cutStrategy, dpr);
    }
}
