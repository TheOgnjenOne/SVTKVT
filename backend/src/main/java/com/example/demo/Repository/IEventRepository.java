package com.example.demo.Repository;

import com.example.demo.Model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {
    List<Event> findByLocationId(Long locationId);
    List<Event> findByLocationIdAndDateAfter(Long locationId, LocalDateTime date);
    List<Event> findByLocationIdAndDateBefore(Long locationId, LocalDateTime date);
    List<Event> findByLocationIdAndDateBetween(Long locationId, LocalDateTime startDate, LocalDateTime endDate);
    List<Event> findByLocationIdIn(List<Long> locationIds);
}
