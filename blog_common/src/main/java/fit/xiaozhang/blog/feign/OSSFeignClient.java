package fit.xiaozhang.blog.feign;

import fit.xiaozhang.blog.fallback.OSSFeignFallback;
import fit.xiaozhang.blog.util.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * blog-oss相关接口
 */
@FeignClient(name = "blog-oss", fallback = OSSFeignFallback.class)
public interface OSSFeignClient {

    @PostMapping("/upload/image")
    Result<String> uploadImage(MultipartFile file, @RequestParam("path") String path);
}