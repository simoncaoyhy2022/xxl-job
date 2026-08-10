package com.xxl.job.executor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * Syncs published script directories from xxl-job-admin to an executor-local cache.
 * Kettle kjb files reference sibling ktr files via ${Internal.Entry.Current.Directory},
 * so the whole directory is synced instead of a single file.
 */
@Service
public class ScriptArtifactService {
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;
    @Value("${xxl.job.file.api-token:}")
    private String apiToken;
    @Value("${xxl.job.executor.script-cache-path:${java.io.tmpdir}/xxl-job-script-cache}")
    private String cachePath;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Path resolve(int scriptId, String requiredSubtype) throws Exception {
        if (apiToken == null || apiToken.isBlank()) throw new IllegalStateException("xxl.job.file.api-token 未配置");
        BundleManifest manifest = fetchBundle(scriptId);
        if (!requiredSubtype.equalsIgnoreCase(manifest.entrySubtype))
            throw new IllegalArgumentException("脚本类型不匹配，需要 " + requiredSubtype);
        syncDirectory(manifest);
        Path entry = cacheRoot().resolve(manifest.entryRelativePath.replace('\\', '/')).normalize();
        if (!entry.startsWith(cacheRoot()) || !Files.isRegularFile(entry))
            throw new IllegalStateException("脚本缓存不可用");
        return entry;
    }

    private BundleManifest fetchBundle(int scriptId) throws Exception {
        Map<?, ?> root = exchangeJson("/scriptfile/api/" + scriptId + "/bundle");
        if (!(root.get("code") instanceof Number) || ((Number) root.get("code")).intValue() != 200)
            throw new IllegalStateException("脚本不可用");
        Map<?, ?> data = (Map<?, ?>) root.get("data");
        if (data == null) throw new IllegalStateException("脚本不可用");
        BundleManifest manifest = new BundleManifest();
        manifest.entryScriptId = scriptId;
        manifest.entryRelativePath = String.valueOf(data.get("entryRelativePath")).replace('\\', '/');
        Object files = data.get("files");
        if (!(files instanceof List<?> fileList) || fileList.isEmpty())
            throw new IllegalStateException("脚本目录为空");
        for (Object item : fileList) {
            if (!(item instanceof Map<?, ?> file)) continue;
            ScriptFileEntry entry = new ScriptFileEntry();
            entry.id = ((Number) file.get("id")).intValue();
            entry.relativePath = String.valueOf(file.get("relativePath")).replace('\\', '/');
            entry.sha256 = String.valueOf(file.get("sha256"));
            entry.scriptSubtype = String.valueOf(file.get("scriptSubtype"));
            manifest.files.add(entry);
            if (entry.id == scriptId) manifest.entrySubtype = entry.scriptSubtype;
        }
        if (manifest.entrySubtype == null) throw new IllegalStateException("脚本不可用");
        return manifest;
    }

    private void syncDirectory(BundleManifest manifest) throws Exception {
        for (ScriptFileEntry file : manifest.files) ensureCached(file);
    }

    private void ensureCached(ScriptFileEntry file) throws Exception {
        Path cache = cacheRoot().resolve(file.relativePath).normalize();
        if (!cache.startsWith(cacheRoot())) throw new IllegalArgumentException("非法缓存路径");
        if (Files.isRegularFile(cache) && file.sha256.equalsIgnoreCase(sha256(cache))) return;
        Files.createDirectories(cache.getParent());
        Path temporary = Files.createTempFile(cache.getParent(), "download-", ".tmp");
        try {
            ResponseEntity<byte[]> download = restTemplate.exchange(
                    adminBase() + "/scriptfile/api/" + file.id + "/download",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    byte[].class);
            if (!download.getStatusCode().is2xxSuccessful() || download.getBody() == null)
                throw new IllegalStateException("下载脚本失败: " + file.relativePath);
            Files.write(temporary, download.getBody(), StandardOpenOption.TRUNCATE_EXISTING);
            if (!file.sha256.equalsIgnoreCase(sha256(temporary)))
                throw new IllegalStateException("脚本校验失败: " + file.relativePath);
            try {
                Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Map<?, ?> exchangeJson(String path) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                adminBase() + path,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class);
        return objectMapper.readValue(response.getBody(), Map.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-XXL-JOB-FILE-TOKEN", apiToken);
        return headers;
    }

    private String adminBase() {
        return adminAddresses.split(",")[0].replaceAll("/+$", "");
    }

    private Path cacheRoot() {
        return Paths.get(cachePath).toAbsolutePath().normalize();
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) md.update(b, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : md.digest()) out.append(String.format("%02x", b));
        return out.toString();
    }

    private static final class BundleManifest {
        private int entryScriptId;
        private String entryRelativePath;
        private String entrySubtype;
        private final java.util.ArrayList<ScriptFileEntry> files = new java.util.ArrayList<>();
    }

    private static final class ScriptFileEntry {
        private int id;
        private String relativePath;
        private String sha256;
        private String scriptSubtype;
    }
}
