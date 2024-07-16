package com.example.KavaSpring.service.impl;

import com.example.KavaSpring.models.dto.BrewEventResult;
import com.example.KavaSpring.models.enums.EventStatus;
import com.example.KavaSpring.service.BrewEventAggregationService;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Setter
public class BrewEventAggregationServiceImpl implements BrewEventAggregationService {

    private static final Logger log = LoggerFactory.getLogger(BrewEventAggregationServiceImpl.class);
    private String id;

    private final MongoTemplate mongoTemplate;

    public BrewEventAggregationServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<BrewEventResult> aggregateBrewEvents() {

        MatchOperation matchOperation = Aggregation.match(
                Criteria.where("userId").ne(id)
                        .and("status").is(EventStatus.PENDING)
        );

        ProjectionOperation projectionOperation = Aggregation.project()
                .and("_id").as("eventId")
                .and("userId").as("userId")
                .and("status").as("status");

        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                projectionOperation
        );

        AggregationResults<BrewEventResult> results = mongoTemplate.aggregate(
                aggregation, "brewEvents", BrewEventResult.class);

        log.info("Aggregation results: " + results.getMappedResults());
        return results.getMappedResults();
    }
}
