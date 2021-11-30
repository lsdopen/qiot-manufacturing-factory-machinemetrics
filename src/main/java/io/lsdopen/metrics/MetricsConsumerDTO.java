package io.lsdopen.metrics;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.qiot.manufacturing.all.commons.domain.production.ProductionChainStageEnum;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author andreabattaglia
 *
 */
@RegisterForReflection
public class MetricsConsumerDTO {
    public UUID productLineId;
    public Map<ProductionChainStageEnum, AtomicInteger> stageCounters;
    public Map<ProductionChainStageEnum, AtomicInteger> waitingForValidationCounters;
    public AtomicInteger totalItems;
    public AtomicInteger completed;
    public AtomicInteger discarded;

    public MetricsConsumerDTO(UUID productLineId) {
        this.productLineId = productLineId;

        stageCounters = new TreeMap<ProductionChainStageEnum, AtomicInteger>();
        for (ProductionChainStageEnum e : ProductionChainStageEnum.values())
            stageCounters.put(e, new AtomicInteger());

        waitingForValidationCounters = new TreeMap<ProductionChainStageEnum, AtomicInteger>();
        for (ProductionChainStageEnum e : ProductionChainStageEnum.values())
            waitingForValidationCounters.put(e, new AtomicInteger());

        totalItems = new AtomicInteger();
        completed = new AtomicInteger();
        discarded = new AtomicInteger();
    }

    /**
     * @return the productLineId
     */
    public UUID getProductLineId() {
        return productLineId;
    }

    public Map<ProductionChainStageEnum, AtomicInteger> getStageCounters() {
        return stageCounters;
    }

    public Map<ProductionChainStageEnum, AtomicInteger> getWaitingForValidationCounters() {
        return waitingForValidationCounters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productLineId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetricsConsumerDTO other = (MetricsConsumerDTO) obj;
        return Objects.equals(productLineId, other.productLineId);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProductionCountersDTO [productLineId=");
        builder.append(productLineId);
        builder.append(", stageCounters=");
        builder.append(stageCounters);
        builder.append(", waitingForValidationCounters=");
        builder.append(waitingForValidationCounters);
        builder.append(", totalItems=");
        builder.append(totalItems);
        builder.append(", completed=");
        builder.append(completed);
        builder.append(", discarded=");
        builder.append(discarded);
        builder.append("]");
        return builder.toString();
    }

}
