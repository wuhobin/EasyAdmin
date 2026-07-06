package com.aurora.service;

import com.aurora.starter.oss.model.OssUploadResult;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * <p>对 {@link com.aurora.starter.oss.template.OssTemplate} 的薄封装，
 * 提供管理员后台常用的文件上传、删除、查询等操作。</p>
 */
public interface FileService {

    /**
     * 上传文件，按当前日期自动生成存储路径（yyyy/MM/dd）
     *
     * @param file 上传的文件
     * @return 上传结果（含访问 URL）
     */
    OssUploadResult upload(MultipartFile file);

    /**
     * 上传文件到指定路径
     *
     * @param file 上传的文件
     * @param path 存储路径前缀（相对 OSS 根目录，例如 {@code avatar/}）
     * @return 上传结果（含访问 URL）
     */
    OssUploadResult upload(MultipartFile file, String path);

    /**
     * 删除文件
     *
     * @param url 文件访问 URL
     * @return 是否删除成功
     */
    boolean delete(String url);

    /**
     * 判断文件是否存在
     *
     * @param url 文件访问 URL
     * @return 是否存在
     */
    boolean exists(String url);

    /**
     * 获取文件信息
     *
     * @param url 文件访问 URL
     * @return 文件信息
     */
    FileInfo getFileInfo(String url);
}