package com.peach.fileservice.impl;

import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.file.LocalStorageImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/3/10 11:45
 */
public class Test {

    public static void main(String[] args) throws FileNotFoundException {
        LocalStorageImpl localStorage = new LocalStorageImpl(new FileProperties());
        File file = new File("C:\\Users\\pc\\Desktop\\a.py");
        String url = localStorage.upload(file,
                "D:\\Mine\\peach-fileservice\\peach-fileservice-service\\src\\main\\java\\com\\peach\\fileservice\\impl\\","测试.py");
        System.out.println(url);
        InputStream inputStream = new FileInputStream(file);
        localStorage.upload(inputStream, "D:\\Mine\\peach-fileservice\\peach-fileservice-service\\src\\main\\java\\com\\peach\\fileservice\\impl\\",
                "测试1.py");
        url = "D:/Mine/peach-fileservice/peach-fileservice-service/src/main/java/com/peach/fileservice/impl//%E6%B5%8B%E8%AF%95.py?timestamp=1741585152464";

        localStorage.delete(url);
    }
}
