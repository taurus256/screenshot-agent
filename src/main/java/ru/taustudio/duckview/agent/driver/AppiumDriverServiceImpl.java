package ru.taustudio.duckview.agent.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pazone.ashot.AShot;
import pazone.ashot.Screenshot;
import pazone.ashot.ShootingStrategies;
import pazone.ashot.ShootingStrategy;
import pazone.ashot.cutter.FixedCutStrategy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


import ru.taustudio.duckview.agent.screenshots.ScreenshotControlFeignClient;

@Component
@ConditionalOnProperty(value="driver",
        havingValue = "appium",
        matchIfMissing = false)
public class AppiumDriverServiceImpl implements ScreenshotDriverService {


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

    public AppiumDriverServiceImpl() {
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

    @Scheduled(fixedRate = 30*60*1000)
    public void watchForSession(){
        if (Instant.now().minusSeconds(lastCommandTime.get()).getEpochSecond() > 30*60) {
            try {
                driver.get("about:blank");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void initDriver() throws InterruptedException {
        System.out.println("Device: " + device);
        System.out.println("INITIALIZATION...");
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "16.1");
        desiredCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");
        desiredCapabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Safari");
        desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, device.getSystemName());
        desiredCapabilities.setCapability("wdaStartupRetries", "4");
        desiredCapabilities.setCapability("iosInstallPause", "30000");
        desiredCapabilities.setCapability("wdaStartupRetryInterval", "20000");
        // время, в течение которого держится сессия
        desiredCapabilities.setCapability("newCommandTimeout", "3600");
        URL url = null;
        try {
            url = new URL("http://127.0.0.1:4723");
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
        ImageOutputStream is = new FileCacheImageOutputStream(os, new File("linux".equals(operationSystem) ? "/tmp" : "C:\\Temp"));
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
        return ShootingStrategies.viewportRetina(500,
                new FixedCutStrategy(20,0), 2f);
    }

    //iPad Air (5th generation)
    private ShootingStrategy iPadAirShootingStrategy(){
        return ShootingStrategies.viewportRetina(3000, 76, 0, 2.0f);
    }

    //iPhone Pro Max
    private ShootingStrategy iPhoneProMaxShootingStrategy(){
        return ShootingStrategies
                .viewportRetina(500, new FixedCutStrategy(58,122), 3f);
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
        driver.context("NATIVE_APP");
        if (driver.findElements(By.xpath("//XCUIElementTypeOther[@name=\"CapsuleViewController\"]/XCUIElementTypeOther[2]")).size() > 0){
            clickElement("//XCUIElementTypeOther[@name=\"CapsuleViewController\"]/XCUIElementTypeOther[2]");
        }
        clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
        clickElement("//XCUIElementTypeButton[@name=\"Close\"]");
        clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
        setNewTabContext(driver.getContextHandles());
    }

    private void clickElement(String elementPath ) throws InterruptedException {
        WebElement element= driver.findElement(By.xpath(elementPath));
        Thread.sleep(500);
        element.click();
    }

    private void setNewTabContext(Set<String> newHandles){
        System.out.println("setting new context, availible:" + driver.getContextHandles());
        System.out.println("Current:" + driver.getContext());
        newHandles.removeAll(initialHandles);
        if (newHandles.size() == 1) {
            System.out.println("SET = " + newHandles.stream().findAny().get());
            driver.context(newHandles.stream().findAny().get());
        }else
            throw new IllegalArgumentException();
    }
}
