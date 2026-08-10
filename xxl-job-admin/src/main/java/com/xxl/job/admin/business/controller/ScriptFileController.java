package com.xxl.job.admin.business.controller;

import com.xxl.job.admin.business.mapper.XxlJobScriptFileMapper;
import com.xxl.job.admin.business.model.ScriptDirectoryBundle;
import com.xxl.job.admin.business.model.ScriptRepositoryItem;
import com.xxl.job.admin.business.model.XxlJobScriptFile;
import com.xxl.job.admin.framework.constant.Consts;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Admin-side script repository. Files are deliberately kept outside the web root.
 */
@Controller
@RequestMapping("/scriptfile")
public class ScriptFileController {
    // Excel files are Kettle script resources, not executable handler targets.
    private static final Set<String> KETTLE = Set.of("ktr", "kjb", "xls", "xlsx");
    private static final Set<String> HOP = Set.of("hpl", "hwf", "hwl", "xls", "xlsx");
    private static final Set<String> PYTHON = Set.of("py", "xls", "xlsx");
    @Resource
    private XxlJobScriptFileMapper scriptFileMapper;
    @Value("${xxl.job.file.repository-path:./data/script-repository}")
    private String repositoryPath;
    @Value("${xxl.job.file.upload-temp-path:./data/script-upload-temp}")
    private String tempPath;
    @Value("${xxl.job.file.max-size:104857600}")
    private long maxSize;
    @Value("${xxl.job.file.api-token:}")
    private String apiToken;

    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        return "business/script.file";
    }

    /**
     * Compatibility endpoint retained for callers of the original table view.
     */
    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<XxlJobScriptFile>> pageList(@RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "10") int pagesize, String name, String scriptType) {
        PageModel<XxlJobScriptFile> page = new PageModel<>();
        page.setData(scriptFileMapper.pageList(offset, pagesize, name, scriptType));
        page.setTotal(scriptFileMapper.pageListCount(name, scriptType));
        return Response.ofSuccess(page);
    }

    /**
     * Lists direct children only; the browser combines this endpoint into a directory tree.
     */
    @GetMapping("/entries")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<ScriptRepositoryItem>> entries(@RequestParam(defaultValue = "") String path) {
        try {
            if (normalizeRelative(path).isEmpty()) ensureTypeRoots();
            Path directory = resolveDirectory(path);
            if (!Files.isDirectory(directory)) return Response.ofFail("目录不存在");
            Map<String, XxlJobScriptFile> files = scriptFileMapper.findByRelativePathPrefix(normalizeRelative(path)).stream()
                    .collect(Collectors.toMap(XxlJobScriptFile::getRelativePath, file -> file, (a, b) -> a));
            try (Stream<Path> children = Files.list(directory)) {
                List<ScriptRepositoryItem> result = children.map(child -> toItem(child, files.get(relative(child))))
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(ScriptRepositoryItem::isDirectory).reversed()
                                .thenComparing(ScriptRepositoryItem::getName, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
                return Response.ofSuccess(result);
            }
        } catch (Exception ex) {
            return Response.ofFail("读取目录失败：" + ex.getMessage());
        }
    }

    @GetMapping("/directories")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<ScriptRepositoryItem>> directories() {
        try {
            ensureTypeRoots();
            try (Stream<Path> paths = Files.walk(repositoryRoot())) {
                List<ScriptRepositoryItem> result = paths.filter(Files::isDirectory).filter(path -> !path.equals(repositoryRoot()))
                        .sorted(Comparator.comparing(this::relative, String.CASE_INSENSITIVE_ORDER)).map(path -> toItem(path, null))
                        .collect(Collectors.toList());
                return Response.ofSuccess(result);
            }
        } catch (Exception ex) {
            return Response.ofFail("读取目录树失败：" + ex.getMessage());
        }
    }

    @PostMapping("/directory")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> createDirectory(@RequestParam(defaultValue = "") String parentPath, @RequestParam String name) {
        try {
            String folder = safeSegment(name);
            Path parent = resolveDirectory(parentPath);
            if (!Files.isDirectory(parent)) return Response.ofFail("父目录不存在");
            Path target = parent.resolve(folder).normalize();
            if (!target.startsWith(repositoryRoot())) return Response.ofFail("非法目录路径");
            if (Files.exists(target)) return Response.ofFail("同名目录或文件已存在");
            Files.createDirectories(target);
            return Response.ofSuccess(relative(target));
        } catch (Exception ex) {
            return Response.ofFail("创建目录失败：" + ex.getMessage());
        }
    }

    @PostMapping("/directory/delete")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> deleteDirectory(@RequestParam String path) {
        try {
            String normalized = normalizeRelative(path);
            if (normalized.isEmpty()) return Response.ofFail("不能删除脚本仓库根目录");
            Path target = resolveDirectory(normalized);
            if (!Files.isDirectory(target)) return Response.ofFail("目录不存在");
            List<XxlJobScriptFile> records = scriptFileMapper.findByRelativePathPrefix(normalized + "/");
            try (Stream<Path> walk = Files.walk(target)) {
                walk.sorted(Comparator.reverseOrder()).forEach(this::deletePath);
            }
            for (XxlJobScriptFile record : records) scriptFileMapper.remove(record.getId());
            return Response.ofSuccess();
        } catch (Exception ex) {
            return Response.ofFail("删除目录失败：" + ex.getMessage());
        }
    }

    @PostMapping("/upload")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public synchronized Response<String> upload(@RequestParam(required = false) Integer id, @RequestParam(required = false) String name, @RequestParam String scriptType,
                                                @RequestParam(required = false) String remark, @RequestParam(required = false) String directory,
                                                @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            if (file == null || file.isEmpty())
                return Response.ofFail("请选择要上传的文件");
            if (file.getSize() > maxSize) return Response.ofFail("文件超过允许大小");
            String type = scriptType == null ? "" : scriptType.trim().toUpperCase(Locale.ROOT);
            String original = safeFilename(file.getOriginalFilename());
            String displayName = StringTool.isBlank(name) ? original : name.trim();
            String ext = extension(original);
            if (!allowed(type, ext)) return Response.ofFail("脚本类型与文件扩展名不匹配");
            LoginInfo login = XxlSsoHelper.loginCheckWithAttr(request).getData();
            String user = login == null ? "system" : login.getUserName();
            XxlJobScriptFile target = id == null ? null : scriptFileMapper.load(id);
            if (id != null && target == null) return Response.ofFail("文件不存在");
            Path previous = target == null || StringTool.isBlank(target.getRelativePath()) ? null : resolve(target);
            String requestedDirectory = normalizeRelative(directory);
            String defaultDirectory = type.toLowerCase(Locale.ROOT);
            String actualDirectory = target != null && requestedDirectory.isEmpty() ? relative(previous.getParent()) : (requestedDirectory.isEmpty() ? defaultDirectory : requestedDirectory);
            validateDirectoryForType(actualDirectory, type);
            Path parent = resolveDirectory(actualDirectory);
            Files.createDirectories(parent);
            Date now = new Date();
            if (target == null) {
                target = new XxlJobScriptFile();
                target.setCreateUser(user);
                target.setCreateTime(now);
                target.setStatus(1);
            }
            Path destination = parent.resolve(original).normalize();
            if (!destination.startsWith(repositoryRoot())) return Response.ofFail("非法文件路径");
            String destinationPath = relative(destination);
            Integer currentFileId = target.getId();
            boolean occupied = scriptFileMapper.findByRelativePathPrefix(destinationPath).stream()
                    .anyMatch(record -> destinationPath.equals(record.getRelativePath()) && (currentFileId == null || !currentFileId.equals(record.getId())));
            if (occupied) return Response.ofFail("当前目录已存在同名脚本");
            Files.createDirectories(Paths.get(tempPath).toAbsolutePath().normalize());
            Path temporary = Files.createTempFile(Paths.get(tempPath).toAbsolutePath().normalize(), "upload-", ".tmp");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha256 = sha256(temporary);
            Path staged = Files.createTempFile(destination.getParent(), ".upload-", ".tmp");
            try {
                Files.copy(temporary, staged, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(staged, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(staged, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
                Files.deleteIfExists(staged);
            }
            if (previous != null && !previous.equals(destination)) Files.deleteIfExists(previous);
            target.setName(displayName);
            target.setScriptType(type);
            target.setScriptSubtype(ext.toUpperCase(Locale.ROOT));
            target.setOriginalFilename(original);
            target.setRelativePath(destinationPath);
            target.setFileSize(Files.size(destination));
            target.setSha256(sha256);
            target.setStatus(1);
            target.setRemark(remark);
            target.setUpdateUser(user);
            target.setUpdateTime(now);
            if (target.getId() == null) scriptFileMapper.save(target);
            else scriptFileMapper.update(target);
            return Response.ofSuccess(String.valueOf(target.getId()));
        } catch (Exception ex) {
            return Response.ofFail("上传失败：" + ex.getMessage());
        }
    }

    @PostMapping("/update")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> update(@RequestParam int id, @RequestParam String name, @RequestParam(required = false) String remark, @RequestParam(defaultValue = "1") int status, HttpServletRequest request) {
        XxlJobScriptFile item = scriptFileMapper.load(id);
        if (item == null || StringTool.isBlank(name) || (status != 0 && status != 1))
            return Response.ofFail("参数错误");
        item.setName(name.trim());
        item.setRemark(remark);
        item.setStatus(status);
        item.setUpdateTime(new Date());
        LoginInfo login = XxlSsoHelper.loginCheckWithAttr(request).getData();
        item.setUpdateUser(login == null ? "system" : login.getUserName());
        return scriptFileMapper.update(item) > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    @PostMapping("/delete")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> delete(@RequestParam("ids[]") List<Integer> ids) {
        if (ids == null || ids.size() != 1) return Response.ofFail("请选择一条文件记录");
        XxlJobScriptFile item = scriptFileMapper.load(ids.get(0));
        if (item == null) return Response.ofSuccess();
        try {
            Files.deleteIfExists(resolve(item));
        } catch (IOException ex) {
            return Response.ofFail("删除文件失败：" + ex.getMessage());
        }
        return scriptFileMapper.remove(item.getId()) > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    @GetMapping("/download/{id}")
    @XxlSso(role = Consts.ADMIN_ROLE)
    public ResponseEntity<FileSystemResource> download(@PathVariable int id) throws IOException {
        return fileResponse(required(id));
    }

    /**
     * Internal endpoint for executors. Configure the same non-empty api-token on admin and executor.
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    @XxlSso(login = false)
    public Response<XxlJobScriptFile> apiInfo(@PathVariable int id, @RequestHeader(value = "X-XXL-JOB-FILE-TOKEN", required = false) String token) {
        if (!internalTokenValid(token)) return Response.ofFail("unauthorized");
        XxlJobScriptFile item = scriptFileMapper.load(id);
        return item != null && item.getStatus() == 1 && Files.isRegularFile(resolve(item)) ? Response.ofSuccess(item) : Response.ofFail("script unavailable");
    }

    @GetMapping("/api/{id}/download")
    @XxlSso(login = false)
    public ResponseEntity<FileSystemResource> apiDownload(@PathVariable int id, @RequestHeader(value = "X-XXL-JOB-FILE-TOKEN", required = false) String token) throws IOException {
        if (!internalTokenValid(token)) return ResponseEntity.status(403).build();
        return fileResponse(required(id));
    }

    /**
     * Returns all enabled scripts in the same directory as the entry script.
     * Executors use this manifest to sync sibling files (e.g. kjb + ktr) before Kettle runs.
     */
    @GetMapping("/api/{id}/bundle")
    @ResponseBody
    @XxlSso(login = false)
    public Response<ScriptDirectoryBundle> apiBundle(@PathVariable int id, @RequestHeader(value = "X-XXL-JOB-FILE-TOKEN", required = false) String token) {
        if (!internalTokenValid(token)) return Response.ofFail("unauthorized");
        XxlJobScriptFile entry = scriptFileMapper.load(id);
        if (entry == null || entry.getStatus() != 1 || !Files.isRegularFile(resolve(entry)))
            return Response.ofFail("script unavailable");
        String directory = parentDirectory(entry.getRelativePath());
        String dirPrefix = directory.isEmpty() ? "" : directory + "/";
        List<XxlJobScriptFile> files = scriptFileMapper.findByRelativePathPrefix(dirPrefix).stream()
                .filter(file -> file.getStatus() == 1)
                .filter(file -> isDirectChild(file.getRelativePath(), dirPrefix))
                .filter(file -> {
                    try {
                        return Files.isRegularFile(resolve(file));
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(XxlJobScriptFile::getRelativePath, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        if (files.stream().noneMatch(file -> Objects.equals(file.getId(), entry.getId())))
            return Response.ofFail("script unavailable");
        ScriptDirectoryBundle bundle = new ScriptDirectoryBundle();
        bundle.setDirectory(directory);
        bundle.setEntryScriptId(entry.getId());
        bundle.setEntryRelativePath(entry.getRelativePath());
        bundle.setFiles(files);
        return Response.ofSuccess(bundle);
    }

    private ScriptRepositoryItem toItem(Path child, XxlJobScriptFile file) {
        try {
            ScriptRepositoryItem item = new ScriptRepositoryItem();
            item.setName(child.getFileName().toString());
            item.setPath(relative(child));
            item.setDirectory(Files.isDirectory(child));
            if (!item.isDirectory()) {
                // Only expose managed script files; stray files never become downloadable through this page.
                if (file == null) return null;
                item.setId(file.getId());
                item.setScriptType(file.getScriptType());
                item.setScriptSubtype(file.getScriptSubtype());
                item.setFileSize(file.getFileSize());
                item.setStatus(file.getStatus());
                item.setUpdateTime(file.getUpdateTime());
            }
            return item;
        } catch (Exception ex) {
            return null;
        }
    }

    private XxlJobScriptFile required(int id) throws IOException {
        XxlJobScriptFile f = scriptFileMapper.load(id);
        if (f == null || f.getStatus() != 1 || !Files.isRegularFile(resolve(f)))
            throw new NoSuchFileException("script unavailable");
        return f;
    }

    private ResponseEntity<FileSystemResource> fileResponse(XxlJobScriptFile item) throws IOException {
        Path p = resolve(item);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(Files.size(p)).eTag(item.getSha256()).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(item.getOriginalFilename(), StandardCharsets.UTF_8).build().toString()).body(new FileSystemResource(p));
    }

    private Path repositoryRoot() {
        return Paths.get(repositoryPath).toAbsolutePath().normalize();
    }

    private Path resolve(XxlJobScriptFile f) {
        return resolvePath(f.getRelativePath());
    }

    private Path resolveDirectory(String relativePath) {
        return resolvePath(relativePath);
    }

    private Path resolvePath(String relativePath) {
        Path p = repositoryRoot().resolve(normalizeRelative(relativePath)).normalize();
        if (!p.startsWith(repositoryRoot())) throw new IllegalArgumentException("非法仓库路径");
        return p;
    }

    private String relative(Path path) {
        return repositoryRoot().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private String normalizeRelative(String path) {
        if (path == null || path.trim().isEmpty()) return "";
        Path value = Paths.get(path.replace('\\', '/')).normalize();
        if (value.isAbsolute() || value.startsWith("..")) throw new IllegalArgumentException("非法目录路径");
        String normalized = value.toString().replace('\\', '/');
        return ".".equals(normalized) ? "" : normalized;
    }

    private void validateDirectoryForType(String directory, String type) {
        String expected = type.toLowerCase(Locale.ROOT);
        if (directory.isEmpty() || !(directory.equals(expected) || directory.startsWith(expected + "/")))
            throw new IllegalArgumentException("请在 " + expected + " 目录下上传此类脚本");
    }

    private void ensureTypeRoots() throws IOException {
        for (String type : List.of("kettle", "hop", "python")) Files.createDirectories(repositoryRoot().resolve(type));
    }

    private String safeSegment(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isBlank() || value.equals(".") || value.equals("..") || value.matches(".*[\\\\/:*?\"<>|].*"))
            throw new IllegalArgumentException("目录名包含非法字符");
        return value;
    }

    private boolean internalTokenValid(String token) {
        return !StringTool.isBlank(apiToken) && token != null && MessageDigest.isEqual(apiToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean allowed(String type, String ext) {
        return ("KETTLE".equals(type) && KETTLE.contains(ext)) || ("HOP".equals(type) && HOP.contains(ext)) || ("PYTHON".equals(type) && PYTHON.contains(ext));
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 1 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String parentDirectory(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index < 0 ? "" : normalized.substring(0, index);
    }

    private static boolean isDirectChild(String relativePath, String dirPrefix) {
        String normalized = relativePath.replace('\\', '/');
        if (!dirPrefix.isEmpty() && !normalized.startsWith(dirPrefix)) return false;
        String remainder = dirPrefix.isEmpty() ? normalized : normalized.substring(dirPrefix.length());
        return !remainder.isEmpty() && !remainder.contains("/");
    }

    private static String safeFilename(String name) {
        String n = name == null ? "" : Paths.get(name).getFileName().toString();
        if (n.isBlank() || n.contains("..")) throw new IllegalArgumentException("非法文件名");
        return n.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void deletePath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
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
}
