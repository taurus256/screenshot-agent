package ru.taustudio.duckview.agent;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.Before;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.taustudio.duckview.agent.driver.Device;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

@RunWith(JUnit4.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringSeleniumAgentApplicationTests {
	private static IOSDriver driver;
	private static Device device = Device.IPHONE_SE;
	private static String webWiewHandle;

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
		webWiewHandle = driver.getContextHandles().stream().filter(h -> !"NATIVE_APP".equals(h)).findAny().get();
	}

	@Test
	@Order(10)
	public void enterPrivateMode() throws InterruptedException {
		driver.context("NATIVE_APP");
		clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
		clickElement("//XCUIElementTypeButton[@name=\"TabGroupsButton\"]");
		clickElement("//XCUIElementTypeCell[@name=\"TabGroupCell?Title=Private&isPrivate=true\"]");
		clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
	}

	@Test
	@Order(20)
	public void openSite() throws InterruptedException {
		((WebDriver)driver).get("http://mypsy.org");
		System.out.println("RETRIEVE WEB SITE");
	}

	@Test
	@Order(30)
	public void closePrivateTab() throws InterruptedException {
		driver.context("NATIVE_APP");
		clickElement("//XCUIElementTypeOther[@name=\"CapsuleViewController\"]/XCUIElementTypeOther[2]");
		clickElement("//XCUIElementTypeButton[@name=\"TabOverviewButton\"]");
		clickElement("//XCUIElementTypeButton[@name=\"Close\"]");
		clickElement("//XCUIElementTypeButton[@name=\"TabViewDoneButton\"]");
	}

	private void clickElement(String elementPath ) throws InterruptedException {
		WebElement element= (WebElement)driver.findElement(By.xpath(elementPath));
		System.out.println("e1ement = " + element);
		Thread.sleep(500);
		element.click();
	}
}
