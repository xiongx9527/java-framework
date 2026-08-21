package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller;

import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

public class ServletMagicAPIRequestAccess implements MagicAPIRequestAccess {

    private final MagicHttpServletRequest request;

    public ServletMagicAPIRequestAccess(MagicHttpServletRequest request) {
        this.request = request;
    }

    @Override
    public MagicUser getMagicUser() {
        return (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
    }

    @Override
    public MagicHttpServletRequest asServletRequest() {
        return request;
    }
}
