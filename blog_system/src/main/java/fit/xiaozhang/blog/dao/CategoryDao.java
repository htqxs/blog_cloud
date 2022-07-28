package fit.xiaozhang.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fit.xiaozhang.blog.dto.CategoryDTO;
import fit.xiaozhang.blog.entity.Category;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * @author zhangzhi
 */
public interface CategoryDao extends BaseMapper<Category> {

    /**
     * 查询分类和对应文章数量
     * @return 分类集合
     */
    List<CategoryDTO> listCategoryDTO();

}
