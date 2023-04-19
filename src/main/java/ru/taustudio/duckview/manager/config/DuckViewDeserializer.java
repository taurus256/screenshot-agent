package ru.taustudio.duckview.manager.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.kafka.support.serializer.JsonDeserializer;

public class DuckViewDeserializer<JobDescription> extends JsonDeserializer<JobDescription> {

  public DuckViewDeserializer() {
    super();
    Map<String, String> configs = new HashMap<>();
    configs.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES,
        "*");
    this.configure(configs, false);
  }
}
