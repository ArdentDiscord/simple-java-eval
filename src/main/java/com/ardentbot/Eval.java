package com.ardentbot;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class Eval {
    public static Triple<Object, String, String> eval(String code, HashMap<String, Object> shortcuts, Collection<String> defaultImports, int timeout) {
        return EvalEngine.GROOVY.eval(shortcuts, Collections.emptyList(), defaultImports, timeout, code);
    }

    public static Triple<Object, String, String> evalJda(String code, HashMap<String, Object> shortcuts, int timeout) {
        return EvalEngine.GROOVY.eval(shortcuts, Collections.emptyList(), Arrays.asList("net.dv8tion.jda.core.entities.impl", "net.dv8tion.jda.core.managers",
                "net.dv8tion.jda.core.entities", "net.dv8tion.jda.core", "java.lang", "java.io", "java.math", "java.util", "java.util.concurrent",
                "java.time", "java.lang.System")
                , timeout, code);
    }
}
