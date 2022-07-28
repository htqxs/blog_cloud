package fit.xiaozhang.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author zhangzhi
 * @create 2022/7/28 10:57
 */
@EnableDiscoveryClient
@SpringBootApplication
public class BlogGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogGatewayApplication.class, args);
    }
}
