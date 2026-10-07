package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSetting;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbSettingMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 知识库对外 API 认证 provider 的取值与热生效（A5）：留空回落默认、保存后下一次读取即生效。
 *
 * @author zzy
 */
class KbSettingServiceAuthProviderTest {

    @Test
    void 无记录时对外认证provider为sa_token_login() {
        KbSettingService service = new KbSettingService(mock(KbSettingMapper.class));

        assertThat(service.openApiAuthProvider())
                .isEqualTo(KbSettingService.DEFAULT_OPEN_API_AUTH_PROVIDER);
    }

    @Test
    void 留空归一化为默认provider() {
        KbSettingService service = new KbSettingService(mock(KbSettingMapper.class));

        KbSetting out = service.normalize(new KbSetting());

        assertThat(out.getOpenApiAuthProvider())
                .isEqualTo(KbSettingService.DEFAULT_OPEN_API_AUTH_PROVIDER);
    }

    @Test
    void 保存后热生效() {
        KbSettingMapper mapper = mock(KbSettingMapper.class);
        when(mapper.selectById(any())).thenReturn(null);
        KbSettingService service = new KbSettingService(mapper);

        KbSetting input = new KbSetting();
        input.setStorageType("local");
        input.setOpenApiAuthProvider("permit-all");
        service.save(input);

        assertThat(service.openApiAuthProvider()).isEqualTo("permit-all");
    }
}