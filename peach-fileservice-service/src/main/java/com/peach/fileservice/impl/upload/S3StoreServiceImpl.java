package com.peach.fileservice.impl.upload;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.google.common.collect.Lists;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.api.upload.S3Storage;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.common.exception.FileUploadException;
import com.peach.fileservice.common.util.StoreUtil;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.factory.S3ClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/20 15:25
 */
@Slf4j
public class S3StoreServiceImpl implements S3Storage {

    /**
     * 文件存储过期时间
     */
    private static final long EXPIRATION = 7 * 24 * 60 * 60L;

    @Override
    public PartETag uploadPart(String uploadId, int chunk, byte[] data, String fileFullKey) {
        String fullKey = StoreUtil.getFileFullKey(fileFullKey);
        PartETag partETag = null;
        try (ClosableS3Client client = S3ClientFactory.getS3Client()){
            FileProperties fileProperties = SpringUtil.getBean(FileProperties.class);
            UploadPartRequest uploadRequest = new UploadPartRequest()
                    .withBucketName(fileProperties.getS3().getBucketName())
                    .withKey(fullKey).withUploadId(uploadId)
                    .withPartNumber(chunk + 1)
                    .withPartSize(data.length)
                    .withInputStream(new ByteArrayInputStream(data));
            UploadPartResult uploadPartResult = client.getS3Client().uploadPart(uploadRequest);
            partETag = uploadPartResult.getPartETag();
        } catch (SdkClientException e) {
            log.error("SdkClientException error"+e.getMessage(), e);
            throw new FileUploadException("SdkClientException error"+e.getMessage(), e);
        } catch (Exception e) {
            log.error("uploadPart error,uploadId:{},chunk:{},fileFullKey:{}",uploadId,chunk,fileFullKey);
            throw new FileUploadException("uploadPart error",e);
        }
        return partETag;
    }

    @Override
    public String uploadFile(InputStream inputStream, String targetPath, String fileName) {
        String dealTargetPath = StoreUtil.getFileFullKey(targetPath);
        String localPath = StoreUtil.cleanPath(dealTargetPath);
        String bucketName = getBucketName();
        String fullKey = localPath + fileName;
        try (ClosableS3Client client = S3ClientFactory.getS3Client(); ByteArrayOutputStream buffer = new ByteArrayOutputStream(); InputStream in = inputStream){
            AmazonS3 s3Client = client.getS3Client();
            byte[] data = new byte[2048];
            int reader;
            while ((reader = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, reader);
            }
            buffer.flush();
            byte[] byteArray = buffer.toByteArray();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(byteArray.length);
            s3Client.putObject(new PutObjectRequest(bucketName, fullKey, new ByteArrayInputStream(byteArray), objectMetadata));
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            String ossUrl = s3Client.generatePresignedUrl(bucketName, StoreUtil.cleanFullKey(fullKey), expiration).toString();
            String url = StringUtil.EMPTY;
            if (ossUrl.contains(FileConstant.HTTPS_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, FileConstant.PATH_SEPARATOR);
            } else {
                url = ossUrl.replaceAll(FileConstant.HTTP_DOMAIN_REGEX, FileConstant.PATH_SEPARATOR);
            }
            return url;
        }catch (Exception e){
            log.error("uploadFile error,targetPath:{},fileName:{}",targetPath,fileName);
            throw new FileUploadException("uploadFile error",e);
        }
    }

    @Override
    public boolean mergePart(String uploadId, List<PartETag> partETagList, String fileFullKey) {
        String fullKey = StoreUtil.getFileFullKey(fileFullKey);
        try (ClosableS3Client client = S3ClientFactory.getS3Client()){
            String bucketName = getBucketName();
            List<PartETag> partList = partETagList.stream()
                    .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                    .collect(Collectors.toList());
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, fullKey, uploadId, partList);
            CompleteMultipartUploadResult result = client.getS3Client().completeMultipartUpload(compRequest);
            String eTag = result.getETag();
            return StringUtils.isNotBlank(eTag);
        }catch (Exception e){
            log.error("mergePart error,uploadId:{},fileFullKey:{}",uploadId,fileFullKey);
            throw new FileUploadException("mergePart error",e);
        }
    }

    @Override
    public boolean checkFileExist(String fileFullKey) {
        String fullKey = StoreUtil.getFileFullKey(fileFullKey);
        try (ClosableS3Client client = S3ClientFactory.getS3Client()){
            String bucketName = getBucketName();
            AmazonS3 s3Client = client.getS3Client();
            return s3Client.doesObjectExist(bucketName, fullKey);
        }catch (Exception e){
            log.error("checkFileExist error,fileFullKey:{}",fileFullKey);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean checkPartExist(int chunk, String uploadId, String fileFullKey) {
        String fullKey = StoreUtil.getFileFullKey(fileFullKey);
        try (ClosableS3Client client = S3ClientFactory.getS3Client()){
            AmazonS3 s3Client = client.getS3Client();
            String bucketName = getBucketName();
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName,fullKey,uploadId);
            PartListing partListing = s3Client.listParts(listPartsRequest);
            boolean isExist = Boolean.FALSE;
            for (PartSummary part : partListing.getParts()) {
                if (chunk + 1 == part.getPartNumber() && StringUtils.isNotBlank(part.getETag())) {
                    isExist = Boolean.TRUE;
                    break;
                }
            }
            return isExist;
        }catch (Exception e){
            log.error("checkPartExist error,chunk:{},uploadId:{},fileFullKey:{}",chunk,uploadId,fullKey);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PartSummary> partSummaryList(String uploadId, String fileFullKey) {
        String fileKey = StoreUtil.getFileFullKey(fileFullKey);
        try (ClosableS3Client client = S3ClientFactory.getS3Client()){
            AmazonS3 s3Client = client.getS3Client();
            String bucketName = getBucketName();
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, fileKey, uploadId);
            List<PartSummary> partList = Lists.newArrayList();
            boolean isTruncated = Boolean.TRUE;
            Date startTime = new Date();
            while (isTruncated) {
                long timeOut = DateUtil.between(startTime, new Date(), DateUnit.MINUTE);
                PartListing partListing = s3Client.listParts(listPartsRequest);
                partList.addAll(partListing.getParts());
                isTruncated = partListing.isTruncated();
                if (isTruncated) {
                    listPartsRequest.setPartNumberMarker(partListing.getNextPartNumberMarker());
                }
                if (timeOut >= 10) {
                    log.error("partSummaryList error wait 10 minutes timeOut, uploadId:｛｝，fileKey:｛｝",uploadId,fileKey);
                    isTruncated = false;
                }
            }
            return partList;
        }catch (Exception e){
            log.error("partSummaryList error, uploadId:｛｝，fileKey:｛｝",uploadId,fileKey);
            throw new RuntimeException("mergePart error",e);
        }
    }

    /**
     * 获取bucketName
     * @return
     */
    private String getBucketName(){
        FileProperties fileProperties = SpringUtil.getBean(FileProperties.class);
        String bucketName = fileProperties.getS3().getBucketName();
        return bucketName;
    }
}
