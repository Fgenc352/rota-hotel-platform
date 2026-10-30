package com.hotelbooking.analytics.infrastructure.mapper;

import com.hotelbooking.analytics.application.model.FunnelCount;
import com.hotelbooking.shared.persistence.TupleValues;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

@Component
public class FunnelCountMapper {
    public FunnelCount toModel(Tuple row) {
        return new FunnelCount(
            TupleValues.text(row, "stage"),
            TupleValues.number(row, "sessions")
            );
    }
}
