package ru.taustudio.duckview.manager;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.agents.Agent;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaListener {

    private final List<Agent> agentList;
    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    public void process(){
        for (Agent a: agentList){
            log.info("Start initialization of {}", a.getAgentName());
            a.init();
            log.info("Initialization of {} - OK", a.getAgentName());
        }
        System.out.println("Start consume messages frmn Kafka");
        while(true){
            for (Agent a: agentList){
                a.processMessages();
            }
        }
    }
}
