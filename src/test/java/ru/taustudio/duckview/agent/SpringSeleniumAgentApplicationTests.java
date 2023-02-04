package ru.taustudio.duckview.agent;

import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import pazone.ishot.IShot;
import pazone.ishot.Screenshot;
import pazone.ishot.ShootingStrategies;
import pazone.ishot.cutter.FixedCutStrategy;
import ru.taustudio.duckview.agent.driver.Device;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

@RunWith(JUnit4.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringSeleniumAgentApplicationTests {
	private static IOSDriver driver;
	private static final Device device = Device.IPHONE_SE;
	private static String webWiewHandle;
	private static Set<String> initialHandles;

	@BeforeAll
	public static void contextLoads() {
		System.out.println("Device: " + device);
		System.out.println("INITIALIZATION TEST...");
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
		// получаем имя режима, который не NATIVE_APP
		initialHandles = driver.getContextHandles();
	}

	@Test
	@Order(10)
	public void enterPrivateMode() throws InterruptedException {
		driver.context("NATIVE_APP").switchTo();
		clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
		clickElement("//XCUIElementTypeButton[@name=\"TabGroupsButton\"]");
		clickElement("//XCUIElementTypeCell[@name=\"TabGroupCell?Title=Private&isPrivate=true\"]");
		clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
		System.out.println("driver.getContextHandles() = " + driver.getContextHandles());
		setNewTabContext(driver.getContextHandles());
	}

	@Test
	@Order(20)
	public void openSite() throws InterruptedException, IOException {
		((WebDriver)driver).get("http://mypsy.org");
		System.out.println("RETRIEVE WEB SITE");
		Screenshot s = new IShot()
				.shootingStrategy(ShootingStrategies.viewportRetina(500,
						new FixedCutStrategy(20,0), 2f))
				.takeScreenshot(driver);
		System.out.println("s.getImage().getHeight() = " + s.getImage().getHeight());
		FileOutputStream os = new FileOutputStream("/tmp/png.png");
		ImageOutputStream is = new FileCacheImageOutputStream(os, new File("/tmp"));
		ImageIO.write(s.getImage(), "PNG", is);
		is.close();
		os.close();
		closePrivateTab();
	}

	@Test
	@Order(25)
	public void openSite2() throws InterruptedException, IOException {
		((WebDriver)driver).get("http://mypsy.org");
		System.out.println("RETRIEVE WEB SITE");
		Screenshot s = new IShot()
				.shootingStrategy(ShootingStrategies.viewportRetina(2000,
						new FixedCutStrategy(20,0), 2f))
				.takeScreenshot(driver);
		System.out.println("s.getImage().getHeight() = " + s.getImage().getHeight());
		FileOutputStream os = new FileOutputStream("/tmp/png.png");
		ImageOutputStream is = new FileCacheImageOutputStream(os, new File("/tmp"));
		ImageIO.write(s.getImage(), "PNG", is);
		is.close();
		os.close();
		closePrivateTab();
	}


	public void closePrivateTab() throws InterruptedException {
		System.out.println("Contexts:" + driver.getContextHandles());
		System.out.println("Current:" + driver.getContext());
		driver.context("NATIVE_APP").switchTo();
		System.out.println("Current after switching:" + driver.getContext());
		clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
		clickElement("//XCUIElementTypeButton[@name=\"Close\"]");
		clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
		setNewTabContext(driver.getContextHandles());
	}

	private void clickElement(String elementPath ) throws InterruptedException {
		WebElement element= driver.findElement(By.xpath(elementPath));
		System.out.println("e1ement = " + element);
		Thread.sleep(500);
		element.click();
	}

	private void setNewTabContext(Set<String> newHandles){
		System.out.println("setting new context, availible:" + driver.getContextHandles());
		System.out.println("Current:" + driver.getContext());
		newHandles.removeAll(initialHandles);
		if (newHandles.size() == 1) {
			System.out.println("SET = " + newHandles.stream().findAny().get());
			driver.context(newHandles.stream().findAny().get()).switchTo();
		}else
			throw new IllegalArgumentException();
	}
}
