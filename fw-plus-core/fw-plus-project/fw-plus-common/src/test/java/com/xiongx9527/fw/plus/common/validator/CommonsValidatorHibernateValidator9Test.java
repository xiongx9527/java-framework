package com.xiongx9527.fw.plus.common.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * CommonsValidator 在 Hibernate Validator 9（Spring Boot 4.1 基线）下的路径节点适配回归测试。
 *
 * <p>Hibernate Validator 9 移除了 8.x 的 PathImpl/NodeImpl 内部类，本测试通过真实 Bean Validation
 * 校验流程（嵌套属性、容器元素索引、自定义约束）验证 basePath 反射适配后仍能正确构建 JSON 路径
 * 并得到预期校验结果。</p>
 */
public class CommonsValidatorHibernateValidator9Test {

    /**
     * 嵌套属性上的 @CommonsValidatorAnn 在 HV9 下应正常给出校验结果（路径提取不因内部 API 变化而异常）。
     */
    public void testNestedCommonsValidatorAnnWorksWithHibernateValidator9() {
        try {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

            SampleForm form = new SampleForm();
            form.setNested(new SampleNested());
            form.getNested().setEmail("alice@example.com");
            CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, form);

            Set<ConstraintViolation<SampleForm>> validResult = validator.validate(form);
            assertTrue("合法邮箱应通过校验，实际违规：" + validResult, validResult.isEmpty());

            form.getNested().setEmail("not-an-email");
            Set<ConstraintViolation<SampleForm>> invalidResult = validator.validate(form);
            assertEquals(1, invalidResult.size());
            assertEquals("nested.email", invalidResult.iterator().next().getPropertyPath().toString());
        } finally {
            CommonsValidatorHolder.clear();
        }
    }

    /**
     * 由 HV9 路径节点构建的 jsonPath 变量应等于 $.nested.pathProbe。
     */
    public void testJsonPathVariableBuiltFromValidator9PathNodes() {
        try {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

            SampleForm form = new SampleForm();
            form.setNested(new SampleNested());
            form.getNested().setPathProbe("probe");
            CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, form);

            Set<ConstraintViolation<SampleForm>> result = validator.validate(form);
            assertTrue("jsonPath 变量应为 $.nested.pathProbe，实际违规：" + result, result.isEmpty());
        } finally {
            CommonsValidatorHolder.clear();
        }
    }

    /**
     * 容器元素（List 索引）场景下路径节点与索引应正常参与 JSON 路径构建。
     */
    public void testListItemIndexPathWorksWithHibernateValidator9() {
        try {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

            SampleItem item = new SampleItem();
            item.setEmail("alice@example.com");
            item.setPathProbe("probe");
            SampleForm form = new SampleForm();
            form.setNested(new SampleNested());
            form.getNested().setItems(Collections.singletonList(item));
            CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, form);

            Set<ConstraintViolation<SampleForm>> validResult = validator.validate(form);
            assertTrue("列表元素合法邮箱应通过校验，实际违规：" + validResult, validResult.isEmpty());

            item.setEmail("not-an-email");
            Set<ConstraintViolation<SampleForm>> invalidResult = validator.validate(form);
            assertEquals(1, invalidResult.size());
            assertEquals("nested.items[0].email", invalidResult.iterator().next().getPropertyPath().toString());
        } finally {
            CommonsValidatorHolder.clear();
        }
    }

    public static class SampleForm {
        @Valid
        private SampleNested nested;

        public SampleNested getNested() {
            return nested;
        }

        public void setNested(SampleNested nested) {
            this.nested = nested;
        }
    }

    public static class SampleNested {
        @CommonsValidatorAnn(message = "邮箱格式不正确", dataValidType = CommonsValidatorAnn.DataValidType.STRING_IS_EMAIL)
        private String email;

        @CommonsValidatorAnn(message = "jsonPath 不正确",
                dataValidType = CommonsValidatorAnn.DataValidType.VALID_EXPRESSIONS,
                dataValidExpressions = "#jsonPath == '$.nested.pathProbe'")
        private String pathProbe;

        @Valid
        private List<SampleItem> items;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPathProbe() {
            return pathProbe;
        }

        public void setPathProbe(String pathProbe) {
            this.pathProbe = pathProbe;
        }

        public List<SampleItem> getItems() {
            return items;
        }

        public void setItems(List<SampleItem> items) {
            this.items = items;
        }
    }

    public static class SampleItem {
        @CommonsValidatorAnn(message = "邮箱格式不正确", dataValidType = CommonsValidatorAnn.DataValidType.STRING_IS_EMAIL)
        private String email;

        @CommonsValidatorAnn(message = "jsonPath 不正确",
                dataValidType = CommonsValidatorAnn.DataValidType.VALID_EXPRESSIONS,
                dataValidExpressions = "#jsonPath == '$.nested.items[0].pathProbe'")
        private String pathProbe;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPathProbe() {
            return pathProbe;
        }

        public void setPathProbe(String pathProbe) {
            this.pathProbe = pathProbe;
        }
    }

    private void assertTrue(String message, boolean actual) {
        if (!actual) {
            throw new AssertionError(message);
        }
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }
}
