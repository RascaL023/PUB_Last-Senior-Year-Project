package id.my.rascal.menu.internal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import id.my.rascal.menu.internal.entity.ModifierOption;

public interface ModifierOptionRepository extends JpaRepository<ModifierOption, Long> {
    // [OPENCODE/REVIEW] Ambil semua option berdasarkan id parent modifier type
    List<ModifierOption> findByModifierTypeId(Long modifierTypeId);

    // [OPENCODE/REVIEW] Query efisien by parent & id (validasi kepemilikan)
    Optional<ModifierOption> findByModifierTypeIdAndId(Long modifierTypeId, Long id);
}
