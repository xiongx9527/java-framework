package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.model.SaTokenInfo;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

public class SaTokenMagicDynamicRegistry extends AbstractMagicDynamicRegistry<SaTokenInfo> {

    public SaTokenMagicDynamicRegistry(MagicResourceStorage<SaTokenInfo> magicResourceStorage) {
        super(magicResourceStorage);
    }

}
