package com.wind.aop;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @ClassName TestAop
 * @Description TODO
 * @Author xiaowj
 * @Date DATE{TIME}
 */
@SpringBootApplication(scanBasePackages = "com.wind.*")
public class TestAop {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestAop.class);
        TestAopBean bean = context.getBean(TestAopBean.class);
        bean.test();
    }
}
