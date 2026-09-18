package id.my.rascal.employee.internal.seeder;

import java.time.LocalDateTime;
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
public class FormalEmployeeSeeder implements Seeder {

    private final EmployeeRepository employeeRepository;
    private final AuthApi authApi;

    public FormalEmployeeSeeder(EmployeeRepository employeeRepository, AuthApi authApi) {
        this.employeeRepository = employeeRepository;
        this.authApi = authApi;
    }

    @Override
    public SeedType seedType() {
        return SeedType.FORMAL;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();

        String email = "admin@rascal.id";
        String roleName = "ADMIN";

        Optional<UserAuthApiResponse> auth = authApi.getByEmail(email);
        if (auth.isEmpty()) return;

        Long userAuthId = auth.get().id();

        Employee employee = employeeRepository.findByEmail(email).orElse(null);
        if (employee == null) {
            employee = new Employee();
            employee.setName("Admin Employee");
            employee.setEmail(email);
            employee.setPhone("081234567890");
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
