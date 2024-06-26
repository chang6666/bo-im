package com.james.platform.service.impl;

import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.james.common.constant.RedisKey;
import com.james.platform.exception.BizException;
import com.james.platform.mapper.FriendMapper;
import com.james.platform.mapper.UserMapper;
import com.james.platform.pojo.entity.Friend;
import com.james.platform.pojo.entity.User;
import com.james.platform.pojo.vo.FriendVO;
import com.james.platform.service.FriendService;
import com.james.platform.session.SessionContext;
import com.james.platform.session.UserSession;

import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author james
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = RedisKey.IM_CACHE_FRIEND)
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    private final UserMapper userMapper;

    @Override
    public List<Friend> findFriendByUserId(Long userId) {
        LambdaQueryWrapper<Friend> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Friend::getUserId, userId);
        return this.list(queryWrapper);
    }

     @Transactional(rollbackFor = Exception.class)
    @Override
     public void addFriend(@NotEmpty(message = "好友id不能为空") Long friendId) {
         long userId = SessionContext.getSession().getUserId();
         if (userId == friendId) {
             throw new BizException("不允许添加自己为好友");
         }
         // 互相绑定好友关系
         FriendServiceImpl proxy = (FriendServiceImpl) AopContext.currentProxy();
         proxy.bindFriend(userId, friendId);
         proxy.bindFriend(friendId, userId);
         log.info("添加好友，用户id:{},好友id:{}", userId, friendId);
     }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delFriend(Long friendId) {
        long userId = SessionContext.getSession().getUserId();
        // 互相解除好友关系，走代理清理缓存
        FriendServiceImpl proxy = (FriendServiceImpl) AopContext.currentProxy();
        proxy.unbindFriend(userId, friendId);
        proxy.unbindFriend(friendId, userId);
        log.info("删除好友，用户id:{},好友id:{}", userId, friendId);
    }


    @Cacheable(key = "#userId1+':'+#userId2")
    @Override
    public Boolean isFriend(Long userId1, Long userId2) {
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Friend::getUserId, userId1)
                .eq(Friend::getFriendId, userId2);
        return this.count(queryWrapper) > 0;
    }


    @Override
    public void update(FriendVO vo) {
        long userId = SessionContext.getSession().getUserId();
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, vo.getId());

        Friend f = this.getOne(queryWrapper);
        if (f == null) {
            throw new BizException( "对方不是您的好友");
        }

        f.setFriendHeadImage(vo.getHeadImage());
        f.setFriendNickName(vo.getNickName());
        this.updateById(f);
    }

    @Override
    public FriendVO findFriend(Long friendId) {
        UserSession session = SessionContext.getSession();
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(Friend::getUserId, session.getUserId())
                .eq(Friend::getFriendId, friendId);
        Friend friend = this.getOne(wrapper);
        if (friend == null) {
            throw new BizException("对方不是您的好友");
        }
        FriendVO vo = new FriendVO();
        vo.setId(friend.getFriendId());
        vo.setHeadImage(friend.getFriendHeadImage());
        vo.setNickName(friend.getFriendNickName());
        return vo;
    }




    

      /**
     * 单向绑定好友关系
     *
     * @param userId   用户id
     * @param friendId 好友的用户id
     */
    @CacheEvict(key = "#userId+':'+#friendId")
    public void bindFriend(Long userId, Long friendId) {
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        if (this.count(queryWrapper) == 0) {
            Friend friend = new Friend();
            friend.setUserId(userId);
            friend.setFriendId(friendId);
            User friendInfo = userMapper.selectById(friendId);
            friend.setFriendHeadImage(friendInfo.getHeadImage());
            friend.setFriendNickName(friendInfo.getNickName());
            this.save(friend);
        }
    }


    /**
     * 单向解除好友关系
     *
     * @param userId   用户id
     * @param friendId 好友的用户id
     */
    @CacheEvict(key = "#userId+':'+#friendId")
    public void unbindFriend(Long userId, Long friendId) {
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        List<Friend> friends = this.list(queryWrapper);
        friends.forEach(friend -> this.removeById(friend.getId()));
    }


}
