package fit.xiaozhang.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fit.xiaozhang.blog.dao.ArticleDao;
import fit.xiaozhang.blog.dao.CategoryDao;
import fit.xiaozhang.blog.dto.CategoryDTO;
import fit.xiaozhang.blog.dto.PageDTO;
import fit.xiaozhang.blog.entity.Article;
import fit.xiaozhang.blog.entity.Category;
import fit.xiaozhang.blog.exception.ServeException;
import fit.xiaozhang.blog.service.CategoryService;
import fit.xiaozhang.blog.vo.CategoryVO;
import fit.xiaozhang.blog.vo.ConditionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;


/**
 * @author zhangzhi
 * @since 2020-05-18
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, Category> implements CategoryService {
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private ArticleDao articleDao;

    @Override
    public PageDTO<CategoryDTO> listCategories() {
        return new PageDTO<>(categoryDao.listCategoryDTO(), categoryDao.selectCount(null));
    }

    @Override
    public PageDTO<Category> listCategoryBackDTO(ConditionVO condition) {
        // 分页查询分类列表
        Page<Category> page = new Page<>(condition.getCurrent(), condition.getSize());
        Page<Category> categoryPage = categoryDao.selectPage(page, new LambdaQueryWrapper<Category>()
                .select(Category::getId, Category::getCategoryName, Category::getCreateTime)
                .like(StringUtils.isNotBlank(condition.getKeywords()), Category::getCategoryName, condition.getKeywords())
                .orderByDesc(Category::getId));
        return new PageDTO<>(categoryPage.getRecords(), (int) categoryPage.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteCategory(List<Integer> categoryIdList) {
        // 查询分类id下是否有文章
        Integer count = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                .in(Article::getCategoryId, categoryIdList));
        if (count > 0) {
            throw new ServeException("删除失败，该分类下存在文章");
        }
        categoryDao.deleteBatchIds(categoryIdList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateCategory(CategoryVO categoryVO) {
        // 判断分类名重复
        Integer count = categoryDao.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getCategoryName, categoryVO.getCategoryName()));
        if (count > 0) {
            throw new ServeException("分类名已存在");
        }
        Category category = Category.builder()
                .id(categoryVO.getId())
                .categoryName(categoryVO.getCategoryName())
                .createTime(Objects.isNull(categoryVO.getId()) ? new Date() : null)
                .build();
        saveOrUpdate(category);
    }

}
