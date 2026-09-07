package id.my.rascal.customer.internal.seeder;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.seed.SeedType;
import id.my.rascal.common.seed.Seeder;
import id.my.rascal.customer.internal.entity.Customer;
import id.my.rascal.customer.internal.repository.CustomerRepository;

@Component
@Order(40)
public class DevCustomerSeeder implements Seeder {

    private final CustomerRepository customerRepository;

    public DevCustomerSeeder(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
    }

    @Override
    @Transactional
    public void seed() {
        if (customerRepository.count() > 0)
            return;

        LocalDateTime now = LocalDateTime.now();
        List<Customer> customers = List.of(
            member("Budi Santoso", "budi@example.com", "081234567890", now),
            member("Siti Aminah", "siti@example.com", "081234567891", now)
        );

        customerRepository.saveAll(customers);
    }

    private Customer member(String name, String email, String phone, LocalDateTime now) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setCreatedAt(now);
        return customer;
    }

}
