package id.my.rascal.imagekit.internal.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.image.api.ImageApi;
import id.my.rascal.image.api.ImageUploadAuthApiResponse;
import id.my.rascal.image.api.ImageWebhookApiPayload;
import id.my.rascal.imagekit.internal.component.ImageKitProperties;
import id.my.rascal.imagekit.internal.model.response.ImageKitWebhookPayloadResponse;

@Service
public class ImageKitService implements ImageApi {

    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long AUTH_TTL_SECONDS = 3600L;
    private static final Logger log = LoggerFactory.getLogger(ImageKitService.class);

    private final ImageKitProperties properties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;
    private final RestClient apiClient;

    public ImageKitService(ImageKitProperties properties, ObjectMapper objectMapper, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
        this.objectMapper = objectMapper;
        this.apiClient = RestClient.builder()
            .baseUrl(this.properties.baseUrl())
            .defaultHeader("Authorization", basicAuth(properties.privateKey()))
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

        String endpoint = trimTrailingSlash(properties.urlEndpoint());
        return endpoint + (path.startsWith("/") ? path : "/" + path);
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
    public ImageUploadAuthApiResponse getAuthenticationParameters() {
        String token = randomToken();
        long expire = Instant.now().getEpochSecond() + AUTH_TTL_SECONDS;
        String signature = hmacHex(HMAC_SHA1, properties.privateKey(), token + expire);

        return new ImageUploadAuthApiResponse(properties.publicKey(), token, expire, signature);
    }

    public ImageWebhookApiPayload verifyAndParseWebhook(String rawPayload, Map<String, String> headers, String secret) {
        Map<String, String> normalized = normalizeHeaders(headers);
        String effectiveSecret = StringUtil.safeIsBlank(secret) ? properties.webhooksSecret() : secret;
        if (StringUtil.safeIsBlank(effectiveSecret))
            log.warn("ImageKit webhook secret is blank, signature verification will fail bruh!");

        String strategy = verifyAnyStrategy(rawPayload, normalized, effectiveSecret);
        if (strategy == null)
            throw new BadRequestException("Invalid ImageKit webhook signature");

        try {
            ImageKitWebhookPayloadResponse payload = objectMapper.readValue(rawPayload, ImageKitWebhookPayloadResponse.class);
            ImageKitWebhookPayloadResponse.ImageKitWebhookDataResponse data = payload.data();
            return new ImageWebhookApiPayload(
                payload.type(),
                data == null ? null : data.fileId(),
                data == null ? null : data.filePath(),
                data == null ? null : data.url()
            );
        } catch (JsonProcessingException ex) { throw new BadRequestException("Invalid ImageKit webhook payload"); }
    }


    private String verifyAnyStrategy(String rawPayload, Map<String, String> headers, String secret) {
        if (StringUtil.safeIsBlank(secret))
            return null;
        if (headers.containsKey("webhook-signature")) {
            byte[] decoded = standardKeyBytes(secret);
            if (decoded != null && matchesStandardSignature(rawPayload, headers, decoded))
                return "standard-base64";
            if (matchesStandardSignature(rawPayload, headers, secret.getBytes(StandardCharsets.UTF_8)))
                return "standard-raw";
            String stripped = secret.startsWith("whsec_") ? secret.substring(6) : secret;
            if (matchesStandardSignature(rawPayload, headers, stripped.getBytes(StandardCharsets.UTF_8)))
                return "standard-stripped-raw";
        }
        if (headers.containsKey("x-ik-signature") && matchesLegacySignature(rawPayload, headers.get("x-ik-signature"), secret))
            return "legacy";
        return null;
    }

    private boolean matchesStandardSignature(String rawPayload, Map<String, String> headers, byte[] key) {
        String id = headers.get("webhook-id");
        String timestamp = headers.get("webhook-timestamp");
        String signatureHeader = headers.get("webhook-signature");
        if (StringUtil.safeIsBlank(id) || StringUtil.safeIsBlank(timestamp) || StringUtil.safeIsBlank(signatureHeader))
            return false;

        String signedContent = id + "." + timestamp + "." + rawPayload;
        byte[] expected = hmacBytes(HMAC_SHA256, key, signedContent);

        for (String part : signatureHeader.split(" ")) {
            String candidate = part.startsWith("v1,") ? part.substring(3) : part;
            byte[] actual;
            try { actual = Base64.getDecoder().decode(candidate); } 
            catch (IllegalArgumentException ex) { continue; }
            if (MessageDigest.isEqual(expected, actual)) return true;
        }

        return false;
    }

    private boolean matchesLegacySignature(String rawPayload, String signatureHeader, String secret) {
        if (StringUtil.safeIsBlank(signatureHeader) || StringUtil.safeIsBlank(secret))
            return false;

        String timestamp = null;
        String signature = null;
        for (String item : signatureHeader.split(",")) {
            if (item.startsWith("t=")) timestamp = item.substring(2);
            if (item.startsWith("v1=")) signature = item.substring(3);
        }

        if (StringUtil.safeIsBlank(timestamp) || StringUtil.safeIsBlank(signature)) return false;

        String expected = hmacHex(HMAC_SHA256, secret, timestamp + "." + rawPayload);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private byte[] standardKeyBytes(String secret) {
        if (StringUtil.safeIsBlank(secret))
            return null;
        String stripped = secret.startsWith("whsec_") ? secret.substring(6) : secret;
        try { return Base64.getDecoder().decode(stripped); } 
        catch (IllegalArgumentException ex) { return null; }
    }

    private Map<String, String> normalizeHeaders(Map<String, String> headers) {
        Map<String, String> normalized = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null) normalized.putAll(headers);
        return normalized;
    }

    private String randomToken() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) sb.append((char) ('a' + secureRandom.nextInt(26)));
        return sb.toString();
    }

    private String hmacHex(String algorithm, String key, String message) {
        return toHex(hmacBytes(algorithm, key.getBytes(StandardCharsets.UTF_8), message));
    }

    private byte[] hmacBytes(String algorithm, byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("ImageKit signing failed", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
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
        if (StringUtil.safeIsBlank(url)) return "";

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String basicAuth(String privateKey) {
        return "Basic " + Base64.getEncoder().encodeToString((privateKey + ":").getBytes());
    }

}
