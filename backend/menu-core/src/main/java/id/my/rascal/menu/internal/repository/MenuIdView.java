package id.my.rascal.menu.internal.repository;

// v2: projection ringan, hanya berisi id yang relevan untuk cache FE
public interface MenuIdView {

    Long getMenuId();
    Long getCategoryId();
    Long getModifierTypeId();

}
