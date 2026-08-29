package id.my.rascal.menu.internal.adapter;

import id.my.rascal.menu.api.MenuApi;
import id.my.rascal.menu.api.MenuApiResponse;
import id.my.rascal.menu.api.ModifierOptionApiResponse;
import id.my.rascal.menu.api.ModifierTypeApiResponse;
import id.my.rascal.menu.internal.repository.MenuRepository;
import id.my.rascal.menu.internal.repository.ModifierOptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class MenuApiImpl implements MenuApi {

    private final MenuRepository menuRepository;
    private final ModifierOptionRepository modifierOptionRepository;

    public MenuApiImpl(
        MenuRepository menuRepository,
        ModifierOptionRepository modifierOptionRepository
    ) {
        this.menuRepository = menuRepository;
        this.modifierOptionRepository = modifierOptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuApiResponse> getMenuSnapshots(Collection<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty())
            return List.of();

        return menuRepository.findAllById(menuIds).stream()
            .map(menu -> new MenuApiResponse(
                menu.getId(),
                menu.getName(),
                menu.getBasePrice(),
                menu.getIsAvailable(),
                Optional.ofNullable(menu.getModifierTypes()).orElse(List.of()).stream()
                    .map(type -> new ModifierTypeApiResponse(
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
    public List<ModifierOptionApiResponse> getModifierOptionSnapshots(Collection<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty())
            return List.of();

        return modifierOptionRepository.findAllById(optionIds).stream()
            .map(option -> new ModifierOptionApiResponse(
                option.getId(),
                option.getModifierType().getId(),
                option.getName(),
                option.getAdditionalPrice()
            ))
            .toList();
    }

}
