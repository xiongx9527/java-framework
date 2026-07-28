package com.xiongx9527.fw.plus.common.validator;

import com.xiongx9527.fw.plus.common.web.WebFluxRequestContextHolder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;

public class CommonsValidatorWebFluxBindingTest {

    public void testBindingResultBindsWebFluxQueryParameters() {
        try {
            WebFluxRequestContextHolder.set(MockServerWebExchange.from(MockServerHttpRequest
                    .get("/users?name=alice&roles=admin&roles=dev&age=7")
                    .build()));
            RequestContextHolder.resetRequestAttributes();

            SampleForm form = new SampleForm();
            BindingResult bindingResult = CommonsValidator.bindingResult(form);

            assertFalse(bindingResult.hasErrors());
            assertEquals("alice", form.getName());
            assertEquals(Integer.valueOf(7), form.getAge());
            assertArrayEquals(new String[]{"admin", "dev"}, form.getRoles());
        } finally {
            WebFluxRequestContextHolder.clear();
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private void assertArrayEquals(String[] expected, String[] actual) {
        if (expected == null ? actual != null : actual == null || expected.length != actual.length) {
            throw new AssertionError("expected array length:<" + (expected == null ? null : expected.length)
                    + "> but was:<" + (actual == null ? null : actual.length) + ">");
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] == null ? actual[i] != null : !expected[i].equals(actual[i])) {
                throw new AssertionError("expected[" + i + "]:<" + expected[i] + "> but was:<" + actual[i] + ">");
            }
        }
    }

    private void assertFalse(boolean actual) {
        if (actual) {
            throw new AssertionError("expected false but was true");
        }
    }

    public static class SampleForm {
        private String name;
        private Integer age;
        private String[] roles;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String[] getRoles() {
            return roles;
        }

        public void setRoles(String[] roles) {
            this.roles = roles;
        }
    }
}
