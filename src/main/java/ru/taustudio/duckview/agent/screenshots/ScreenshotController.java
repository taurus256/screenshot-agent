package ru.taustudio.duckview.agent.screenshots;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class ScreenshotController {
	@Autowired
	ScreenshotService screenshotService;

	@GetMapping(value = "/screenshot")
	public boolean processScreenshot(@RequestParam(required = true) Long jobId,
			@RequestParam(required = true) String url,
								 @RequestParam(required = false) Integer width,
								 @RequestParam(required = false) Integer height)
			throws IOException, InterruptedException {
		System.out.println("url = " + url);
		System.out.println("width = " + width);
		System.out.println("height = " + height);
		if (width == null) width = 1024;
		if (height == null) height = 768;
		return screenshotService.addJobForProcessing(jobId, url, width, height);
	}
}
