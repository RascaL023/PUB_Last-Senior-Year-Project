package id.my.rascal.menu.api;

import java.util.Collection;
import java.util.List;

public interface MenuDataProvider {
    List<MenuSnapshot> getMenuSnapshots(Collection<Long> menuIds);
    List<ModifierOptionSnapshot> getModifierOptionSnapshots(Collection<Long> optionIds);
}
