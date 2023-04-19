package ru.taustudio.duckview.manager.agents;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.openqa.selenium.WebDriver;
import ru.taustudio.duckview.manager.driver.Worker;
import ru.taustudio.duckview.shared.JobDescription;

/**Абстрактный класс. инкапсулирует работу с конкретным браузером
 * Имеет три абстрактных метода, которые нужно определить:
 * initAgent - инициализирует агент браузера с помощью WebDriverManager. Обновляет версию и т.п.
 * initConsumer - инициализирует консьюмер Кафки. через который агент получает задания
 * initWorker - инициализирует обработчик (класс, выполняющий собственно обработку заданий с помощью агента)*/
public abstract class Agent {
  protected KafkaConsumer<String,JobDescription> consumer;

  private String agentName;
  private Worker worker;


  public Agent(String agentName, Worker worker){
    this.agentName = agentName;
    this.worker = worker;
  }

  public void init(){
    initAgent();
    initConsumer();
    initWorker();
  }

  abstract protected void initAgent();

  protected void initConsumer(){
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("group.id", "test");
    props.put("enable.auto.commit", "false");
    props.put("max.poll.records", "1");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "ru.taustudio.duckview.manager.config.DuckViewDeserializer");
    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Arrays.asList(agentName));
  }

  protected void initWorker(){
    worker.init();
  };

  public void processMessages(){
    ConsumerRecords<String, JobDescription> records = consumer.poll(Duration.of(1000, ChronoUnit.MILLIS));
    for (ConsumerRecord<String, JobDescription> record : records) {
      System.out.println("record = " + record);

      processRecord(record.value());
    }
    consumer.commitSync();
  }

  protected void processRecord(JobDescription job){
    try {
      worker.doScreenshot(job.getJobUUID(), job.getUrl(),job.getWidth(),job.getHeight());
    } catch (Exception e) {
      e.printStackTrace();
    }
  };

  public String getAgentName() {
    return agentName;
  }
}
