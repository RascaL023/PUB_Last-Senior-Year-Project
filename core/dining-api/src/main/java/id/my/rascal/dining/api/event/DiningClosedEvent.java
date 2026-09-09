package id.my.rascal.dining.api.event;

import java.time.LocalDateTime;

public record DiningClosedEvent(
    Long diningId,
    LocalDateTime closedAt
) {}
