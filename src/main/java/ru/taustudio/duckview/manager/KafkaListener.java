package ru.taustudio.duckview.manager;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.taustudio.duckview.manager.agents.Agent;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaListener {

    private final List<Agent> agentList;
    private boolean work = true;

    Thread thread = new Thread(){
        public void run(){
            for (Agent a: agentList){
                log.info("Start initialization of {}", a.getAgentName());
                a.init();
                log.info("Initialization of {} - OK", a.getAgentName());
            }
            System.out.println("Start consume messages from Kafka");
            while(work){
                for (Agent a: agentList){
                    try {
                        a.processMessages();
                    }catch(Throwable trw){
                        System.out.println("Error when processing " + a.getAgentName());
                    }
                }
            }
            System.out.println("Shutting down agents");
            for (Agent a: agentList){
                a.destroy();
            }
        }
    };

    @PostConstruct
    public void init(){
        thread.start();
    }

    @PreDestroy
    public void destroy(){
        work = false;
    }
}
