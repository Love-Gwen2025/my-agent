package com.ynp.agent.service.file;

import com.ynp.agent.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 文件存储服务，管理上传与删除。
 */
@Service
public class FileStorageService {

    private final AppProperties appProperties;

    private final Path uploadDir;

    public FileStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.uploadDir = initUploadDir();
    }

    /**
     * 1. 创建上传目录。
     */
    private Path initUploadDir() {
        Path baseDir = Paths.get(appProperties.getDataDir()).toAbsolutePath();
        Path uploadPath = baseDir.resolve("uploads");
        try {
            if (!Files.exists(uploadPath)) {
                /* 1. 递归创建上传目录 */
                Files.createDirectories(uploadPath);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建上传目录", ex);
        }
        return uploadPath;
    }

    /**
     * 1. 保存单个文件，返回存储信息。
     */
    public StoredFile storeFile(MultipartFile file) throws IOException {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new IllegalArgumentException("文件内容为空");
        }
        /* 1. 生成带扩展名的唯一文件名 */
        String ext = "";
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String storedName = buildUniqueName(ext);
        Path target = uploadDir.resolve(storedName);
        /* 2. 写入磁盘 */
        file.transferTo(target.toFile());
        String safeOriginal = StringUtils.hasText(original) ? original : storedName;
        return new StoredFile(safeOriginal, storedName, file.getContentType(), file.getSize());
    }

    /**
     * 1. 批量保存文件。
     */
    public List<StoredFile> storeFiles(List<MultipartFile> files) throws IOException {
        List<StoredFile> stored = new ArrayList<>();
        for (MultipartFile file : files) {
            stored.add(storeFile(file));
        }
        return stored;
    }

    /**
     * 1. 删除文件列表。
     */
    public void deleteFiles(List<String> storedNames) {
        for (String storedName : storedNames) {
            if (!StringUtils.hasText(storedName)) {
                continue;
            }
            Path target = uploadDir.resolve(storedName);
            try {
                /* 1. 若文件存在则删除，不影响主流程 */
                Files.deleteIfExists(target);
            } catch (IOException ex) {
                // 保持静默，防止阻断业务流程
            }
        }
    }

    /**
     * 1. 生成唯一文件名。
     */
    private String buildUniqueName(String ext) {
        String safeExt = StringUtils.hasText(ext) ? ext : "";
        String randomPart = UUID.randomUUID().toString().replace("-", "");
        String timePart = String.valueOf(Instant.now().toEpochMilli());
        return timePart + "-" + randomPart + safeExt;
    }

    /**
     * 1. 获取上传目录路径。
     */
    public Path getUploadDir() {
        return uploadDir;
    }
}
