package ru.taustudio.duckview.manager.screenshots;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "control-app", path="/job")
public interface ScreenshotControlFeignClient {
	@PutMapping("/{jobUUID}")
	public String sendResult(@PathVariable String jobUUID, @RequestBody ByteArrayResource bars);
}
