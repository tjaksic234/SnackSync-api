package com.example.KavaSpring.services;

import com.example.KavaSpring.models.dto.*;

import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest request);
    EventDto getEventById(String id);
    List<EventExpandedResponse> searchEvents(EventSearchRequest request);
    void updateEventsJob();
}
