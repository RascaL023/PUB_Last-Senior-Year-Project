package id.my.rascal.menu.internal.listener;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.util.StringUtil;
import id.my.rascal.image.api.event.ImageCreatedEvent;
import id.my.rascal.image.api.event.ImageDeletedEvent;
import id.my.rascal.image.api.event.ImageUpdatedEvent;
import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.repository.MenuRepository;

@Component
public class MenuImageSyncListener {

    private static final Logger log = LoggerFactory.getLogger(MenuImageSyncListener.class);

    private final MenuRepository menuRepository;

    public MenuImageSyncListener(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @EventListener
    public void onImageCreated(ImageCreatedEvent event) {
        log.info("Received ImageCreatedEvent: filePath={}, fileId={}, url={}", event.filePath(), event.fileId(), event.url());
    }

    @EventListener
    @Transactional
    public void onImageUpdated(ImageUpdatedEvent event) {
        log.info("Received ImageUpdatedEvent: filePath={}, previousFilePath={}, fileId={}, url={}",
            event.filePath(), event.previousFilePath(), event.fileId(), event.url());

        if (StringUtil.safeIsBlank(event.previousFilePath())
            || StringUtil.safeIsBlank(event.filePath())
            || event.previousFilePath().equals(event.filePath()))
            return;

        List<Menu> menus = menuRepository.findByImageUrl(event.previousFilePath());
        for (Menu menu : menus) {
            Collections.replaceAll(menu.getImageUrls(), event.previousFilePath(), event.filePath());
        }
        menuRepository.saveAll(menus);
    }

    @EventListener
    @Transactional
    public void onImageDeleted(ImageDeletedEvent event) {
        log.info("Received ImageDeletedEvent: filePath={}, fileId={}", event.filePath(), event.fileId());
        if (StringUtil.safeIsBlank(event.filePath())) return;

        List<Menu> menus = menuRepository.findByImageUrl(event.filePath());
        for (Menu menu : menus)
            menu.getImageUrls().removeIf(event.filePath()::equals);
        menuRepository.saveAll(menus);
    }

}
