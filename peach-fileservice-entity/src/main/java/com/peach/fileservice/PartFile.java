package com.peach.fileservice;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 22:29
 */
@Data
public class PartFile implements Serializable {

    private static final long serialVersionUID = -2894812448651527459L;
    private byte[] content;
    private String contentType;
    private Long size;
    private String originalFilename;
    private String fId;

    public static PartFile from(MultipartFile mFile) throws IOException {
        PartFile f = new PartFile();
        f.content = mFile.getBytes();
        f.contentType = mFile.getContentType();
        f.size = mFile.getSize();
        f.originalFilename = mFile.getOriginalFilename();
        return f;
    }
}
