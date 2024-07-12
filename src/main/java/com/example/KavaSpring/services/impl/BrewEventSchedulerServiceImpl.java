package com.example.KavaSpring.services.impl;

import com.example.KavaSpring.models.dao.BrewEvent;
import com.example.KavaSpring.models.dao.enums.EventStatus;
import com.example.KavaSpring.repository.BrewEventRepository;
import com.example.KavaSpring.services.BrewEventSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BrewEventSchedulerServiceImpl implements BrewEventSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(BrewEventSchedulerServiceImpl.class);
    @Autowired
    private BrewEventRepository brewEventRepository;

    @Override
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void updatePendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<BrewEvent> pendingEvents = brewEventRepository.findByStatusAndStartTimeBefore(EventStatus.PENDING, now);

        for (BrewEvent event : pendingEvents) {
            event.setStatus(EventStatus.IN_PROGRESS);
            brewEventRepository.save(event);
        }
    }
}
