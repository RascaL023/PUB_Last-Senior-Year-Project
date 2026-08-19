package id.my.rascal.common.image;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import id.my.rascal.common.util.StringUtil;

/**
 * ImageKit-backed {@link ImageService}. DB never stores full URLs — only relative
 * paths like {@code /assets/images/menus/<category_code>/nama_file}. This
 * implementation resolves those paths against the ImageKit URL endpoint and
 * deletes files by path using ImageKit's delete-by-query API.
 */
@Service
public class ImageKitImageService implements ImageService {

    private static final String HMAC_ALGO = "HmacSHA1";
    private static final long AUTH_TTL_SECONDS = 3600L;

    private final String urlEndpoint;
    private final String publicKey;
    private final String privateKey;
    private final RestClient apiClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public ImageKitImageService(
        @Value("${imagekit.url-endpoint:}") String urlEndpoint,
        @Value("${imagekit.public-key:}") String publicKey,
        @Value("${imagekit.private-key:}") String privateKey
    ) {
        this.urlEndpoint = trimTrailingSlash(urlEndpoint);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.apiClient = RestClient.builder()
            .baseUrl("https://api.imagekit.io/v1")
            .defaultHeader("Authorization", basicAuth(privateKey))
            .build();
    }

    @Override
    public String buildPath(String folderPath, String fileName) {
        String folder = normalizeFolder(folderPath);
        String name = StringUtil.normalizeSpaces(fileName);
        if (StringUtil.safeIsBlank(name))
            throw new IllegalArgumentException("File name cannot be blank");

        return folder + "/" + sanitizeFileName(name);
    }

    @Override
    public String resolveUrl(String path) {
        if (StringUtil.safeIsBlank(path))
            return "";

        return urlEndpoint + (path.startsWith("/") ? path : "/" + path);
    }

    @Override
    public void deleteByPath(String path) {
        if (StringUtil.safeIsBlank(path)) return;

        apiClient.post()
            .uri("/files/deleteByQuery")
            .body(Map.of("filePath", path, "force", true))
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public ImageUploadAuth generateUploadAuth() {
        String token = randomToken();
        long expire = Instant.now().getEpochSecond() + AUTH_TTL_SECONDS;
        String signature = hmacSha1Hex(privateKey, token + expire);

        return new ImageUploadAuth(publicKey, token, expire, signature);
    }

    private String randomToken() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++)
            sb.append((char) ('a' + secureRandom.nextInt(26)));
        return sb.toString();
    }

    private String hmacSha1Hex(String key, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            return toHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("ImageKit auth signing failed", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02x", b));

        return sb.toString();
    }

    private String normalizeFolder(String folderPath) {
        if (StringUtil.safeIsBlank(folderPath))
            return "";

        String folder = folderPath.trim();
        if (folder.endsWith("/"))
            folder = folder.substring(0, folder.length() - 1);

        return folder.startsWith("/") ? folder : "/" + folder;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String trimTrailingSlash(String url) {
        if (StringUtil.safeIsBlank(url))
            return "";

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String basicAuth(String privateKey) {
        return "Basic " + Base64.getEncoder().encodeToString((privateKey + ":").getBytes());
    }
}
