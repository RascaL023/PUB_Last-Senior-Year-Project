package id.my.rascal.menu.internal.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.util.StringUtil;
import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.entity.ModifierOption;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.model.request.ModifierOptionPutRequest;
import id.my.rascal.menu.internal.model.request.ModifierOptionRequest;
import id.my.rascal.menu.internal.model.request.ModifierTypePutRequest;
import id.my.rascal.menu.internal.model.request.ModifierTypeRequest;
import id.my.rascal.menu.internal.model.response.ModifierOptionResponse;
import id.my.rascal.menu.internal.model.response.ModifierTypeResponse;
import id.my.rascal.menu.internal.repository.MenuRepository;
import id.my.rascal.menu.internal.repository.ModifierTypeRepository;

@Service
public class ModifierService {

    private final ModifierTypeRepository modifierTypeRepository;
    private final MenuRepository menuRepository;
    private final ModifierHelper modifierHelper;

    public ModifierService(
        ModifierTypeRepository modifierTypeRepository,
        MenuRepository menuRepository,
        ModifierHelper modifierHelper
    ) {
        this.modifierTypeRepository = modifierTypeRepository;
        this.menuRepository = menuRepository;
        this.modifierHelper = modifierHelper;
    }

    @Transactional
    public ModifierTypeResponse create(ModifierTypeRequest request) {
        ModifierType modifierType = new ModifierType();
        modifierType = this.fill(
            modifierType, request.name(), 
            request.minSelection(), request.maxSelection()
        );

        List<ModifierOption> modifierOptions = new ArrayList<>();

        // Lambda rule: Local variable modifierType defined in an enclosing scope must be final or effectively final
        // request.options().stream().forEach(req -> {
        //     ModifierOption modifierOption = newEntity(req);
        //     modifierOptions.add(modifierOption);
        //     modifierOption.setModifierType(modifierType);
        // });

        for (ModifierOptionRequest req : request.options()) {
            ModifierOption modifierOption = new ModifierOption();
            this.fill(modifierOption, req.name(), req.additionalPrice());

            modifierOptions.add(modifierOption);
            modifierOption.setModifierType(modifierType);
        }

        modifierType.setModifierOptions(modifierOptions);
        modifierType = modifierTypeRepository.save(modifierType);

        return this.toResponse(modifierType);
    }

    @Transactional(readOnly = true)
    public ModifierTypeResponse getById(Long id) {
        return toResponse(modifierHelper.getById(id));
    }

    @Transactional(readOnly = true) 
    public Page<ModifierTypeResponse> getAllPaged(String name, Pageable pageable){
        return modifierTypeRepository.search(StringUtil.normalizeSearch(name), pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ModifierTypeResponse> getAllByIds(Iterable<Long> ids) {
        return modifierHelper.getByIds(ids)
            .stream().map(this::toResponse).toList();
    }


    @Transactional
    public ModifierTypeResponse update(Long id, ModifierTypePutRequest request) {
        ModifierType modifierType = assign(modifierHelper.getById(id), request);
        modifierType.getModifierOptions().clear();

        for (ModifierOptionPutRequest req : request.options()) {
            ModifierOption opt = req.id() == null ?
                new ModifierOption() :
                modifierHelper.getAndValidateOwnerShipById(modifierType.getId(), req.id());

            fill(opt, req.name(), req.additionalPrice());
            opt.setModifierType(modifierType);
            modifierType.getModifierOptions().add(opt);
        }

        return toResponse(modifierTypeRepository.save(modifierType));
    }

    @Transactional
    public void delete(Long id) {
        ModifierType modifierType = modifierHelper.getById(id);

        for (Menu menu : menuRepository.findByModifierTypeId(id)) {
            menu.getModifierTypes().remove(modifierType);
            menuRepository.save(menu);
        }

        modifierTypeRepository.delete(modifierType);
    }



    private ModifierType assign(ModifierType modifierType, ModifierTypePutRequest request) {
        String name = StringUtil.normalizeSpaces(request.name());
        name = StringUtil.capitalize(name);

        modifierType.setName(name);
        modifierType.setMinSelection(request.minSelection());
        modifierType.setMaxSelection(request.maxSelection());
        
        return modifierType;
    }

    private ModifierOption fill(
        ModifierOption modifierOption,
        String name, Integer additionalPrice
    ) {
        name = StringUtil.normalizeSpaces(name);
        modifierOption.setName(StringUtil.normalizeAndCapitalizeFirst(name));
        modifierOption.setAdditionalPrice(additionalPrice);

        return modifierOption;
    }

    private ModifierType fill(
        ModifierType modifierType,
        String name, Integer minSelection, Integer maxSelection
    ) {
        name = StringUtil.normalizeSpaces(name);
        name = StringUtil.capitalize(name);

        modifierType.setName(name);
        modifierType.setMinSelection(minSelection);
        modifierType.setMaxSelection(maxSelection);
        
        return modifierType;
    }

    private ModifierOptionResponse toResponse(ModifierOption modifierOption) {
        return new ModifierOptionResponse(
            modifierOption.getId(), 
            modifierOption.getName(), 
            modifierOption.getAdditionalPrice()
        );
    }

    private ModifierTypeResponse toResponse(ModifierType modifierType) {
        return new ModifierTypeResponse(
            modifierType.getId(), 
            modifierType.getName(), 
            modifierType.getMinSelection(), 
            modifierType.getMaxSelection(), 
            modifierType.getModifierOptions().stream()
                .map(this::toResponse).toList()
        );
    }

}
