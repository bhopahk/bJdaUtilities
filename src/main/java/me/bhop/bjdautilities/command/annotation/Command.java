package me.bhop.bjdautilities.command.annotation;

import net.dv8tion.jda.core.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String value() default "";

    String[] label() default {};

    Permission permission() default Permission.UNKNOWN;

    int minArgs() default 0;

    Class<?>[] children() default Void.class;
}
