package id.my.rascal.menu.internal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.repository.MenuCategoryRepository;

@Service
public class MenuCategoryHelper {

    private final MenuCategoryRepository menuCategoryRepository;

    public MenuCategoryHelper(MenuCategoryRepository menuCategoryRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
    }

    @Transactional(readOnly = true)
    public MenuCategory getActiveById(Long id) {
        return menuCategoryRepository.findActiveById(id, false)
            .orElseThrow(() -> new NotFoundException("Menu category with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<MenuCategory> getActiveByIds(Iterable<Long> ids) {
        return menuCategoryRepository.findActiveByIds(ids);
    }

}
