package com.jd.logistics.cache.redis;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * ProxyBenchmark
 *
 * @author Y.Y.Zhao
 */
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
//@Threads(3)
public class ProxyBenchmark {
    private static final Good impl = new GoodImpl();
    private static final Good goodJavassist = getByJavassist();
    private static final Good good = (Good) Proxy.newProxyInstance(Good.class.getClassLoader(), new Class[]{Good.class},
            (proxy, method, args) -> method.invoke(impl, args));

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int proxyStatic() {
        return good.expire();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int proxyNew() {
        Good good = (Good) Proxy.newProxyInstance(Good.class.getClassLoader(), new Class[]{Good.class},
                (proxy, method, args) -> method.invoke(impl, args));
        return good.expire();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int implNew() {
        return new GoodImpl().expire();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int implStatic() {
        return impl.expire();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int javassistNew() {
        return getByJavassist().expire();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int javassistStatic() {
        return goodJavassist.expire();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ProxyBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    public interface Good {
        int expire();
    }

    static class GoodImpl implements Good {

        @Override
        public int expire() {
            return 3;
        }
    }

    private static Class aClass;

    private static Good getByJavassist() {
        try {
            if (aClass == null) {
                ClassPool pool = ClassPool.getDefault();
                CtClass cc = pool.makeClass("com.jd.gj");
                cc.addInterface(pool.get(Good.class.getName()));
                for (CtMethod m : cc.getMethods()) {
                    if (m.getDeclaringClass().isInterface()) {
                        CtMethod mc = CtNewMethod.copy(m, cc, null);
                        mc.setBody("return 5;");
                        cc.addMethod(mc);
                    }
                }
                aClass = cc.toClass();
            }
            return (Good) aClass.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}