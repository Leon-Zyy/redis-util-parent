package com.jd.logistics.cache.redis;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.cglib.beans.BeanCopier;

import java.lang.reflect.InvocationTargetException;
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
public class BeanBenchmark {
    public static final BeanCopier CP = BeanCopier.create(Good.class, Good1.class, false);
    private static Good source = new Good("id", "value");
    private static Good1 target = new Good1();

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Good1 apache() throws InvocationTargetException, IllegalAccessException {
        org.apache.commons.beanutils.BeanUtils.copyProperties(target, source);
        return target;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Good1 apache1() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        org.apache.commons.beanutils.PropertyUtils.copyProperties(target, source);
        return target;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Good1 spring() {
        org.springframework.beans.BeanUtils.copyProperties(source, target);
        return target;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Good1 copier() {
        CP.copy(source, target, null);
        return target;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BeanBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    public static class Good {
        private String id;
        private String value;

        public Good(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Good1 {
        private String id;
        private String value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}