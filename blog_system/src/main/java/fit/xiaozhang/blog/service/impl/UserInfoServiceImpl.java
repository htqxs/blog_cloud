package fit.xiaozhang.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fit.xiaozhang.blog.dao.UserInfoDao;
import fit.xiaozhang.blog.dto.PageDTO;
import fit.xiaozhang.blog.dto.UserInfoDTO;
import fit.xiaozhang.blog.dto.UserOnlineDTO;
import fit.xiaozhang.blog.entity.UserInfo;
import fit.xiaozhang.blog.entity.UserRole;
import fit.xiaozhang.blog.enums.FilePathEnum;
import fit.xiaozhang.blog.exception.ServeException;
import fit.xiaozhang.blog.feign.OSSFeignClient;
import fit.xiaozhang.blog.service.UserInfoService;
import fit.xiaozhang.blog.service.UserRoleService;
import fit.xiaozhang.blog.util.UserUtil;
import fit.xiaozhang.blog.vo.ConditionVO;
import fit.xiaozhang.blog.vo.EmailVO;
import fit.xiaozhang.blog.vo.UserInfoVO;
import fit.xiaozhang.blog.vo.UserRoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static fit.xiaozhang.blog.constant.RedisPrefixConst.CODE_KEY;


/**
 * @author zhangzhi
 * @since 2020-05-18
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoDao, UserInfo> implements UserInfoService {
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OSSFeignClient ossFeignClient;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserInfo(UserInfoVO userInfoVO) {
        // 封装用户信息
        UserInfo userInfo = UserInfo.builder()
                .id(UserUtil.getLoginUser().getUserInfoId())
                .nickname(userInfoVO.getNickname())
                .intro(userInfoVO.getIntro())
                .webSite(userInfoVO.getWebSite())
                .updateTime(new Date())
                .build();
        userInfoDao.updateById(userInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String updateUserAvatar(MultipartFile file) {
        // 头像上传oss，返回图片地址
        String avatar = ossFeignClient.uploadImage(file, FilePathEnum.AVATAR.getPath()).getData();
        // 更新用户信息
        UserInfo userInfo = UserInfo.builder()
                .id(UserUtil.getLoginUser().getUserInfoId())
                .avatar(avatar)
                .build();
        userInfoDao.updateById(userInfo);
        return avatar;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveUserEmail(EmailVO emailVO) {
        if (!emailVO.getCode().equals(redisTemplate.boundValueOps(CODE_KEY + emailVO.getEmail()).get())) {
            throw new ServeException("验证码错误！");
        }
        UserInfo userInfo = UserInfo.builder()
                .id(UserUtil.getLoginUser().getUserInfoId())
                .email(emailVO.getEmail())
                .build();
        userInfoDao.updateById(userInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserRole(UserRoleVO userRoleVO) {
        // 更新用户角色和昵称
        UserInfo userInfo = UserInfo.builder()
                .id(userRoleVO.getUserInfoId())
                .nickname(userRoleVO.getNickname())
                .build();
        userInfoDao.updateById(userInfo);
        // 删除用户角色重新添加
        userRoleService.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userRoleVO.getUserInfoId()));
        List<UserRole> userRoleList = userRoleVO.getRoleIdList().stream()
                .map(roleId -> UserRole.builder()
                        .roleId(roleId)
                        .userId(userRoleVO.getUserInfoId())
                        .build())
                .collect(Collectors.toList());
        userRoleService.saveBatch(userRoleList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserDisable(Integer userInfoId, Integer isDisable) {
        // 更新用户禁用状态
        UserInfo userInfo = UserInfo.builder()
                .id(userInfoId)
                .isDisable(isDisable)
                .build();
        userInfoDao.updateById(userInfo);
    }

    @Override
    public PageDTO<UserOnlineDTO> listOnlineUsers(ConditionVO conditionVO) {
        // 获取security在线session
        List<UserOnlineDTO> userOnlineDTOList = sessionRegistry.getAllPrincipals().stream()
                .filter(item -> sessionRegistry.getAllSessions(item, false).size() > 0)
                .map(item -> JSON.parseObject(JSON.toJSONString(item), UserOnlineDTO.class))
                .sorted(Comparator.comparing(UserOnlineDTO::getLastLoginTime).reversed())
                .collect(Collectors.toList());
        // 执行分页
        int current = (conditionVO.getCurrent() - 1) * conditionVO.getSize();
        int size = userOnlineDTOList.size() > conditionVO.getSize() ? current + conditionVO.getSize() : userOnlineDTOList.size();
        List<UserOnlineDTO> userOnlineList = userOnlineDTOList.subList((conditionVO.getCurrent() - 1) * conditionVO.getSize(), size);
        return new PageDTO<>(userOnlineList, userOnlineDTOList.size());
    }

    @Override
    public void removeOnlineUser(Integer userInfoId) {
        // todo 不是很理解，下线用户
        // 获取用户session
        List<Object> userInfoList = sessionRegistry.getAllPrincipals().stream().filter(item -> {
            UserInfoDTO userInfoDTO = (UserInfoDTO) item;
            return userInfoDTO.getUserInfoId().equals(userInfoId);
        }).collect(Collectors.toList());
        List<SessionInformation> allSessions = new ArrayList<>();
        userInfoList.forEach(item -> allSessions.addAll(sessionRegistry.getAllSessions(item, false)));
        // 注销session
        allSessions.forEach(SessionInformation::expireNow);
    }

}
