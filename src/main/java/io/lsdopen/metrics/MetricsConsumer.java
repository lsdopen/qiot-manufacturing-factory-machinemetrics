package io.lsdopen.metrics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * A bean consuming metrics from the JMS queue.
 */
@ApplicationScoped
public class MetricsConsumer implements Runnable {

    @Inject
    ConnectionFactory connectionFactory;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    private volatile String metrics;

    @ConfigProperty(name = "qiot.productline.metrics.queue-prefix")
    String productLineMetricsQueueName;

    public String getMetrics() {
        return metrics;
    }

    void onStart(@Observes StartupEvent ev) {
        scheduler.submit(this);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        System.out.println("Starting to consume metrics.");
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue(productLineMetricsQueueName));

            while (true) {
                Message messagePayload = consumer.receive();
                System.out.println("Receving messages payload.");
                if (messagePayload == null) return;
                metrics = messagePayload.getBody(String.class);
                System.out.println(metrics);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}