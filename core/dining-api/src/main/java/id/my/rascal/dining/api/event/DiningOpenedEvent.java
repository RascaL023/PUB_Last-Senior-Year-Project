package id.my.rascal.dining.api.event;

import java.time.LocalDateTime;

public record DiningOpenedEvent(
    Long diningId,
    Long tableId,
    String tableNumber,
    LocalDateTime openedAt
) {}
