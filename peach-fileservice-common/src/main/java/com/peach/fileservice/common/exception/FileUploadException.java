package com.peach.fileservice.common.exception;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 文件上传异常处理 / Handling file upload exceptions
 * @CreateTime 2025/6/20 16:21
 */
public class FileUploadException extends RuntimeException{

    private static final long serialVersionUID = 9087757948069493484L;

    public FileUploadException() {
        super();
    }

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileUploadException(Throwable cause) {
        super(cause);
    }
}
