package ru.taustudio.duckview.manager.agents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import ru.taustudio.duckview.manager.RenderException;
import ru.taustudio.duckview.manager.aop.RetrytOnFailure;
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
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  @Value("${spring.kafka.bootstrap-servers}")
  private String boostrapServers;

  @Value("${spring.kafka.properties.sasl.jaas.config}")
  private String saslConfig;

  @Value("${spring.kafka.properties.sasl.mechanism}")
  private String saslMechanism;

  @Value("${spring.kafka.properties.security.protocol}")
  private String saslProtocol;

  public Agent(String agentName, Worker worker){
    this.agentName = agentName;
    this.worker = worker;
  }

  @RetrytOnFailure(3)
  public void init(){
    initAgent();
    initConsumer();
    initWorker();
  }

  abstract protected void initAgent();

  public void destroy(){
    System.out.println("Shutdown agent: " + getAgentName());
  };

  protected void initConsumer(){
    Properties props = new Properties();
    props.put("bootstrap.servers", boostrapServers);
    props.put("group.id", "listeners");
    props.put("enable.auto.commit", "false");
    props.put("max.poll.records", "1");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "ru.taustudio.duckview.manager.config.DuckViewDeserializer");
    props.put("sasl.jaas.config", saslConfig);
    props.put("sasl.mechanism", saslMechanism);
    props.put("security.protocol", saslProtocol);
    props.put("jaas.enabled", true);

    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Arrays.asList(agentName));
  }

  protected void initWorker(){
    worker.init();
  };

  public void processMessages(){
    try {
      ConsumerRecords<String, JobDescription> records = consumer.poll(Duration.of(1000, ChronoUnit.MILLIS));
      for (ConsumerRecord<String, JobDescription> record : records) {
        System.out.println("record = " + record);

        processRecord(record.value());
      }
      consumer.commitSync();
    } catch (Throwable trw){
      System.out.println("ERROR: " + trw.getMessage());
      trw.printStackTrace();
    }
  }

  protected void processRecord(JobDescription job){

    Callable<Integer> workerCall = () -> {
      worker.doScreenshot(job.getJobUUID(), job.getUrl(), job.getWidth(), job.getHeight());
      return 0;
    };

    try {
     var future = executorService.submit( workerCall);
      future.get(60, TimeUnit.SECONDS);
      System.out.println("future returned for " + getAgentName());
    } catch (TimeoutException | ExecutionException | InterruptedException  e) {
      System.out.println("Error where rendering in agent " + getAgentName());
      System.out.println(e.getMessage());
      try {
        closeBrowser();
      } catch (IOException ex) {
        System.out.println("Error where attempting to stop the browser process in agent " + getAgentName());
      }
    }
  };

  public String getAgentName() {
    return agentName;
  }

  public Worker getWorker() {
    return worker;
  }

  protected void closeBrowser() throws IOException {
    Process process = Runtime.getRuntime().exec("taskkill /f /IM msedge.exe");
    new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(
        System.out::println);
  }
}
