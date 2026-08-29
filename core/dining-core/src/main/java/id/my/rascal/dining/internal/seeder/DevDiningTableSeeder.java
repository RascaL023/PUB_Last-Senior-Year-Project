package id.my.rascal.dining.internal.seeder;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.repository.DiningTableRepository;

@Component
@Order(50)
public class DevDiningTableSeeder implements Seeder {

    private static final List<String> TABLE_NUMBERS = List.of(
        "1", "2", "3", "4", "5",
        "6", "7", "8", "9", "10"
    );

    private final DiningTableRepository diningTableRepository;

    public DevDiningTableSeeder(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();
        for (String tableNumber : TABLE_NUMBERS) {
            if (diningTableRepository.existsByTableNumber(tableNumber)) continue;
            DiningTable table = new DiningTable();
            table.setTableNumber(tableNumber);
            table.setStatus(TableStatus.AVAILABLE);
            table.setCreatedAt(now);
            diningTableRepository.save(table);
        }
    }
}
