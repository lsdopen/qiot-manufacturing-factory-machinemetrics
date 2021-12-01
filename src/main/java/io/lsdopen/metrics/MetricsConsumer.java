package io.lsdopen.metrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkus.logging.Log;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import io.qiot.manufacturing.all.commons.domain.production.ProductionChainStageEnum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
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

    private final MeterRegistry registry;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private JMSContext context;

    private JMSConsumer consumer;

    private Queue queue;

    @ConfigProperty(name = "qiot.productline.metrics.queue-prefix")
    String productLineMetricsQueueName;

    private JsonNode metrics;

    MetricsConsumer(MeterRegistry registry) {
        this.registry = registry;
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

    public JsonNode getMetrics() {
        return metrics;
    }

    @Override
    public void run() {

        Log.debug("\nStarting mainloop");

        while (true) {

            Message metricsMessage = consumer.receive();

            try {

                String messagePayload = metricsMessage.getBody(String.class);

                Log.debug("\n\nNew metrics:");
                Log.debug(messagePayload);
                metrics= MAPPER.readTree(messagePayload);

                Tag machineNameTag = Tag.of("machineName", metrics.get("machineName").textValue());
                Tag machineSerialTag = Tag.of("machineSerial", metrics.get("machineSerial").textValue());
                Iterable<Tag> tags = Tags.of(machineNameTag, machineSerialTag);

                Iterator<Entry<String, JsonNode>> productionCounters = metrics.get("productionCounters").fields();

                while (productionCounters.hasNext()) {
                    Map.Entry<String, JsonNode> productLine = (Map.Entry<String, JsonNode>) productionCounters.next();

                    JsonNode productLineCounter = productLine.getValue();
                    int weavingValue = productLineCounter.get("stageCounters").get(ProductionChainStageEnum.WEAVING.toString()).intValue();
                    int coloringValue = productLineCounter.get("stageCounters").get(ProductionChainStageEnum.COLORING.toString()).intValue();
                    int printingValue = productLineCounter.get("stageCounters").get(ProductionChainStageEnum.PRINTING.toString()).intValue();
                    int packagingValue = productLineCounter.get("stageCounters").get(ProductionChainStageEnum.PACKAGING.toString()).intValue();
                    int weavingValidationValue = productLineCounter.get("waitingForValidationCounters").get(ProductionChainStageEnum.WEAVING.toString()).intValue();
                    int coloringValidationValue = productLineCounter.get("waitingForValidationCounters").get(ProductionChainStageEnum.COLORING.toString()).intValue();
                    int printingValidationValue = productLineCounter.get("waitingForValidationCounters").get(ProductionChainStageEnum.PRINTING.toString()).intValue();
                    int packagingValidationValue = productLineCounter.get("waitingForValidationCounters").get(ProductionChainStageEnum.PACKAGING.toString()).intValue();

                    Tag productLineTag = Tag.of("productLine", productLineCounter.get("productLineId").textValue());
                    Iterable<Tag> productLineTags = Tags.of(productLineTag);

                    Counter totalItems = registry.counter("machinemetrics.totalItems", Tags.concat(tags, productLineTags));
                    totalItems.increment(productLineCounter.get("totalItems").intValue() - totalItems.count());
                    Counter completed = registry.counter("machinemetrics.completed", Tags.concat(tags, productLineTags));
                    completed.increment(productLineCounter.get("completed").intValue() - completed.count());
                    Counter discarded = registry.counter("machinemetrics.discarded", Tags.concat(tags, productLineTags));
                    discarded.increment(productLineCounter.get("discarded").intValue() - discarded.count());

                    AtomicInteger weaving = registry.gauge("machinemetrics.weaving", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    weaving.set(weavingValue);

                    AtomicInteger coloring = registry.gauge("machinemetrics.coloring", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    coloring.set(coloringValue);

                    AtomicInteger printing = registry.gauge("machinemetrics.printing", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    printing.set(printingValue);

                    AtomicInteger packaging = registry.gauge("machinemetrics.packaging", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    packaging.set(packagingValue);

                    AtomicInteger weavingValidation = registry.gauge("machinemetrics.waitingForValidation.weaving", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    weavingValidation.set(weavingValidationValue);

                    AtomicInteger coloringValidation = registry.gauge("machinemetrics.waitingForValidation.coloring", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    coloringValidation.set(coloringValidationValue));

                    AtomicInteger printingValidation = registry.gauge("machinemetrics.waitingForValidation.printing", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    printingValidation.set(printingValidationValue);

                    AtomicInteger packagingValidation = registry.gauge("machinemetrics.waitingForValidation.packaging", Tags.concat(tags, productLineTags), new AtomicInteger(0));
                    packagingValidation.set(packagingValidationValue);
                }

            } catch (JsonMappingException e) {
                System.out.println("Mapping exception");
                System.out.println(e.getCause());
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                System.out.println("Processing exception");
                System.out.println(e.getCause());
                throw new RuntimeException(e);
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

}