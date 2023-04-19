package ru.taustudio.duckview.manager;

import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.agents.Agent;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaListener {

    private final List<Agent> agentList;

    Thread thread = new Thread(){
        public void run(){
            for (Agent a: agentList){
                log.info("Start initialization of {}", a.getAgentName());
                a.init();
                log.info("Initialization of {} - OK", a.getAgentName());
            }
            System.out.println("Start consume messages from Kafka");
            while(true){
                for (Agent a: agentList){
                    a.processMessages();
                }
            }
        }
    };

    @PostConstruct
    public void init(){
        thread.start();
    }
}
