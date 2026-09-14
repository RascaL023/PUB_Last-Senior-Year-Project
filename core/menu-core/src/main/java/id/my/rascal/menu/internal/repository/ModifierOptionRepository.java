package id.my.rascal.menu.internal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import id.my.rascal.menu.internal.entity.ModifierOption;

public interface ModifierOptionRepository extends JpaRepository<ModifierOption, Long> {

    List<ModifierOption> findByModifierTypeId(Long modifierTypeId);

    Optional<ModifierOption> findByModifierTypeIdAndId(Long modifierTypeId, Long id);

}
