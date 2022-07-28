package fit.xiaozhang.blog.config;

import fit.xiaozhang.blog.constant.MQPrefixConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rabbitmq配置类
 *
 * @author zhangzhi
 */
@Configuration
public class RabbitConfig {

    // 创建队列
    @Bean
    public Queue emailQueue() {
        return new Queue(MQPrefixConst.EMAIL_QUEUE, true);
    }

    // 创建交换机
    @Bean
    public FanoutExchange emailExchange() {
        return new FanoutExchange(MQPrefixConst.EMAIL_EXCHANGE, true, false);
    }

    // 把队列和交换机绑定在一起
    @Bean
    public Binding bindingEmailDirect() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange());
    }
}
