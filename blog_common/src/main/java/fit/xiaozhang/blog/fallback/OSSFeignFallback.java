package fit.xiaozhang.blog.fallback;

import fit.xiaozhang.blog.constant.StatusConst;
import fit.xiaozhang.blog.feign.OSSFeignClient;
import fit.xiaozhang.blog.util.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 服务降级处理类
 * @author zhangzhi
 * @create 2022/7/28 14:15
 */
@Component
public class OSSFeignFallback implements OSSFeignClient {

    @Override
    public Result<String> uploadImage(MultipartFile file, String path) {
        return new Result<>(false, StatusConst.ERROR, "图片上传失败!");
    }
}
