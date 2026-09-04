package id.my.rascal.image.internal.adapter;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.util.StringUtil;
import id.my.rascal.image.api.ImageRegistryApi;
import id.my.rascal.image.internal.entity.ImageMetadata;
import id.my.rascal.image.internal.repository.ImageMetadataRepository;

@Component
public class ImageRegistryApiImpl implements ImageRegistryApi {

    private final ImageMetadataRepository imageMetadataRepository;

    public ImageRegistryApiImpl(ImageMetadataRepository imageMetadataRepository) {
        this.imageMetadataRepository = imageMetadataRepository;
    }

    @Override
    @Transactional
    public String registerOrUpdate(String fileId, String filePath) {
        if (StringUtil.safeIsBlank(fileId) || StringUtil.safeIsBlank(filePath)) return null;

        ImageMetadata metadata = imageMetadataRepository.findByFileId(fileId)
            .orElseGet(ImageMetadata::new);
        String previousFilePath = metadata.getFilePath();

        if (metadata.getId() == null) {
            metadata.setFileId(fileId);
            metadata.setCreatedAt(LocalDateTime.now());
        }
        metadata.setFilePath(filePath);
        metadata.setUpdatedAt(LocalDateTime.now());
        imageMetadataRepository.save(metadata);

        return previousFilePath;
    }

    @Override
    @Transactional
    public String resolveAndDelete(String fileId) {
        if (StringUtil.safeIsBlank(fileId)) return null;

        return imageMetadataRepository.findByFileId(fileId)
            .map(metadata -> {
                String filePath = metadata.getFilePath();
                imageMetadataRepository.delete(metadata);
                return filePath;
            })
            .orElse(null);
    }

}
