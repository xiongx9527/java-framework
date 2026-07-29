package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller;

import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

public interface MagicAPIRequestAccess {

    MagicUser getMagicUser();

    MagicHttpServletRequest asServletRequest();
}
