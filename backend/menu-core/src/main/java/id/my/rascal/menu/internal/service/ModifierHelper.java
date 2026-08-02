package id.my.rascal.menu.internal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.menu.internal.entity.ModifierOption;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.repository.ModifierOptionRepository;
import id.my.rascal.menu.internal.repository.ModifierTypeRepository;

@Service
public class ModifierHelper {

    private final ModifierTypeRepository modifierTypeRepository;
    private final ModifierOptionRepository modifierOptionRepository;

    public ModifierHelper(
        ModifierTypeRepository modifierTypeRepository,
        ModifierOptionRepository modifierOptionRepository
    ) {
        this.modifierTypeRepository = modifierTypeRepository;
        this.modifierOptionRepository = modifierOptionRepository;
    }

    @Transactional(readOnly = true)
    public ModifierType getById(Long id) {
        return modifierTypeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Modifier with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<ModifierType> getByIds(Iterable<Long> ids) {
        return modifierTypeRepository.findAllById(ids);
    }


    @Transactional(readOnly = true)
    public List<ModifierOption> getOptionByIds(Iterable<Long> ids) {
        return modifierOptionRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public ModifierOption getOptionById(Long id) {
        return modifierOptionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Modifier option with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public ModifierOption getAndValidateOwnerShipById(Long modifierId, Long optionId) {
        return modifierOptionRepository.findByModifierTypeIdAndId(modifierId, optionId)
            .orElseThrow(() -> new BadRequestException(
                "The modifier id " + modifierId + " doesn't compatible with option id " + optionId
            ));
    }

}
