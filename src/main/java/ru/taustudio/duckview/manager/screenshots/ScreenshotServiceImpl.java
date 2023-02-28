package ru.taustudio.duckview.manager.screenshots;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.taustudio.duckview.manager.driver.Worker;
import ru.taustudio.duckview.manager.job.JobDescription;

import javax.annotation.PostConstruct;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

	@Autowired
	ScreenshotControlFeignClient feignClient;

	@Autowired
	Worker driverService;

	final Integer QUEUE_BOUND = 5;
	final Integer ADDING_TIMEOUT = 5;

	@PostConstruct
	public void init(){
		thread.start();
	}


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
					driverService.doScreenshot(job.getJobId(), job.getUrl(), job.getWidth(), job.getHeight());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	};


	@Override
	public boolean addJobForProcessing(Long jobId, String url, Integer width, Integer height) throws InterruptedException {
		jobQueue.offer(new JobDescription(jobId, url, width, height), ADDING_TIMEOUT, TimeUnit.SECONDS);
		return (jobQueue.size() == 1);
	}

}
