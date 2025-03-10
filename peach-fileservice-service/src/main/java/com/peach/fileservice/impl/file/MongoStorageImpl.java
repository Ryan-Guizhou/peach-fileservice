package com.peach.fileservice.impl.file;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.peach.fileservice.impl.AbstractFileStorageService;
import com.peach.fileservice.config.FileProperties;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/2/28 16:32
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "mongo")
public class MongoStorageImpl extends AbstractFileStorageService {

    private final MongoClient client;

    private final String dbName;

    private final GridFSBucket gridFSBucket;

    public MongoStorageImpl(FileProperties fileProperties) {
        this.client = MongoClients.create(fileProperties.getMongo().getUri());
        this.dbName = fileProperties.getMongo().getDbName();
        MongoDatabase database = client.getDatabase(dbName);
        this.gridFSBucket = GridFSBuckets.create(database);
        log.info("MongoStorageImpl initialized with database: {}", dbName);
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        return false;
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        return false;
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        try {
            GridFSUploadOptions options = new GridFSUploadOptions();
            ObjectId fileId = gridFSBucket.uploadFromStream(fileName, inputStream, options);
            log.info("File uploaded: {} with ID: {}", fileName, fileId.toHexString());
            return fileId.toHexString();
        } catch (Exception e) {
            log.error("File upload failed: {}", fileName, e);
            return null;
        }
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try {
            GridFSUploadOptions options = new GridFSUploadOptions();
            ObjectId fileId = gridFSBucket.uploadFromStream(fileName, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), options);
            log.info("File uploaded: {} with ID: {}", fileName, fileId.toHexString());
            return fileId.toHexString();
        } catch (Exception e) {
            log.error("File upload failed: {}", fileName, e);
            return null;
        }
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null){
            return Lists.newArrayList();
        }
        List<String> fileIdList = Lists.newArrayList();
        for (File file : files) {
            String fileId = upload(file, targetPath, file.getName());
            fileIdList.add(fileId);
        }
        return fileIdList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        try {
            GridFSUploadOptions options = new GridFSUploadOptions();
            ObjectId fileId = gridFSBucket.uploadFromStream(fileName, new FileInputStream(file), options);
            log.info("File uploaded: {} with ID: {}", fileName, fileId.toHexString());
            return fileId.toHexString();
        } catch (Exception e) {
            log.error("File upload failed: {}", fileName, e);
            return null;
        }
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        try (FileOutputStream outputStream = new FileOutputStream(localPath + "/" + fileName)) {
            gridFSBucket.downloadToStream(new ObjectId(targetPath), outputStream);
            log.info("File downloaded: {} to {}", fileName, localPath);
            return true;
        } catch (Exception e) {
            log.error("File download failed: {}", fileName, e);
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            gridFSBucket.delete(new ObjectId(key));
            log.info("File deleted: {}", key);
            return true;
        } catch (Exception e) {
            log.error("File delete failed: {}", key, e);
            return false;
        }
    }


    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        return null;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        return null;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        return false;
    }

    @Override
    public String getUrlByKey(String key) {
        return "";
    }

    @Override
    public String getPathByKey(String key) {
        return "";
    }

    @Override
    public void setPublicReadAcl(String path) {

    }
}
