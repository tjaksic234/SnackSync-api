package com.example.KavaSpring.services.impl;

import com.example.KavaSpring.converters.ConverterService;
import com.example.KavaSpring.exceptions.NotFoundException;
import com.example.KavaSpring.models.dao.Order;
import com.example.KavaSpring.models.dao.UserProfile;
import com.example.KavaSpring.models.dto.*;
import com.example.KavaSpring.models.enums.EventStatus;
import com.example.KavaSpring.models.enums.OrderStatus;
import com.example.KavaSpring.repository.EventRepository;
import com.example.KavaSpring.repository.OrderRepository;
import com.example.KavaSpring.repository.UserProfileRepository;
import com.example.KavaSpring.security.utils.Helper;
import com.example.KavaSpring.services.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final UserProfileRepository userProfileRepository;

    private final EventRepository eventRepository;

    private final ConverterService converterService;

    private final MongoTemplate mongoTemplate;

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        UserProfile userProfile = userProfileRepository.getUserProfileByUserId(Helper.getLoggedInUserId());
        boolean existsEvent = eventRepository.existsById(request.getEventId());


        if (userProfile == null) {
            throw new NotFoundException("No userProfile associated with id");
        }

        if (!existsEvent) {
            throw new NotFoundException("No event associated with eventId in the order");
        }

        boolean existingOrder = orderRepository.existsByUserProfileIdAndEventId(userProfile.getId(), request.getEventId());
        if (existingOrder) {
            throw new IllegalStateException("User already has an order for this event");
        }

        log.info("the order request is: {}", request);
        Order order = new Order();
        order.setUserProfileId(userProfile.getId());
        order.setEventId(request.getEventId());
        order.setAdditionalOptions(request.getAdditionalOptions());
        orderRepository.save(order);

        log.info("Order created");
        return converterService.convertToOrderResponse(request);
    }

    @Override
    public OrderDto getOrderById(String id) {
        Order order = orderRepository.getById(id)
                .orElseThrow(() -> new NotFoundException("No order associated with id"));

        log.info("Get order by id finished");
        return converterService.convertToOrderDto(order);
    }

    @Override
    public List<OrderEventInfoDto> getAllOrdersFromUserProfile() {
        UserProfile userProfile = userProfileRepository.getUserProfileByUserId(Helper.getLoggedInUserId());

        if (userProfile == null) {
            throw new NotFoundException("Bad user profile id provided");
        }

        MatchOperation matchOperation = Aggregation.match(Criteria.where("userProfileId").is(userProfile.getId()));

        AddFieldsOperation convertEventIdToObjectId  = Aggregation.addFields()
                .addField("eventId")
                .withValueOf(ConvertOperators.ToObjectId.toObjectId("$eventId"))
                .build();

        LookupOperation lookupOperation = Aggregation.lookup("events", "eventId", "_id", "eventDetails");

        UnwindOperation unwindOperation = Aggregation.unwind("eventDetails");

        ProjectionOperation projectionOperation = Aggregation.project()
                .and("eventId").as("eventId")
                .and("eventDetails.eventType").as("eventType")
                .and("status").as("status")
                .and("additionalOptions").as("additionalOptions")
                .and("rating").as("rating")
                .and("createdAt").as("createdAt");

        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"));

        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                convertEventIdToObjectId,
                lookupOperation,
                unwindOperation,
                projectionOperation,
                sortOperation
        );

        AggregationResults<OrderEventInfoDto> results = mongoTemplate.aggregate(aggregation, "orders", OrderEventInfoDto.class);

        return results
                .getMappedResults()
                .stream()
                .map(converterService::convertToOrderEventInfoDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderActivityResponse> getOrdersByActivityStatus(boolean isActive) {
        UserProfile userProfile = userProfileRepository.getUserProfileByUserId(Helper.getLoggedInUserId());

        if (userProfile == null) {
            throw new NotFoundException("User profile is null");
        }

        if (userProfile.getId() == null) {
            throw new IllegalStateException("User profile id is null");
        }

        MatchOperation matchUserOrders  = Aggregation.match(Criteria.where("userProfileId").is(userProfile.getId()));


        AddFieldsOperation convertEventIdToObjectId  = Aggregation.addFields()
                .addField("eventId")
                .withValueOf(ConvertOperators.ToObjectId.toObjectId("$eventId"))
                .build();

        LookupOperation lookupOperation = Aggregation.lookup("events", "eventId", "_id", "event");

        UnwindOperation unwindOperation = Aggregation.unwind("event");

        //! Determine which statuses to include based on isActive parameter
        List<EventStatus> statusesToInclude = isActive
                ? Arrays.asList(EventStatus.PENDING, EventStatus.IN_PROGRESS)
                : Collections.singletonList(EventStatus.COMPLETED);

        MatchOperation matchEventStatus  = Aggregation.match(Criteria.where("event.status").in(statusesToInclude));

        ProjectionOperation projectionOperation = Aggregation.project()
                .and("event._id").as("eventId")
                .and("_id").as("orderId")
                .and("event.title").as("title")
                .and("event.description").as("description")
                .and("event.groupId").as("groupId")
                .and("event.status").as("status")
                .and("event.eventType").as("eventType")
                .and("event.createdAt").as("createdAt")
                .and("event.pendingUntil").as("pendingUntil");

        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.DESC, "event.createdAt"));

        Aggregation aggregation = Aggregation.newAggregation(
                matchUserOrders,
                convertEventIdToObjectId,
                lookupOperation,
                unwindOperation,
                matchEventStatus,
                projectionOperation,
                sortOperation
        );

        AggregationResults<OrderActivityResponse> results = mongoTemplate.aggregate(aggregation, "orders", OrderActivityResponse.class);

        return results
                .getMappedResults()
                .stream()
                .map(converterService::convertToOrderActiveResponse)
                .collect(Collectors.toList());

    }

    @Override
    public String updateOrderStatus(String id, OrderStatus status) {
        Optional<Order> order = orderRepository.getById(id);
        if (order.isEmpty()) {
            throw new NotFoundException("The order status update was not successful");
        }
        order.get().setStatus(status);
        orderRepository.save(order.get());

        log.info("Order status successfully updated");
        return "Order status successfully updated";
    }

    @Override
    public List<OrderExpandedResponse> getOrdersByEventId(String id) {

        eventRepository.findById(id).orElseThrow(() -> new NotFoundException("No event associated with the given eventId"));

        MatchOperation matchOrdersByEventId = Aggregation.match(Criteria.where("eventId").is(id));

        AddFieldsOperation convertUserProfileIdToObjectId  = Aggregation.addFields()
                .addField("userProfileId")
                .withValueOf(ConvertOperators.ToObjectId.toObjectId("$userProfileId"))
                .build();

        LookupOperation lookupOperation = Aggregation.lookup("userProfiles", "userProfileId", "_id", "userProfile");

        UnwindOperation unwindOperation = Aggregation.unwind("userProfile");

        ProjectionOperation projectionOperation = Aggregation.project()
                .andExclude("_id")
                .and("$_id").as("orderId")
                .and("$userProfileId").as("userProfileId")
                .and("$userProfile.firstName").as("firstName")
                .and("$userProfile.lastName").as("lastName")
                .and("$additionalOptions").as("additionalOptions")
                .and("$status").as("status")
                .and("$createdAt").as("createdAt");

        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"));

        Aggregation aggregation = Aggregation.newAggregation(
              matchOrdersByEventId,
              convertUserProfileIdToObjectId,
              lookupOperation,
              unwindOperation,
              projectionOperation,
              sortOperation
        );

        AggregationResults<OrderExpandedResponse> results = mongoTemplate.aggregate(aggregation, "orders", OrderExpandedResponse.class);

        log.info("Fetched the orders successfully");
        return results.getMappedResults();
    }


}
