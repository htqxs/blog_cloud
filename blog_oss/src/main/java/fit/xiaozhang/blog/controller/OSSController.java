package fit.xiaozhang.blog.controller;

import fit.xiaozhang.blog.constant.StatusConst;
import fit.xiaozhang.blog.service.UploadService;
import fit.xiaozhang.blog.util.Result;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author zhangzhi
 * @create 2022/7/28 9:57
 */
@RestController
public class OSSController {

    @Autowired
    private UploadService uploadService;

    @ApiOperation(value = "上传图片")
    @ApiImplicitParam(name = "file", value = "文章图片", required = true, dataType = "MultipartFile")
    @PostMapping("/upload/image")
    public Result<String> uploadImage(MultipartFile file, String path) {
        return new Result<>(true, StatusConst.OK, "上传成功", uploadService.upload(file, path));
    }
}