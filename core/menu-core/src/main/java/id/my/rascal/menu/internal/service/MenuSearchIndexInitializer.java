package id.my.rascal.menu.internal.service;

import java.util.List;

import org.springframework.stereotype.Component;

import id.my.rascal.search.api.IndexSettings;
import id.my.rascal.search.api.SearchIndexInitializer;

@Component
public class MenuSearchIndexInitializer implements SearchIndexInitializer {

    @Override
    public String indexName() {
        return "menus";
    }

    @Override
    public IndexSettings indexSettings() {
        return IndexSettings.of(
            List.of("name", "description"),
            List.of("categoryIds", "basePrice", "isAvailable", "isDeleted"),
            List.of("name", "basePrice", "createdAt")
        );
    }

}
