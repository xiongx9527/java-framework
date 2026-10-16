package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.core.annotation.AnnotationUtils;
import org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守住本模块 MagicAPI 入口自动配置与上游 {@code MagicAPIAutoConfiguration} 的先后顺序。
 *
 * <p><b>要守住什么</b>：上下游各自用 {@code @ConditionalOnMissingBean} 声明了
 * <em>同名</em> bean {@code magicResourceService}——上游给
 * {@code DefaultMagicResourceService}，本模块给 {@code MyDefaultMagicResourceService}。
 * {@code @ConditionalOnMissingBean} 判定时看到的是“此刻已注册的 bean 定义”，
 * 所以后注册的一方必然退让，<b>胜者完全由 {@code AutoConfigurationSorter} 的输出顺序决定</b>。
 * 一旦上游先注册，本模块就静默失去
 * {@code MyDefaultMagicResourceService} 的写锁保护，而
 * {@code MagicAPIController} 对 {@code MagicConfiguration.getMagicResourceService()}
 * 做 {@code MyDefaultMagicResourceService} 硬转，运行时抛 ClassCastException。
 *
 * <p><b>为什么这个不变量会失守</b>：{@code AutoConfigurationSorter} 的输出并不稳定，
 * 它会先用 {@code AutoConfigurationMetadata.getAllNames()} 给候选列表播种，
 * 再做 {@code @AutoConfigureAfter} 的稳定分区；而候选列表的初始顺序来自
 * {@code ClassLoader.getResources("META-INF/spring.factories")}，即各 jar 在 classpath 上的先后。
 * 实测同一对类在两个 classpath 上得到相反结果：
 * <ul>
 *   <li>本模块自身的测试 classpath：本模块在前 → 排序后本模块在前，bug 被完全掩盖；</li>
 *   <li>{@code fw-plus-ai-magicapi-sample} 的真实 classpath：上游
 *       {@code magic-api-spring-boot-starter} 在前 → 排序后上游在前，bug 触发。</li>
 * </ul>
 * 实测数据（AI 样例 classpath，候选列表与原始顺序完全相同，唯一变量是本注解）：
 * <pre>
 * 不加 @AutoConfigureBefore : fw=14 up=0 -&gt; 排序后 fw=7 up=6  （上游在前，触发 bug）
 * 加   @AutoConfigureBefore : fw=14 up=0 -&gt; 排序后 fw=6 up=7  （本模块在前，正常）
 * </pre>
 *
 * <p><b>为什么断言的是注解而不是排序结果</b>：正因为排序结果依赖 classpath 组成，
 * 本模块内的任何行为断言都测不出这个回归——实测把候选列表缩到这两个类时，
 * 排序恒按字母序把 {@code com.xiongx9527.*} 排在 {@code org.ssssssss.*} 之前，
 * 摘掉注解后测试依然全绿。唯一能在本模块内守住该回归的办法，
 * 就是守住那条真正决定胜负的声明。真正的端到端验证在
 * {@code fw-plus-ai-magicapi-sample} 上做：修复后该样例容器里的
 * {@code MagicResourceService} 必须是 {@code MyDefaultMagicResourceService}。
 *
 * <p>另注：注解必须声明在本类（spring.factories 中的自动配置入口）上，
 * {@code @Import} 进来的 {@code MagicAPIWebMvcAutoConfiguration} /
 * {@code MagicAPIWebFluxAutoConfiguration} 是普通配置类，排序器看不到它们。
 */
class MagicAPIAutoConfigurationOrderTest {

    @Test
    void mustDeclareAutoConfigureBeforeUpstreamMagicApiAutoConfiguration() {
        AutoConfigureBefore before = AnnotationUtils.findAnnotation(
                MagicAPIBaseAutoConfiguration.class, AutoConfigureBefore.class);

        assertThat(before)
                .as("MagicAPIBaseAutoConfiguration 必须声明 @AutoConfigureBefore(MagicAPIAutoConfiguration.class)，"
                        + "否则同名 bean magicResourceService 的覆盖胜负会随 classpath 顺序漂移，"
                        + "退化为不带写锁保护的 DefaultMagicResourceService 并导致 MagicAPIController 抛 ClassCastException")
                .isNotNull();
        assertThat(before.value())
                .as("@AutoConfigureBefore 必须用 value 引用上游类；"
                        + "AutoConfigurationSorter 只读 value，String 形式的 name 属性会被忽略")
                .containsExactly(MagicAPIAutoConfiguration.class);
    }
}