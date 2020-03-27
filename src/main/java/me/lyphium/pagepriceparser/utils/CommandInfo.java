package me.lyphium.pagepriceparser.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandInfo {

    String description() default "";

    String usage() default "";

    String[] aliases() default {};

}