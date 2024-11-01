package ru.taustudio.duckview.manager.screenshots;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.taustudio.duckview.shared.JobStatus;

@FeignClient(name = "control-app", path="/job")
public interface ScreenshotControlFeignClient {
	@PutMapping("/{jobUUID}")
	public String sendResult(@PathVariable String jobUUID, @RequestBody ByteArrayResource bars);

	@PutMapping("/{jobUUID}/status/{jobStatus}")
	public void changeJobStatus(@PathVariable String jobUUID, @PathVariable JobStatus jobStatus);
	@PutMapping("/{jobUUID}/status/{jobStatus}")
	public void changeJobStatus(@PathVariable String jobUUID, @PathVariable JobStatus jobStatus,
			@RequestBody Map<String, String> description);
}
