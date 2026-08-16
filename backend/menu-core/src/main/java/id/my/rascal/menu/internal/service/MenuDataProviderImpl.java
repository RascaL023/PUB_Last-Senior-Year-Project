package id.my.rascal.menu.internal.service;

import id.my.rascal.menu.api.MenuDataProvider;
import id.my.rascal.menu.api.MenuSnapshot;
import id.my.rascal.menu.api.ModifierOptionSnapshot;
import id.my.rascal.menu.api.ModifierTypeSnapshot;
import id.my.rascal.menu.internal.repository.MenuRepository;
import id.my.rascal.menu.internal.repository.ModifierOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class MenuDataProviderImpl implements MenuDataProvider {

    private final MenuRepository menuRepository;
    private final ModifierOptionRepository modifierOptionRepository;

    public MenuDataProviderImpl(
        MenuRepository menuRepository,
        ModifierOptionRepository modifierOptionRepository
    ) {
        this.menuRepository = menuRepository;
        this.modifierOptionRepository = modifierOptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuSnapshot> getMenuSnapshots(Collection<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty())
            return List.of();

        return menuRepository.findAllById(menuIds).stream()
            .map(menu -> new MenuSnapshot(
                menu.getId(),
                menu.getName(),
                menu.getBasePrice(),
                Optional.ofNullable(menu.getModifierTypes()).orElse(List.of()).stream()
                    .map(type -> new ModifierTypeSnapshot(
                        type.getId(),
                        type.getMinSelection(),
                        type.getMaxSelection()
                    ))
                    .toList()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModifierOptionSnapshot> getModifierOptionSnapshots(Collection<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty())
            return List.of();

        return modifierOptionRepository.findAllById(optionIds).stream()
            .map(option -> new ModifierOptionSnapshot(
                option.getId(),
                option.getModifierType().getId(),
                option.getName(),
                option.getAdditionalPrice()
            ))
            .toList();
    }

}
