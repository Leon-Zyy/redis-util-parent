package com.jd.logistics.cache.jedis.support;

import java.nio.charset.StandardCharsets;

/**
 * Lua脚本
 *
 * @author Y.Y.Zhao
 */
public final class Lua {
    private String sha;
    private String script;

    public Lua(String script) {
        this.script = script;
        this.sha = SHA.digest(script.getBytes(StandardCharsets.UTF_8));
    }

    public String getSha() {
        return sha;
    }

    public String getScript() {
        return script;
    }
}
