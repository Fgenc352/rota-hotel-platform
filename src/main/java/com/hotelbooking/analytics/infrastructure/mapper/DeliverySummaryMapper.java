package com.hotelbooking.analytics.infrastructure.mapper;

import com.hotelbooking.analytics.application.model.DeliverySummary;
import com.hotelbooking.shared.persistence.TupleValues;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

@Component
public class DeliverySummaryMapper {
    public DeliverySummary toModel(Tuple row) {
        return new DeliverySummary(
            TupleValues.text(row, "consumer"),
            TupleValues.number(row, "pending"),
            TupleValues.number(row, "dead")
            );
    }
}
