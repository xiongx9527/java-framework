package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.UserMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.User;
import com.xiongx9527.fw.plus.tinyengine.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

	@Override
	public List<User> queryAllUser() {
		return baseMapper.queryAllUser();
	}

	@Override
	public User queryUserById(String id) {
		return baseMapper.queryUserById(id);
	}

	@Override
	public List<User> queryUserByCondition(User user) {
		return baseMapper.queryUserByCondition(user);
	}

	@Override
	public Integer deleteUserById(String id) {
		return baseMapper.deleteUserById(id);
	}

	@Override
	public Integer updateUserById(User user) {
		return baseMapper.updateUserById(user);
	}
}
