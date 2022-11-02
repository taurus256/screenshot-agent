package ru.taustudio.duckview.agent.screenshots;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "control-app", path="/job")
public interface ScreenshotControlFeignClient {
	@PutMapping("/{jobId}")
	public String sendResult(@PathVariable Long jobId, @RequestBody ByteArrayResource bars);
}
