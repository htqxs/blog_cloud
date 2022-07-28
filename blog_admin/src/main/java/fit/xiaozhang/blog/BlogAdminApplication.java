package fit.xiaozhang.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author zhangzhi
 * @create 2022/7/27 17:06
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients("fit.xiaozhang.blog.feign")
public class BlogAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogAdminApplication.class, args);
    }
}
