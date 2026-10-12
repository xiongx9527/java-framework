package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.MagicAPIDynamicDataSourceAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxConditionalConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration;

/**
 * MagicAPI 模块自动配置聚合入口。
 *
 * <p>环境无关的统一入口，通过 {@link Import} 聚合 SERVLET 模式（{@link MagicAPIWebMvcAutoConfiguration}）、
 * REACTIVE 模式（{@link MagicAPIWebFluxAutoConfiguration}）以及动态数据源支持
 * （{@link MagicAPIDynamicDataSourceAutoConfiguration}）三个子配置，
 * 使 {@code spring.factories} 中 MagicAPI 模块从 3 行收敛为 1 行。</p>
 *
 * <p>子配置各自带 {@code @ConditionalOnWebApplication(type = SERVLET / REACTIVE)} 条件，
 * 按应用类型自动激活对应的子配置，互不干扰。</p>
 *
 * <p><b>排序必须声明在本类（而非 {@code @Import} 的子配置）上</b>：
 * {@code AutoConfigurationSorter} 只对 spring.factories 里的自动配置类排序，
 * 子配置是普通配置类，不参与排序。</p>
 *
 * <p><b>为什么必须 {@code @AutoConfigureBefore(MagicAPIAutoConfiguration.class)}</b>：
 * 本模块的 {@code MagicAPIWebMvcAutoConfiguration} 与上游 starter 各自用
 * {@code @ConditionalOnMissingBean} 声明了<b>同名</b> bean {@code magicResourceService}
 * （分别是 {@code MyDefaultMagicResourceService} 与 {@code DefaultMagicResourceService}）。
 * 该条件只看"此刻已注册的 bean 定义"，后注册者必然退让，胜者完全由排序结果决定；
 * 而排序结果并不稳定——候选列表初始顺序来自各 jar 在 classpath 上的先后，
 * 实测同一对类在本模块测试 classpath 与示例应用 classpath 上会得到相反结果。
 * 一旦上游先注册，本模块就静默失去 {@code MyDefaultMagicResourceService} 的写锁保护，
 * 并让 {@code MagicAPIController} 的硬转在请求期抛 ClassCastException。
 * 详见 {@code MagicAPIAutoConfigurationOrderTest}。</p>
 *
 * <p><b>为什么还要 {@code @AutoConfigureAfter(WsAutoConfiguration.class)}</b>：
 * WS 插件配置 {@code MagicAPIWsConfiguration} 是经本类 {@code @Import} 链路挂进来的
 * （{@code @Import} 在解析期深度优先立即展开），而 WS 地基 {@code WsAutoConfiguration}
 * 在 spring.factories 中排在本类之后。{@code MagicAPIWsConfiguration} 用
 * {@code @ConditionalOnBean(AbstractWebSocketHandler.class)} 决定是否装配，
 * 该条件只能看到"此刻已解析完的配置"里声明过的 {@code @Bean}，所以地基必须先解析。
 * 顺序反了 WS 插件会被静默跳过——实测把 WsAutoConfiguration 作为延迟自动配置、
 * MagicAPIWsConfiguration 作为用户配置注册时，{@code wsModule} bean 数为 0。
 * 两条注解合起来固定了整条链：{@code WsAutoConfiguration} → 本类 → 上游 starter。</p>
 *
 * <p>另注：两条注解都必须写<b>类引用</b>（{@code value}）而不是 {@code name = "..."}。
 * {@code AutoConfigurationSorter} 的 {@code getCandidates} 只读取 {@code value} 属性，
 * String 形式的 {@code name} 会被静默忽略，该注解因此完全不产生排序约束。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(WsAutoConfiguration.class)
@AutoConfigureBefore(MagicAPIAutoConfiguration.class)
@Import({
		MagicAPIWebMvcAutoConfiguration.class,
		MagicAPIWebFluxConditionalConfiguration.class,
		MagicAPIDynamicDataSourceAutoConfiguration.class
})
public class MagicAPIBaseAutoConfiguration {
}
