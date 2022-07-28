package fit.xiaozhang.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author zhangzhi
 * @create 2022/7/28 9:56
 */
@SpringBootApplication
@EnableDiscoveryClient
public class BlogOSSApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogOSSApplication.class, args);
    }
}
