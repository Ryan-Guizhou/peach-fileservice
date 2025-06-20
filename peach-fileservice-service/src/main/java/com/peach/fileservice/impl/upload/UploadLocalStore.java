package com.peach.fileservice.impl.upload;

import com.amazonaws.services.s3.model.PartETag;
import com.peach.common.response.Response;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.common.exception.FileUploadException;
import com.peach.fileservice.impl.AbstractUploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description Local Upload Storage Implementation
 * @CreateTime 2025/6/20 18:28
 */
@Slf4j
public class UploadLocalStore extends AbstractUploadService {

    @Override
    protected boolean uploadFile(InputStream inputStream, String targetFilePath, String fileName) {
        try {
            File targetFile = new File(targetFilePath, fileName);
            FileUtils.copyInputStreamToFile(inputStream, targetFile);
            return true;
        } catch (IOException e) {
            log.error("Local uploadFile error", e);
            throw new FileUploadException("Local uploadFile error", e);
        }
    }

    @Override
    protected PartETag uploadFilePart(String uploadId, int chunk, byte[] data, String partUploadPath, String partUploadFileName, String fullFileKey) {
        File partDir = new File(partUploadPath);
        if (!partDir.exists()) {
            partDir.mkdirs();
        }
        File chunkFile = new File(partDir, partUploadFileName);
        try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
            fos.write(data);
            return new PartETag(chunk + 1, String.valueOf(chunkFile.length()));
        } catch (IOException e) {
            log.error("Local uploadFilePart error", e);
            throw new FileUploadException("Local uploadFilePart error", e);
        }
    }

    @Override
    protected boolean mergePart(String uploadId, String chunkPath, String targetPath, String finalName, Function<Response, Void> dealwithChunkInfoFun) {
        File chunkDir = new File(chunkPath);
        File finalFile = new File(targetPath, finalName);
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            File[] partFiles = chunkDir.listFiles((dir, name) -> name.contains(FileConstant.FILE_NAME_SEPARATOR));
            if (partFiles == null || partFiles.length == 0) {
                return false;
            }
            List<File> sortedParts = Arrays.stream(partFiles).sorted(Comparator.comparingInt(f -> {
                        String[] parts = f.getName().split("-");
                        return Integer.parseInt(parts[1]);
                    })).collect(Collectors.toList());
            for (File part : sortedParts) {
                FileUtils.copyFile(part, fos);
            }
            if (dealwithChunkInfoFun != null) {
                dealwithChunkInfoFun.apply(Response.success());
            }
            return true;
        } catch (IOException e) {
            log.error("Local mergePart error", e);
            throw new FileUploadException("Local mergePart error", e);
        }
    }

    @Override
    protected int getParCount(String uploadId, String chunkPath, String fullFileKey) {
        File chunkDir = new File(chunkPath);
        if (!chunkDir.exists() || !chunkDir.isDirectory()) {
            return 0;
        }
        String[] files = chunkDir.list((dir, name) -> name.contains(FileConstant.FILE_NAME_SEPARATOR));
        return files == null ? 0 : files.length;
    }

    @Override
    protected boolean checkFileExist(String fullFileKey) {
        if (StringUtils.isBlank(fullFileKey)) {
            return false;
        }
        File file = new File(fullFileKey);
        return file.exists();
    }
}
