package ru.taustudio.duckview.manager.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class JobDescription {
	Long jobId;
	String url;
	Integer width;
	Integer height;
}
