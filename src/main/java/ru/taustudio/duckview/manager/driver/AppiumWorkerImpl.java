package ru.taustudio.duckview.manager.driver;

import static pazone.ashot.ShootingStrategies.cutting;
import static pazone.ashot.ShootingStrategies.scaling;
import static pazone.ashot.ShootingStrategies.simple;
import static pazone.ashot.ShootingStrategies.viewportPasting;

import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pazone.ishot.IShot;
import pazone.ishot.Screenshot;
import pazone.ishot.ShootingStrategies;
import pazone.ishot.ShootingStrategy;
import pazone.ishot.cutter.FixedCutStrategy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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


import ru.taustudio.duckview.agent.aop.RetrytOnFailure;
import ru.taustudio.duckview.agent.screenshots.ScreenshotControlFeignClient;

@Component
@ConditionalOnProperty(value="driver",
        havingValue = "appium",
        matchIfMissing = false)
@Slf4j
public class AppiumWorkerImpl implements Worker {


    public static final String NATIVE_APP = "NATIVE_APP";
    public static final int INTERSECTION = 40;
    @Value("${appium.port:4723}")
    String appiumPort;
    @Value("${operationSystem:linux}")
    String operationSystem;
    @Value("${browser:firefox}")
    String driverType;
    @Value("${device:iPhonePro}")
    Device device;
    final Integer aShotTimeout = 100;

    private static IOSDriver driver;
    Set<String> initialHandles;

    @Autowired
    ScreenshotControlFeignClient feignClient;

    private final AtomicLong lastCommandTime = new AtomicLong(Instant.now().getEpochSecond());

    public AppiumWorkerImpl() {
    }

    @PostConstruct
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
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "16.2");
        desiredCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");
        desiredCapabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Safari");
        desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, device.getSystemName());
        desiredCapabilities.setCapability("wdaStartupRetries", "10");
        desiredCapabilities.setCapability("iosInstallPause", "30000");
        desiredCapabilities.setCapability("wdaStartupRetryInterval", "20000");
        // время, в течение которого держится сессия
        desiredCapabilities.setCapability("newCommandTimeout", "3600");
        URL url = null;
        try {
            url = new URL("http://127.0.0.1:".concat(appiumPort));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        driver = new IOSDriver(url, desiredCapabilities);
        System.out.println("CONFIGURE BROWSER...");
        initialHandles = driver.getContextHandles();
        enterPrivateMode();
        setNewTabContext(driver.getContextHandles());
        System.out.println("READY TO WORK");
    }

    @PreDestroy
    public void destroy() {
        //close the app.
        if (driver != null)
            driver.quit();
    }

    @Override
    public void doScreenshot(Long jobId, String url, Integer width, Integer height) throws Exception {
        lastCommandTime.set(Instant.now().getEpochSecond());

        System.out.println("Preparing render screenshot from url = " + url + ", save to " + System.getProperty("user.dir"));
        ((WebDriver)driver).get(url);
        System.out.println("Do screenshot ");

        Screenshot s = new AShot()
                .shootingStrategy(getStrategyForDevice(device))
                .takeScreenshot(driver);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream is = new FileCacheImageOutputStream(os, new File("windows".equals(operationSystem) ? "C:\\Temp" : "/tmp"));
        ImageIO.write(s.getImage(), "PNG", is);
        feignClient.sendResult(jobId, new ByteArrayResource(os.toByteArray()));
        closePrivateTab();
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
        return viewportRetinaIntersect(500, new FixedCutStrategy(58,122), 3f);
    }

    public void enterPrivateMode() throws InterruptedException {
        driver.context("NATIVE_APP").switchTo();
        clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
        clickElement("//XCUIElementTypeButton[@name=\"TabGroupsButton\"]");
        clickElement("//XCUIElementTypeCell[@name=\"TabGroupCell?Title=Private&isPrivate=true\"]");
        clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
    }

    public void closePrivateTab() throws InterruptedException {
        System.out.println("Contexts:" + driver.getContextHandles());
        System.out.println("Current:" + driver.getContext());
        driver.context(NATIVE_APP).switchTo();
        System.out.println("New current:" + driver.getContext());
//        if (driver.findElements(By.xpath("//XCUIElementTypeOther[@name=\"CapsuleViewController\"]/XCUIElementTypeOther[2]")).size() > 0){
//            clickElement("//XCUIElementTypeOther[@name=\"CapsuleViewController\"]/XCUIElementTypeOther[2]");
//        }
        clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
        clickElement("//XCUIElementTypeButton[@name=\"Close\"]");
        clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
        setNewTabContext(driver.getContextHandles());
    }

    private void clickElement(String elementPath ) throws InterruptedException {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(120))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(elementPath)));
        element.click();
    }

    private void setNewTabContext(Set<String> newHandles){
        System.out.println("setting new context, available:" + driver.getContextHandles());
        System.out.println("Current:" + driver.getContext());
        newHandles.removeAll(initialHandles);
        if (newHandles.size() == 1) {
            System.out.println("SET = " + newHandles.stream().findAny().get());
            driver.context(newHandles.stream().findAny().get());
        }else
            throw new IllegalArgumentException();
    }

    // переопределенный стратегии AShot с управлением наложением
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
