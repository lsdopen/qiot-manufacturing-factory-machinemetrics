package io.lsdopen.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/metrics")
@Produces("application/json")
public class MetricsResource {

    private final MeterRegistry registry;

    LinkedList<Long> list = new LinkedList<>();

    MetricsResource(MeterRegistry registry) {
        this.registry = registry;
        registry.gaugeCollectionSize("example.list.size", Tags.empty(), list);
    }

    @GET
    @Path("")
    public Long checkListSize(@PathParam("number") long number) {
        if (number == 2 || number % 2 == 0) {
            // add even numbers to the list
            list.add(number);
        } else {
            // remove items from the list for odd numbers
            try {
                number = list.removeFirst();
            } catch (NoSuchElementException nse) {
                number = 0;
            }
        }
        return number;
    }
}