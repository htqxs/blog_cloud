package fit.xiaozhang.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fit.xiaozhang.blog.constant.CommonConst;
import fit.xiaozhang.blog.dao.CommentDao;
import fit.xiaozhang.blog.dao.UserInfoDao;
import fit.xiaozhang.blog.dto.*;
import fit.xiaozhang.blog.entity.Comment;
import fit.xiaozhang.blog.service.CommentService;
import fit.xiaozhang.blog.util.EmailDTO;
import fit.xiaozhang.blog.util.HTMLUtil;
import fit.xiaozhang.blog.util.UserUtil;
import fit.xiaozhang.blog.vo.CommentVO;
import fit.xiaozhang.blog.vo.ConditionVO;
import fit.xiaozhang.blog.vo.DeleteVO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static fit.xiaozhang.blog.constant.CommonConst.*;
import static fit.xiaozhang.blog.constant.MQPrefixConst.EMAIL_EXCHANGE;
import static fit.xiaozhang.blog.constant.RedisPrefixConst.COMMENT_LIKE_COUNT;
import static fit.xiaozhang.blog.constant.RedisPrefixConst.COMMENT_USER_LIKE;

/**
 * @author zhangzhi
 * @since 2020-05-18
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentDao, Comment> implements CommentService {
    @Autowired
    private CommentDao commentDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageDTO<CommentDTO> listComments(Integer articleId, Long current) {
        // ?????????????????????
        Integer commentCount = commentDao.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Objects.nonNull(articleId), Comment::getArticleId, articleId)
                .isNull(Objects.isNull(articleId), Comment::getArticleId)
                .isNull(Comment::getParentId)
                .eq(Comment::getIsDelete, CommonConst.FALSE));
        if (commentCount == 0) {
            return new PageDTO<>();
        }
        // ????????????????????????
        List<CommentDTO> commentDTOList = commentDao.listComments(articleId, (current - 1) * 10);
        // ??????redis?????????????????????
        Map<String, Integer> likeCountMap = (Map<String, Integer>) redisTemplate.boundHashOps(COMMENT_LIKE_COUNT).entries();
        // ????????????id??????
        List<Integer> commentIdList = new ArrayList<>();
        // ?????????????????????
        commentDTOList.forEach(item -> {
            commentIdList.add(item.getId());
            item.setLikeCount(Objects.requireNonNull(likeCountMap).get(item.getId().toString()));
        });
        // ????????????id????????????????????????
        List<ReplyDTO> replyDTOList = commentDao.listReplies(commentIdList);
        // ?????????????????????
        replyDTOList.forEach(item -> item.setLikeCount(Objects.requireNonNull(likeCountMap).get(item.getId().toString())));
        // ????????????id??????????????????
        Map<Integer, List<ReplyDTO>> replyMap = replyDTOList.stream().collect(Collectors.groupingBy(ReplyDTO::getParentId));
        // ????????????id???????????????
        Map<Integer, Integer> replyCountMap = commentDao.listReplyCountByCommentId(commentIdList)
                .stream().collect(Collectors.toMap(ReplyCountDTO::getCommentId, ReplyCountDTO::getReplyCount));
        // ?????????????????????????????????????????????????????????
        commentDTOList.forEach(item -> {
            item.setReplyDTOList(replyMap.get(item.getId()));
            item.setReplyCount(replyCountMap.get(item.getId()));
        });
        return new PageDTO<>(commentDTOList, commentCount);
    }

    @Override
    public List<ReplyDTO> listRepliesByCommentId(Integer commentId, Long current) {
        // ????????????????????????????????????
        List<ReplyDTO> replyDTOList = commentDao.listRepliesByCommentId(commentId, (current - 1) * 5);
        // ??????redis?????????????????????
        Map<String, Integer> likeCountMap = (Map<String, Integer>) redisTemplate.boundHashOps(COMMENT_LIKE_COUNT).entries();
        // ??????????????????
        replyDTOList.forEach(item -> item.setLikeCount(Objects.requireNonNull(likeCountMap).get(item.getId().toString())));
        return replyDTOList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveComment(CommentVO commentVO) {
        // ??????html??????
        commentVO.setCommentContent(HTMLUtil.deleteCommentTag(commentVO.getCommentContent()));
        Comment comment = Comment.builder()
                .userId(UserUtil.getLoginUser().getUserInfoId())
                .replyId(commentVO.getReplyId())
                .articleId(commentVO.getArticleId())
                .commentContent(commentVO.getCommentContent())
                .parentId(commentVO.getParentId())
                .createTime(new Date())
                .build();
        commentDao.insert(comment);
        // ????????????
        notice(commentVO);
    }

    /**
     * ??????????????????
     *
     * @param commentVO ????????????
     */
    @Async
    public void notice(CommentVO commentVO) {
        // ???????????????????????????????????????
        Integer userId = Objects.nonNull(commentVO.getReplyId()) ? commentVO.getReplyId() : BLOGGER_ID;
        // ???????????????
        String email = userInfoDao.selectById(userId).getEmail();
        if (StringUtils.isNotBlank(email)) {
            // ??????????????????
            String url = Objects.nonNull(commentVO.getArticleId()) ? URL + ARTICLE_PATH + commentVO.getArticleId() : URL + LINK_PATH;
            // ????????????
            EmailDTO emailDTO = EmailDTO.builder()
                    .email(email)
                    .subject("????????????")
                    .content("??????????????????????????????????????????" + url + "\n????????????")
                    .build();
            rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, "*", new Message(JSON.toJSONBytes(emailDTO), new MessageProperties()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveCommentLike(Integer commentId) {
        // ????????????????????????????????????id??????
        HashSet<Integer> commentLikeSet = (HashSet<Integer>) redisTemplate.boundHashOps(COMMENT_USER_LIKE).get(UserUtil.getLoginUser().getUserInfoId().toString());
        // ????????????????????????
        if (CollectionUtils.isEmpty(commentLikeSet)) {
            commentLikeSet = new HashSet<>();
        }
        // ??????????????????
        if (commentLikeSet.contains(commentId)) {
            // ????????????????????????id
            commentLikeSet.remove(commentId);
            // ???????????????-1
            redisTemplate.boundHashOps(COMMENT_LIKE_COUNT).increment(commentId.toString(), -1);
        } else {
            // ????????????????????????id
            commentLikeSet.add(commentId);
            // ???????????????+1
            redisTemplate.boundHashOps(COMMENT_LIKE_COUNT).increment(commentId.toString(), 1);
        }
        // ??????????????????
        redisTemplate.boundHashOps(COMMENT_USER_LIKE).put(UserUtil.getLoginUser().getUserInfoId().toString(), commentLikeSet);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCommentDelete(DeleteVO deleteVO) {
        // ??????????????????????????????
        List<Comment> commentList = deleteVO.getIdList().stream()
                .map(id -> Comment.builder().id(id).isDelete(deleteVO.getIsDelete()).build())
                .collect(Collectors.toList());
        this.updateBatchById(commentList);
    }

    @Override
    public PageDTO<CommentBackDTO> listCommentBackDTO(ConditionVO condition) {
        // ????????????
        condition.setCurrent((condition.getCurrent() - 1) * condition.getSize());
        // ?????????????????????
        Integer count = commentDao.countCommentDTO(condition);
        if (count == 0) {
            return new PageDTO<>();
        }
        // ????????????????????????
        List<CommentBackDTO> commentBackDTOList = commentDao.listCommentBackDTO(condition);
        // ?????????????????????
        Map<String, Integer> likeCountMap = redisTemplate.boundHashOps(COMMENT_LIKE_COUNT).entries();
        //???????????????
        commentBackDTOList.forEach(item -> item.setLikeCount(Objects.requireNonNull(likeCountMap).get(item.getId().toString())));
        return new PageDTO<>(commentBackDTOList, count);
    }

}
