package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-21 16:00
 **/
@Service
@Log4j2
public class UploadService {

    @Autowired
    private FastFileStorageClient storageClient;

    private static final List<String> ALLOW_CONTENT_TYPE = Arrays.asList("image/png", "image/jpeg");

    public String uploadImage(MultipartFile file) {
        try {
            // 文件校验

            // 1、类型校验
            String contentType = file.getContentType();
            if(!ALLOW_CONTENT_TYPE.contains(contentType)){
                // 文件类型不支持
                return null;
            }

            // 2、内容校验
            BufferedImage image = ImageIO.read(file.getInputStream());
            if(image == null){
                return null;
            }


            // 目标保存路径
//            File dest = new File("D:\\heima31\\nginx-1.12.2\\html\\" + file.getOriginalFilename());
//            file.transferTo(dest);

            // 上传到FastDFS
            String extName = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath path = storageClient.uploadFile(file.getInputStream(), file.getSize(), extName, null);

            // 返回url地址
            return "http://image.leyou.com/" + path.getFullPath();
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException(e);
        }
    }
}
