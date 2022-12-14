package ru.taustudio.duckview.agent.screenshots;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import ru.taustudio.duckview.agent.job.JobDescription;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

	@Autowired
	ScreenshotControlFeignClient feignClient;

	@Value("${operationSystem:linux}")
	String operationSystem;
	@Value("${browser:firefox}")
	String driverType;
	final Integer aShotTimeout = 100;
	final Integer QUEUE_BOUND = 5;
	final Integer ADDING_TIMEOUT = 5;
	LinkedBlockingQueue<JobDescription> jobQueue = new LinkedBlockingQueue<>(QUEUE_BOUND);
	Thread thread = new Thread(){
		@Override
		public void run(){
			while(true){
				try {
					log.info("Taking job from queue");
					log.info("{}", Thread.currentThread());
					JobDescription job = jobQueue.take();
					log.info("Processing job {} from {}" , job.getJobId(), jobQueue.size() );
					doScreenshot(job.getJobId(), job.getUrl(), job.getWidth(), job.getHeight());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	};

	@PostConstruct
	public void init(){
		System.out.println("AGENT STARTED FOR: ");
		System.out.println("operationSystem = " + operationSystem);
		System.out.println("driverType = " + driverType);
		thread.start();
	}

	@Override
	public boolean addJobForProcessing(Long jobId, String url, Integer width, Integer height) throws InterruptedException {
		jobQueue.offer(new JobDescription(jobId, url, width, height), ADDING_TIMEOUT, TimeUnit.SECONDS);
		return (jobQueue.size() == 1);
	}

	public void doScreenshot(Long jobId, String url, Integer width, Integer height) throws IOException, InterruptedException {
		System.out.println("Preparing render screenshot from url = " + url + ", save to " + System.getProperty("user.dir"));
		RemoteWebDriver driver = initDriver();
		driver.get(url);
		System.out.println("Setting size to " + width + " x " + height);
		driver.manage().window().setSize(new Dimension(width, height));
		System.out.println("Do screenshot ");
		Screenshot s = new AShot()
				.shootingStrategy(ShootingStrategies.viewportPasting(aShotTimeout))
				.takeScreenshot(driver);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageOutputStream is= new FileCacheImageOutputStream(os, new File("linux".equals(operationSystem) ? "/tmp" : "C:\\Temp"));
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
		}
		throw new RuntimeException("DW: cannot find suitable driver for browser: " + driverType + " and OS: " + operationSystem);
	}
}
