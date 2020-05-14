package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("admin/product/")
@Api(tags = "品牌管理")
public class FileUploadController {
    @Value("${fileServer.url}")
    private String fileUrl;

    @ApiOperation(value = "上传品牌图片")
    @RequestMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws Exception {
        String path = null;
        //获取resource数据源中的tracker
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        if (configFile != null) {
            //初始化
            ClientGlobal.init(configFile);
            //创建trackerClient
            TrackerClient trackerClient = new TrackerClient();
            //获取trackerService
            TrackerServer trackerServer = trackerClient.getConnection();
            //创建storageClient1
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            //上传文件
            //第一个参数表示要上传的字节数组
            //第二个参数，文件的后缀名
            //第三个参数，数组，null
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);
            System.out.println(fileUrl + path);
        }

        return Result.ok(fileUrl + path);
    }


}
