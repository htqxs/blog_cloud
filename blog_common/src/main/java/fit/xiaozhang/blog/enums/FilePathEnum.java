package fit.xiaozhang.blog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhangzhi
 **/
@Getter
@AllArgsConstructor
public enum FilePathEnum {
    /**
     * 头像路径
     */
    AVATAR("avatar/", "头像路径"),
    /**
     * 文章图片路径
     */
    ARTICLE("articles/", "文章图片路径");

    /**
     * 路径
     */
    private final String path;

    /**
     * 描述
     */
    private final String desc;

}
