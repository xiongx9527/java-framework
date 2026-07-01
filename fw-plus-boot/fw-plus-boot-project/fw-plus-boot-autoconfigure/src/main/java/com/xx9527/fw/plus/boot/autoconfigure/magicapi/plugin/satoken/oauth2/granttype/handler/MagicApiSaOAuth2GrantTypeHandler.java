package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.oauth2.granttype.handler;

import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.oauth2.data.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.granttype.handler.SaOAuth2GrantTypeHandlerInterface;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.model.SaTokenInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.service.SaTokenMagicResourceStorage;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;
import java.util.stream.Collectors;

public class MagicApiSaOAuth2GrantTypeHandler implements SaOAuth2GrantTypeHandlerInterface {

    private final SaTokenMagicResourceStorage magicResourceStorage;

    public MagicApiSaOAuth2GrantTypeHandler(SaTokenMagicResourceStorage magicResourceStorage) {
        this.magicResourceStorage = magicResourceStorage;
    }

    @Override
    public String getHandlerGrantType() {
        return "magicapi";
    }

    @Override
    public AccessTokenModel getAccessToken(SaRequest req, String clientId, List<String> scopes) {
        String grantBizType = req.getParamNotNull("grantBizType");
        List<SaTokenInfo> magicResources = magicResourceStorage.findAll().stream()
                .filter(e -> grantBizType.equals(e.getBizType())).collect(Collectors.toList());
        if (magicResources.isEmpty() || magicResources.size() != 1) {
            throw new RuntimeException("Invalid grantBizType");
        }
        SaTokenInfo magicResource = magicResources.get(0);
        MagicScriptContext magicScriptContext = new MagicScriptContext();
        return (AccessTokenModel) ScriptManager.executeScript(magicResource.getScript(), magicScriptContext);
    }

}
