package com.ardentbot;

import com.google.common.util.concurrent.MoreExecutors;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * Full credit to https://github.com/Almighty-Alpaca/JDA-Butler/blob/master/src/main/java/com/almightyalpaca/discord/jdabutler/eval/Engine.java
 */
public enum EvalEngine {
    GROOVY("Groovy", "groovy") {
        @Override
        public Triple<Object, String, String> eval(final Map<String, Object> fields, final Collection<String> classImports, final Collection<String> packageImports, final int timeout, final String script) {
            StringBuilder importString = new StringBuilder();
            for (final String s : classImports)
                importString.append("import ").append(s).append(";");
            for (final String s : packageImports)
                importString.append("import ").append(s).append(".*;");
            return this.eval(fields, timeout, importString + script, new GroovyScriptEngineImpl());
        }

    },
    JAVASCRIPT("JavaScript", "js", "javascript") {

        private final ScriptEngineManager engineManager = new ScriptEngineManager();

        @Override
        public Triple<Object, String, String> eval(final Map<String, Object> fields, final Collection<String> classImports, final Collection<String> packageImports, final int timeout, String script) {
            StringBuilder importString = new StringBuilder();
            for (final String s : packageImports)
                importString.append(s).append(", ");
            importString = new StringBuilder(replaceLast(importString.toString(), ", ", ""));

            script = " (function() { with (new JavaImporter(" + importString + ")) {" + script + "} })();";
            return this.eval(fields, timeout, script, this.engineManager.getEngineByName("nashorn"));
        }

    };


    private final static ScheduledExecutorService service = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Eval-Thread"));

    private final List<String> codes;

    private final String name;

    EvalEngine(final String name, final String... codes) {
        this.name = name;
        this.codes = new ArrayList<>();
        for (final String code : codes)
            this.codes.add(code.toLowerCase());
    }

    public static EvalEngine getEngineByCode(String code) {
        code = code.toLowerCase();
        for (final EvalEngine engine : EvalEngine.values())
            if (engine.codes.contains(code))
                return engine;
        return null;
    }

    public static void shutdown() {
        MoreExecutors.shutdownAndAwaitTermination(EvalEngine.service, 10, TimeUnit.SECONDS);
    }

    public abstract Triple<Object, String, String> eval(Map<String, Object> fields, final Collection<String> classImports, final Collection<String> packageImports, int timeout, String script);

    public List<String> getCodes() {
        return this.codes;
    }

    public String getName() {
        return this.name;
    }

    protected Triple<Object, String, String> eval(final Map<String, Object> fields, final int timeout, final String script, final ScriptEngine engine) {

        for (final Entry<String, Object> shortcut : fields.entrySet())
            engine.put(shortcut.getKey(), shortcut.getValue());

        final StringWriter outString = new StringWriter();
        final PrintWriter outWriter = new PrintWriter(outString);
        engine.getContext().setWriter(outWriter);

        final StringWriter errorString = new StringWriter();
        final PrintWriter errorWriter = new PrintWriter(errorString);
        engine.getContext().setErrorWriter(errorWriter);

        final ScheduledFuture<Object> future = EvalEngine.service.schedule(() -> engine.eval(script), 0, TimeUnit.MILLISECONDS);

        Object result = null;

        try {
            result = future.get(timeout, TimeUnit.SECONDS);
        } catch (final ExecutionException e) {
            errorWriter.println(e.getCause().toString());
        } catch (TimeoutException | InterruptedException e) {
            future.cancel(true);
            errorWriter.println(e.toString());
        }

        return new Triple<>(result, outString.toString(), errorString.toString());
    }

    public static class Import {

        private final String name;

        private final Type type;

        public Import(final Import.Type type, final String name) {
            this.type = type;
            this.name = name;
        }

        public final String getName() {
            return this.name;
        }

        public final Type getType() {
            return this.type;
        }

        public enum Type {
            CLASS, PACKAGE
        }

    }

    /**
     * Method retrieved from https://stackoverflow.com/questions/1660034/replace-last-part-of-string
     */
    String replaceLast(String string, String from, String to) {
        int lastIndex = string.lastIndexOf(from);
        if (lastIndex < 0) return string;
        String tail = string.substring(lastIndex).replaceFirst(from, to);
        return string.substring(0, lastIndex) + tail;
    }
}