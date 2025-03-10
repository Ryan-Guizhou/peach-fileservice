package com.peach.fileservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/3/10 15:41
 */
@Data
@Builder
public class DiskDO implements Serializable {

    private static final long serialVersionUID = 769835859489176882L;
    /**
     * 磁盘名称
     */
    private String name;
    /**
     *  磁盘路径
     */
    private String path;
    /**
     *   磁盘类型
     */
    private String type;
    /**
     *  磁盘总大小(GB)
     */
    private double totalSpace;
    /**
     *    可用空间
     *    该方法返回文件系统上当前可供使用的空间大小
     *    返回的是实际可写入文件的剩余空间大小
     *
     */
    private double usableSpace;
    /**
     *    未分配空间 (GB)
     *    该方法返回文件系统上未分配给任何文件或目录的空闲空间大小
     *    它表示文件系统中尚未被使用的、可供分配的空间大小
     */
    private double unallocatedSpace;

    @Override
    public String toString() {
        return "{" +
                ", 查询路径 ='" + path + '\'' +
                ", 磁盘类型 ='" + type + '\'' +
                ", 磁盘总大小 =" + totalSpace + "GB" +
                ", 可用空间 =" + usableSpace + "GB" +
                ", 未分配空间 =" + unallocatedSpace + "GB" +
                "}";
    }

}
