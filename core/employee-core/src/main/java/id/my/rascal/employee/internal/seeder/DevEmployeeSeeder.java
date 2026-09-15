package id.my.rascal.employee.internal.seeder;

import java.time.LocalDateTime;
import java.util.List;

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
public class DevEmployeeSeeder implements Seeder {

    private final EmployeeRepository employeeRepository;

    public DevEmployeeSeeder(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
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

        Employee cashier = new Employee();
        cashier.setName("Kasir Employee");
        cashier.setEmail("kasir@rascal.id");
        cashier.setPhone("081234567891");
        cashier.setPosition("Cashier");
        cashier.setDepartment("Finance");
        cashier.setStatus(EmployeeStatus.ACTIVE);
        cashier.markActive();
        cashier.setCreatedAt(now);

        if (!employeeRepository.existsByEmail(admin.getEmail())) {
            employeeRepository.save(admin);
        }
        if (!employeeRepository.existsByEmail(cashier.getEmail())) {
            employeeRepository.save(cashier);
        }
    }
}