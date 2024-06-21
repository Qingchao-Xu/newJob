package org.xu.newjob.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.xu.newjob.entity.Event;

@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void fireEvent(Event event) {
        kafkaTemplate.send(event.getTopic(), event);
    }

}
