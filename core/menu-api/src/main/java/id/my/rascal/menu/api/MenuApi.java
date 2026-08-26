package id.my.rascal.menu.api;

import java.util.Collection;
import java.util.List;

public interface MenuApi {
    List<MenuApiResponse> getMenuSnapshots(Collection<Long> menuIds);
    List<ModifierOptionApiResponse> getModifierOptionSnapshots(Collection<Long> optionIds);
}
