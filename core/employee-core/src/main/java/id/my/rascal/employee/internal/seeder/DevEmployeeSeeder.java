package id.my.rascal.employee.internal.seeder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.AuthApi;
import id.my.rascal.auth.api.UserAuthApiResponse;
import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;
import id.my.rascal.employee.internal.entity.Employee;
import id.my.rascal.employee.internal.model.enums.EmployeeStatus;
import id.my.rascal.employee.internal.repository.EmployeeRepository;

@Component
@Order(35)
public class DevEmployeeSeeder implements Seeder {

    private static final Map<String, String> DEV_EMPLOYEE_MAP = Map.of(
        "admin@rascal.id", "ADMIN",
        "kasir@rascal.id", "CASHIER",
        "waiter@rascal.id", "WAITER",
        "kitchen@rascal.id", "KITCHEN"
    );

    private static final Map<String, String> DEV_EMPLOYEE_NAMES = Map.of(
        "admin@rascal.id", "Admin Employee",
        "kasir@rascal.id", "Kasir Employee",
        "waiter@rascal.id", "Waiter Employee",
        "kitchen@rascal.id", "Kitchen Employee"
    );

    private static final Map<String, String> DEV_EMPLOYEE_PHONES = Map.of(
        "admin@rascal.id", "081234567890",
        "kasir@rascal.id", "081234567891",
        "waiter@rascal.id", "081234567892",
        "kitchen@rascal.id", "081234567893"
    );

    private final EmployeeRepository employeeRepository;
    private final AuthApi authApi;

    public DevEmployeeSeeder(EmployeeRepository employeeRepository, AuthApi authApi) {
        this.employeeRepository = employeeRepository;
        this.authApi = authApi;
    }

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, String> entry : DEV_EMPLOYEE_MAP.entrySet()) {
            String email = entry.getKey();
            String roleName = entry.getValue();

            Optional<UserAuthApiResponse> auth = authApi.getByEmail(email);
            if (auth.isEmpty()) continue;

            Long userAuthId = auth.get().id();

            Employee employee = employeeRepository.findByEmail(email).orElse(null);
            if (employee == null) {
                employee = new Employee();
                employee.setName(DEV_EMPLOYEE_NAMES.get(email));
                employee.setEmail(email);
                employee.setPhone(DEV_EMPLOYEE_PHONES.get(email));
                employee.setStatus(EmployeeStatus.ACTIVE);
                employee.markActive();
                employee.setCreatedAt(now);
            }

            employee.setUserAuthId(userAuthId);
            employee.setRoleName(roleName);
            employee.setUpdatedAt(now);

            employeeRepository.save(employee);
        }
    }
}
