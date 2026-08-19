package id.my.rascal.common.seed;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;

/**
 * Idempotent, chunked seeding helper.
 * <p>
 * Processes an ordered list sequentially in chunks: per chunk it loads the
 * names that already exist with a single query, inserts only the missing
 * ones, then flushes and clears the persistence context so memory stays
 * bounded to one chunk at a time regardless of total dataset size.
 */
@Component
public class ChunkedSeederSupport {

    private static final int CHUNK_SIZE = 100;

    private final EntityManager entityManager;

    public ChunkedSeederSupport(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public <T, E> void seedInChunks(
            List<T> items,
            Function<T, String> nameOf,
            Function<T, E> newEntity,
            Function<Collection<String>, List<String>> existingNames,
            Consumer<List<E>> saveAll) {

        for (int i = 0; i < items.size(); i += CHUNK_SIZE) {
            List<T> chunk = items.subList(i, Math.min(i + CHUNK_SIZE, items.size()));

            List<String> names = chunk.stream().map(nameOf).toList();
            Set<String> existing = new HashSet<>(existingNames.apply(names));

            List<E> entities = chunk.stream()
                .filter(item -> !existing.contains(nameOf.apply(item)))
                .map(newEntity)
                .toList();

            if (!entities.isEmpty()) {
                saveAll.accept(entities);
            }

            entityManager.flush();
            entityManager.clear();
        }
    }

}
