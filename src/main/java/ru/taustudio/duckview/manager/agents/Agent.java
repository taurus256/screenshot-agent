package ru.taustudio.duckview.manager.agents;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import ru.taustudio.duckview.manager.driver.Worker;

public abstract class Agent {
  protected KafkaConsumer<String,String> consumer;

  private String agentName;
  private Worker worker;


  public Agent(String agentName, Worker worker){
    this.agentName = agentName;
    this.worker = worker;
  }

  public void init(){
    initConsumer();
    initWorker();
  }

  protected void initConsumer(){
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("group.id", "test");
    props.put("enable.auto.commit", "false");
    props.put("max.poll.records", "1");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Arrays.asList(agentName));
  }

  protected void initWorker(){
    worker.init();
  };

  public void processMessages(){
    ConsumerRecords<String, String> records = consumer.poll(Duration.of(1000, ChronoUnit.MILLIS));
    for (ConsumerRecord<String, String> record : records) {
      System.out.println("record = " + record);
    }
    consumer.commitSync();
  }

  protected void processRecord(){
    try {
      worker.doScreenshot(null, null,0,0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  };

  public String getAgentName() {
    return agentName;
  }
}
