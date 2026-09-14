package id.my.rascal.employee.internal.seeder;

import java.time.LocalDateTime;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;
import id.my.rascal.employee.internal.entity.Employee;
import id.my.rascal.employee.internal.model.enums.EmployeeStatus;
import id.my.rascal.employee.internal.repository.EmployeeRepository;

@Component
@Order(10)
public class FormalEmployeeSeeder implements Seeder {

    private final EmployeeRepository employeeRepository;

    public FormalEmployeeSeeder(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.FORMAL;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();

        Employee admin = new Employee();
        admin.setName("Admin Employee");
        admin.setEmail("admin@rascal.id");
        admin.setPhone("081234567890");
        admin.setPosition("Manager");
        admin.setDepartment("Management");
        admin.setStatus(EmployeeStatus.ACTIVE);
        admin.markActive();
        admin.setCreatedAt(now);

        if (!employeeRepository.existsByEmail(admin.getEmail())) {
            employeeRepository.save(admin);
        }
    }
}