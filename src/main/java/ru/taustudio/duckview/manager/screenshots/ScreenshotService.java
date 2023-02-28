package ru.taustudio.duckview.manager.screenshots;

import java.io.IOException;

public interface ScreenshotService {
	/** Return true, if job is in process; false, if job is in waiting state*/
	public boolean addJobForProcessing(Long jobId, String url, Integer width, Integer heigh) throws IOException, InterruptedException;
}
