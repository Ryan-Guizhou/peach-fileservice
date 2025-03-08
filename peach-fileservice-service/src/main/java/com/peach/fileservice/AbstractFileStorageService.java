package com.peach.fileservice;

import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 14:43
 */
@Slf4j
public abstract class AbstractFileStorageService{

    protected static final String PATH_SEPARATOR = "/";

    protected static final long EXPIRATION = 3600L * 1000 * 24 * 365 * 2;

    /**
     * 替换url 中的签名信息
     * before replace http://10.125.176.129/dataset/128/151/compressed/5/sjt-sub-14-6.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20240821T030150Z&X-Amz-SignedHeaders=host&X-Amz-Expires=2592000&X-Amz-Credential=F8AOFS0MTN0HXBYV35RD%2F20240821%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=41bb33e53aa055e1b82f5208f6f94f153399c3ef9bdb163571715ba53b9c9080
     * after  replace http://10.125.176.129/dataset/128/151/compressed/5/sjt-sub-14-6.png
     */
    protected static final int URL_TAKE_SIGN_NO = 0;
    protected static final int URL_TAKE_SIGN_YES = 1;

    /**
     * 复制文件夹以及文件夹下面所有内容
     *
     * @param sourceDir 源文件夹
     * @param targetDir 目标目录
     * @return boolean
     */
    public abstract boolean copyDir(String sourceDir, String targetDir);

    /**
     * 下载某个文件夹下所有的文件
     *
     * @param sourceDir 资源文件夹
     * @param localDir  本地文件夹
     * @return boolean
     */
    public abstract boolean downDir(String sourceDir, String localDir);

    /**
     * 文件上传接口
     *
     * @param inputStream 文件流
     * @param targetPath  目标上传路径
     * @param fileName    文件名称 带后缀
     * @return: java.lang.String 带签名存储路径
     * @author: pc
     */
    public abstract String upload(InputStream inputStream, String targetPath, String fileName);

    /**
     * 文件上传接口
     *
     * @param content    文件内容
     * @param targetPath 目标上传路径
     * @param fileName   文件名称 带后缀
     * @return: java.lang.String  带签名存储路径
     * @author: pc
     */
    public abstract String upload(String content, String targetPath, String fileName);

    /**
     * 文件上传接口
     *
     * @param file       文件，一个或者多个
     * @param targetPath 目标上传路径
     * @return: java.util.List  带签名存储路径
     * @author: pc
     */
    public abstract List<String> upload(File[] file, String targetPath);

    /**
     * 文件上传接口
     *
     * @param file       文件
     * @param targetPath 目标上传路径
     * @param fileName   文件名称 带后缀
     * @return: java.lang.String  带签名存储路径
     */
    public abstract String upload(File file, String targetPath, String fileName);

    /**
     * 根据URL下载文件到本地指定目录
     *
     * @param sourceUrl 资源地址
     * @param localPath 本地路径
     * @param fileName  件名称 带后缀
     * @return: void
     * @author: pc
     */
//    public static boolean download(URL sourceUrl, String localPath, String fileName) {
//        boolean flag = true;
//        NacosV3Config nacosV3Config = SpringContextHolder.getBean("nacosV3Config");
//        try {
//            String begin = DateUtil.now();
//            DateTime beginTime = DateUtil.parse(begin);
//            String filePath = localPath;
//            if (!filePath.endsWith(File.separator)) {
//                filePath = filePath + File.separator;
//            }
//            //对文件名称进行编码
////            String originalFileName = FilenameUtils.getName(sourceUrl.getPath());
////            String encodeFileName = URLEncoder.ALL.encode(originalFileName, StandardCharsets.UTF_8);
////            String url = sourceUrl.toString().replace(originalFileName,encodeFileName);
//            String url = sourceUrl.toString();
//            //开启内网访问时，替换为内网地址
//            if (GlobalConstant.USE_INTRANET.equals(nacosV3Config.getUseIntranet())) {
//                String intranetIp = nacosV3Config.getIntranetIpAddress();
//                if (url.contains("https://")) {
//                    url = url.replaceAll("https://[^/]+", intranetIp);
//                } else {
//                    url = url.replaceAll("http://[^/]+", intranetIp);
//                }
//            }
//            HttpRequest httpRequest = HttpUtil.createGet(url,true);
//            if (url.startsWith(GlobalConstant.PROTOCOL_HTTPS)
//                    && GlobalConstant.BYPASS_HTTPS_AUTHENTICATION == nacosV3Config.getHttps_authentication_pass()) {
//                // 创建不执行任何认证操作的TrustManager
//                TrustManager[] trustAllCerts = new TrustManager[]{
//                        new X509TrustManager() {
//                            public X509Certificate[] getAcceptedIssuers() {
//                                return null;
//                            }
//                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                            }
//                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                            }
//                        }
//                };
//                String secureSocketLayer=nacosV3Config.getHttps_secureSocketLayer();
//                if (StringUtils.isBlank(secureSocketLayer)){
//                    secureSocketLayer = GlobalConstant.SSL;
//                }
//                // TLS1.0与SSL3.0基本上没有太大的差别，可粗略理解为TLS是SSL的继承者，但它们使用的是相同的SSLContext
//                SSLContext ctx = SSLContext.getInstance(secureSocketLayer);
//                // 使用TrustManager来初始化该上下文，TrustManager只是被SSL的Socket所使用
//                ctx.init(null, trustAllCerts, new SecureRandom());
//                httpRequest.setSSLSocketFactory(ctx.getSocketFactory());
//                // 创建允许所有主机名的hostname verifier
//                httpRequest.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
//            }
//            String systemReferer = nacosV3Config.getSystemReferer();
//            if (StringUtils.isNotBlank(systemReferer)) {
//                httpRequest.header("referer", systemReferer);
//            }
//            HttpResponse response = httpRequest.timeout(-1).executeAsync();
//            if (response.getStatus() == HttpStatus.HTTP_OK) {
//                response.writeBody(FileUtil.touch(filePath + fileName), new StreamProgress() {
//                    @Override
//                    public void start() {
//                        //log.info("开始下载，时间为：" + begin);
//                    }
//
//                    @Override
//                    public void progress(long total, long progressSize) {
//                        //log.info("已下载：" + FileUtil.readableFileSize(progressSize));
//                    }
//
//                    @Override
//                    public void finish() {
//                        String end = DateUtil.now();
//                        DateTime endTime = DateUtil.parse(end);
//                        long between = DateUtil.between(beginTime, endTime, DateUnit.MS);
//                        log.info("下载完成，用时：" + DateUtil.formatBetween(between, BetweenFormatter.Level.SECOND));
//                    }
//                });
//            } else {
//                flag = false;
//                log.error("文件下载出错：" + response.getStatus() + ";" + response.body());
//            }
//        } catch (Exception e) {
//            flag = false;
//            log.error("下载异常，异常信息为：" + e.getMessage());
//        }
//        return flag;
//    }


    /**
     * 从 targetPath 下载文件到本地指定目录
     *
     * @param targetPath 要下载的目标资源路径,/data/file/test/6.jpg?Expires=1803807824&OSSAccessKeyId=LTAI5tQiXiDypb3HDKq2uGKi&Signature=qDHJTu3G441I6WvId6RKHOdhvEs%3D
     * @param localPath 本地的存储路径
     * @param fileName 下载之后的文件名
     * @return: void
     * @author: pc
     */
    public abstract boolean download(String targetPath, String localPath, String fileName);

    /**
     * 从url 中获取文件流
     *
     * @param url
     * @return: java.io.InputStream
     * @author: pc
     */
//    public static InputStream getInputStream(String url) throws NoSuchAlgorithmException, KeyManagementException {
//        NacosV3Config nacosV3Config = SpringContextHolder.getBean("nacosV3Config");
//        //开启内网访问时，替换为内网地址
//        if (GlobalConstant.USE_INTRANET.equals(nacosV3Config.getUseIntranet())) {
//            String intranetIp = nacosV3Config.getIntranetIpAddress();
//            if (url.contains("https://")) {
//                url = url.replaceAll("https://[^/]+", intranetIp);
//            } else if (url.contains("http://")) {
//                url = url.replaceAll("http://[^/]+", intranetIp);
//            } else {
//                url = "http://" + intranetIp + url;
//            }
//        }
//        HttpRequest httpRequest = HttpUtil.createGet(url,true);
//        if (url.startsWith(GlobalConstant.PROTOCOL_HTTPS)
//                && GlobalConstant.BYPASS_HTTPS_AUTHENTICATION == nacosV3Config.getHttps_authentication_pass()) {
//            // 创建不执行任何认证操作的TrustManager
//            TrustManager[] trustAllCerts = new TrustManager[]{
//                    new X509TrustManager() {
//                        public X509Certificate[] getAcceptedIssuers() {
//                            return null;
//                        }
//                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                        }
//                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                        }
//                    }
//            };
//            String secureSocketLayer=nacosV3Config.getHttps_secureSocketLayer();
//            if (StringUtils.isBlank(secureSocketLayer)){
//                secureSocketLayer = GlobalConstant.SSL;
//            }
//            // TLS1.0与SSL3.0基本上没有太大的差别，可粗略理解为TLS是SSL的继承者，但它们使用的是相同的SSLContext
//            SSLContext ctx = SSLContext.getInstance(secureSocketLayer);
//            // 使用TrustManager来初始化该上下文，TrustManager只是被SSL的Socket所使用
//            ctx.init(null, trustAllCerts, new SecureRandom());
//            httpRequest.setSSLSocketFactory(ctx.getSocketFactory());
//            // 创建允许所有主机名的hostname verifier
//            httpRequest.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
//        }
//        String systemReferer = nacosV3Config.getSystemReferer();
//        if (StringUtils.isNotBlank(systemReferer)) {
//            httpRequest.header("referer", systemReferer);
//        }
//        // 超时时间设置为
//        HttpResponse response = httpRequest.timeout(-1).executeAsync();
//        if (response.getStatus() == HttpStatus.HTTP_OK) {
//            return response.bodyStream();
//        } else {
//            throw new RuntimeException("获取文件流失败!" + response.getStatus() + ":" + response.body());
//        }
//    }

//    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException {
//        String url = "https://uat-pro.shujiajia.com/data/nfsdata/dataset/20192996/20195209/compressed/%E6%96%87%E6%9C%AC%E6%95%B0%E6%8D%AE/1.txt";
//        HttpRequest httpRequest = HttpUtil.createGet(url, true);
        // 创建不执行任何认证操作的TrustManager
        // TrustManager[] trustAllCerts = new TrustManager[]{
        //         new X509TrustManager() {
        //             public X509Certificate[] getAcceptedIssuers() {
        //                 return null;
        //             }
        //
        //             public void checkClientTrusted(X509Certificate[] certs, String authType) {
        //             }
        //
        //             public void checkServerTrusted(X509Certificate[] certs, String authType) {
        //             }
        //         }
        // };
        // // TLS1.0与SSL3.0基本上没有太大的差别，可粗略理解为TLS是SSL的继承者，但它们使用的是相同的SSLContext
        // SSLContext ctx = SSLContext.getInstance("SSL");
        // // 使用TrustManager来初始化该上下文，TrustManager只是被SSL的Socket所使用
        // ctx.init(null, trustAllCerts, new SecureRandom());
        // httpRequest.setSSLSocketFactory(ctx.getSocketFactory());
        // // 创建允许所有主机名的hostname verifier
        // httpRequest.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        // 超时时间设置为
//        HttpResponse response = httpRequest.timeout(-1).executeAsync();
//        InputStream ss = response.bodyStream();
//        System.out.println(ss.toString());

//    }
    /**
     * 获取本地磁盘文件流
     *
     * @param targetPath （样例：/data/nfsdata/或者 /data/nfsdata）
     * @param fileName   (样例：test.yml)
     * @return
     */
//    public static InputStream getLocalInputStream(String targetPath, String fileName) {
//        try {
//            //真实存储路径 前缀
//            targetPath = targetPath.endsWith(StorageUtil.PATH_SEPARATOR) ? targetPath : targetPath + StorageUtil.PATH_SEPARATOR;
//            String filePath = targetPath + fileName;
//            return Files.newInputStream(Paths.get(filePath));
//        } catch (Exception e) {
//            return null;
//        }
//    }

    /**
     * 获取本地磁盘文件流
     *
     * @param key (样例：/nacos/config/test.yml)
     * @return
     */
    public static InputStream getLocalInputStreamByKey(String key) {
        try {
            return Files.newInputStream(Paths.get(key));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过文件路径获取 文件流
     *
     * @param targetPath （样例：/data/nfsdata/ 或者 /data/nfsdata）
     * @param fileName   (样例：test.yml)
     * @return: java.io.InputStream
     * @author: pc
     */
    public abstract InputStream getInputStream(String targetPath, String fileName);


    /**
     * 通过文件路径获取 文件流(文件全路径)
     *
     * @param key (样例：/nacos/config/test.yml)
     * @return
     */
    public abstract InputStream getInputStreamByKey(String key);

    /**
     * 删除文件或者文件夹
     *
     * @param key 目标地址 (样例：/nacos/config/test.yml 或者 /nacos/config/ )
     * @return
     */
    public abstract boolean delete(String key);


    /**
     * 复制文件 （暂未实现）
     *
     * @param currentPath
     * @param targetPath
     * @return
     */
    public abstract boolean copyFile(String currentPath, String targetPath);


    /**
     * 通过文件路径+文件名 获取url（key样例：/data/nfsdata/15374511/abc/大图.zip）
     *
     * @param key
     * @return
     */
    public abstract String getUrlByKey(String key);

    /**
     * 通过文件路径+文件名 获取带签名路径（key样例：/data/nfsdata/15374511/abc/大图.zip）
     *
     * @param key
     * @return
     */
    public abstract String getPathByKey(String key);

    /**
     * 通过文件路径+文件名?密钥形式 获取url（参数path样例：/data/ossdata/dataset/2/2/compressed/Camera_XHS_16699769067191000g0081lmm5p0qd60305n7qehd5pfmul0jh9o0.jpg?Expires=2011225256&OSSAccessKeyId=LTAI5tDkMeGWxhPykUtdJJQB&Signature=ejXQA4H%2B7Rcn2GptFS3BZQYeALE%3D）
     * 兼容外部索引地址形式的
     *
     * @param path
     * @return nginx代理的http地址
     */
//    public static String getUrlByPath(String path) {
//        String url = null;
//        if (StringUtils.isNotBlank(path)) {
//            NacosV3Config nacosV3Config = SpringContextHolder.getBean("nacosV3Config");
//            String nginxProxy = nacosV3Config.getNginxProxy();
//            if (path.startsWith("http://") || path.startsWith("https://")) {
//                url = path;
//            } else {
//                if (!path.startsWith("/")) {
//                    path = "/" + path;
//                }
//                if (path.startsWith("///")) {
//                    path = path.replaceFirst("///", "/");
//                }
//                if (path.startsWith("//")) {
//                    path = path.replaceFirst("//", "/");
//                }
//                if (path.startsWith("/") && nginxProxy.endsWith("/")) {
//                    nginxProxy = nginxProxy.substring(0, nginxProxy.length() - 1);
//                }
//                url = nginxProxy + path;
//            }
//            if (StorageUtil.TYPE_S3.equals(nacosV3Config.getFileServerType())
//                    && nacosV3Config.getS3UrlLatestSign() == StorageUtil.S3_URL_LATEST_SIGN_YES) {
//                int expiredDay = nacosV3Config.getS3UrlSignExpired();
//                String bucketName = nacosV3Config.getS3BucketName();
//                Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * expiredDay);
//                AmazonS3 s3Client = S3StorageImpl.getS3Client();
//                path = path.replace(bucketName + "/", "");
//                if (path.startsWith("/")){
//                    path = path.replaceFirst("/", "");
//                }else if (path.startsWith("//")){
//                    path = path.replaceFirst("//", "");
//                }
//                String s3Url = s3Client.generatePresignedUrl(bucketName, path, expiration).toString();
//                if (s3Url.startsWith("https://")) {
//                    url = s3Url.replaceAll("https://[^/]+", nginxProxy);
//                }
//                if (s3Url.startsWith("http://")) {
//                    url = s3Url.replaceAll("http://[^/]+", nginxProxy);
//                }
//            }
//        }
//        return url;
//    }


    /**
     * 截取url中路径信息(文件路径+文件名)
     *
     * @param url
     * @return 文件路径+文件名?密钥形式
     */
//    public static String getPathWithSignatureByUrl(String url) {
//        String path = null;
//        if (StringUtils.isNotBlank(url)) {
//            NacosV3Config nacosV3Config = SpringContextHolder.getBean("nacosV3Config");
//            path = url.replaceFirst(nacosV3Config.getNginxProxy(), "");
//            path = "/" + StorageUtil.getOssKeyWithSignature(path);
//        }
//        return path;
//    }

    /**
     * 为文件设置公共读 针对oss、ceph 等分布式存储本
     *
     * @param path
     * @return
     */
    public abstract void setPublicReadAcl(String path);

    /**
     * 获取当前服务器磁盘空间 使用情况
     *
     * @param path 目标路径
     * @return DiskEntity 实体
     */
//    public static DiskEntity getCurrentDiskSpace(String path) {
//        try {
//            if (StringUtils.isBlank(path)) {
//                path = File.separator;
//            }
//            FileStore fileStore = Files.getFileStore(Paths.get(path));
//            long totalSpace = fileStore.getTotalSpace();
//            long usableSpace = fileStore.getUsableSpace();
//            long unallocatedSpace = fileStore.getUnallocatedSpace();
//            String type = fileStore.type();
//            double total = bytesToGB(totalSpace);
//            double usable = bytesToGB(usableSpace);
//            double unallocated = bytesToGB(unallocatedSpace);
//            return DiskEntity.builder().path(path).type(type).usableSpace(usable).unallocatedSpace(unallocated).totalSpace(total).build();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * bytes to GB
     *
     * @param bytes 输入字节
     * @return 转换后数据 GB
     */
    private static double bytesToGB(long bytes) {
        BigDecimal size = new BigDecimal(bytes);
        BigDecimal gb = size.divide(BigDecimal.valueOf(1024 * 1024 * 1024), 2, RoundingMode.HALF_UP);
        return gb.doubleValue();
    }

    /**
     * 获取oss存储路径 将格式转换为 a/b/c/
     * @param dirName
     * @return
     */
    public String getOssKey(String dirName) {
        boolean isFirst = true;
        StringBuilder result = new StringBuilder();
        List<String> dirPathList = new LinkedList<>();
        String[] str = dirName.split("\\?");
        String path = str[0];
        String[] dirSins = path.split("/");
        for (String dirsin : dirSins) {
            if (StringUtil.isBlank(dirsin)) {
                continue;
            }
            String[] childDirs = dirsin.split("\\\\");
            for (String childDir : childDirs) {
                if (StringUtil.isNotBlank(childDir)) {
                    dirPathList.add(childDir);
                }
            }
        }
        for (String dirPath : dirPathList) {
            if (isFirst) {
                result = new StringBuilder(dirPath);
                isFirst = false;
            } else {
                result.append("/").append(dirPath);
            }
        }
        return result.toString();
    }


    /**
     * 检查文件路径是否包含非法字符
     * @param key
     * @return
     */
    protected boolean isHasIllegalChar(String key) {
        if ("/".equals(key) || "//".equals(key) || "\\".equals(key) || "\\\\".equals(key)
                || ".".equals(key) || "..".equals(key) || "".equals(key)) {
            return true;
        }
        return false;
    }

    /**
     * 移除url中的host
     * @param url
     * @return
     */
    protected String removeUrlHost(String url) {
        if (StringUtils.isBlank(url)){
            log.error("this url is blank,can't be remove");
            return url;
        }
        if (url.contains("https://")) {
            return url.replaceAll("https://[^/]+", "");
        }
        if (url.contains("http://")) {
            return url.replaceAll("http://[^/]+", "");
        }
        return url;
    }

    protected String handlerKeyPrefix(String key) {
        String prefix;
        //判断是文件还是文件夹
        if (key.startsWith("/")) {
            key = key.replaceFirst("/", "");
        }
        if (key.contains(".") || key.contains("?")) {
            prefix = key;
        } else {
            if (key.endsWith("/")) {
                prefix = key;
            } else {
                prefix = key + "/";
            }
        }
        if (prefix.contains("?")) {
            prefix = prefix.split("\\?")[0];
        }
        try {
            prefix = URLDecoder.decode(prefix, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return prefix;
    }
}
