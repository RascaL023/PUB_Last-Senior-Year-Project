package id.my.rascal.image.internal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import id.my.rascal.image.internal.entity.ImageMetadata;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {

    Optional<ImageMetadata> findByFileId(String fileId);

    Optional<ImageMetadata> findByFilePath(String filePath);

    void deleteByFileId(String fileId);

}
