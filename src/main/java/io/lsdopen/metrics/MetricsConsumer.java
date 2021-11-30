package io.lsdopen.metrics;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import io.quarkus.logging.Log;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import io.qiot.manufacturing.all.commons.domain.production.ProductionChainStageEnum;
import io.lsdopen.metrics.MetricsConsumerDTO;

//import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A bean consuming metrics from the JMS queue.
 */
@ApplicationScoped
public class MetricsConsumer implements Runnable {

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    ObjectMapper MAPPER;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile String metrics;

    private JMSContext context;

    private JMSConsumer consumer;

    private Queue queue;

    @ConfigProperty(name = "qiot.productline.metrics.queue-prefix")
    String productLineMetricsQueueName;

    private final Map<UUID, MetricsConsumerDTO> productionCounters;

    public MetricsConsumer() {
        productionCounters = new TreeMap<UUID, MetricsConsumerDTO>();
    }

    public Map<UUID, MetricsConsumerDTO> getCounters() {
        return productionCounters;
    }

    public String getMetrics() {
        return metrics;
    }

    void onStart(@Observes StartupEvent ev) throws Exception {

        context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
        queue = context.createQueue(productLineMetricsQueueName);
        consumer = context.createConsumer(queue);

        System.out.println("Starting top read messages.");
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {

        Log.debug("\nStarting mainloop");

        while (true) {

            Message metricsMessage = consumer.receive();

            try {

                String messagePayload = metricsMessage.getBody(String.class);
                Log.debugf("\nmessagePayload read from metricsMessage %s\n", messagePayload);
                MAPPER.readValue(messagePayload, productionCounters)

            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

}