package id.my.rascal.menu.internal.seeder;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;
import id.my.rascal.menu.internal.entity.ModifierOption;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.repository.ModifierTypeRepository;

@Component
@Order(45)
public class DevModifierSeeder implements Seeder {

    private final ModifierTypeRepository modifierTypeRepository;

    public DevModifierSeeder(ModifierTypeRepository modifierTypeRepository) {
        this.modifierTypeRepository = modifierTypeRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
    }

    @Override
    @Transactional
    public void seed() {
        for (MenuSeedCatalog.ModifierSeed seed : MenuSeedCatalog.MODIFIERS) {
            if (modifierTypeRepository.existsByName(seed.name())) continue;
            ModifierType type = new ModifierType();
            type.setName(seed.name());
            type.setMinSelection(seed.minSelection());
            type.setMaxSelection(seed.maxSelection());

            List<ModifierOption> options = new ArrayList<>();
            for (MenuSeedCatalog.OptionSeed optionSeed : seed.options()) {
                ModifierOption option = new ModifierOption();
                option.setName(optionSeed.name());
                option.setAdditionalPrice(optionSeed.additionalPrice());
                option.setModifierType(type);
                options.add(option);
            }
            type.setModifierOptions(options);
            modifierTypeRepository.save(type);
        }
    }

}
