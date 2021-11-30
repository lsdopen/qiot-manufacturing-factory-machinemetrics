package io.lsdopen.metrics;

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
import javax.jms.Session;

//import org.jboss.logging.Logger;
import io.quarkus.logging.Log;

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

    // private static final Logger LOG = Logger.getLogger(MetricsConsumer.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile String metrics;

    @ConfigProperty(name = "qiot.productline.metrics.queue-prefix")
    String productLineMetricsQueueName;

    public String getMetrics() {
        return metrics;
    }

    void onStart(@Observes StartupEvent ev) throws Exception {

        System.out.println("Starting top read messages.");
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue(productLineMetricsQueueName));
            Log.debug("\nStarting mainloop");
            while (true) {
                Message messagePayload = consumer.receive();
                Log.debug("\nMessage Received");
                if (messagePayload == null)
                    return;
                metrics = messagePayload.getBody(String.class);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}