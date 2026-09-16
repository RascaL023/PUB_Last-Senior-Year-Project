package id.my.rascal.auth.api.event;

import java.util.Set;

public record UserRolesUpdatedEvent(Long userAuthId, Set<String> roleNames) {}
