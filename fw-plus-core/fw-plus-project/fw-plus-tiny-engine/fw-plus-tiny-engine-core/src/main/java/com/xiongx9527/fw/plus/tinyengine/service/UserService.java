package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

	List<User> queryAllUser();
}
