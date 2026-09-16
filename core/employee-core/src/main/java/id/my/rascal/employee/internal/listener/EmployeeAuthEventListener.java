package id.my.rascal.employee.internal.listener;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.event.UserRolesUpdatedEvent;
import id.my.rascal.employee.internal.repository.EmployeeRepository;

@Component
public class EmployeeAuthEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmployeeAuthEventListener.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeAuthEventListener(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @EventListener
    @Transactional
    public void onUserRolesUpdated(UserRolesUpdatedEvent event) {
        log.info("Received UserRolesUpdatedEvent: userAuthId={}, roleNames={}",
            event.userAuthId(), event.roleNames());

        if (event.userAuthId() == null) return;

        employeeRepository.findActiveByUserAuthId(event.userAuthId()).ifPresent(employee -> {
            String cached = event.roleNames().stream()
                .sorted()
                .collect(Collectors.joining(","));
            employee.setRoleName(cached.isEmpty() ? null : cached);
            employeeRepository.save(employee);
        });
    }

}
