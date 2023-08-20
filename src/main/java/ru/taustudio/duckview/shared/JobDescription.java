package ru.taustudio.duckview.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class JobDescription{
	Long jobId;
	String jobUUID;
	String url;
	Integer width;
	Integer height;
}