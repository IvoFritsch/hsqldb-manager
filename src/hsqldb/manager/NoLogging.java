/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.manager;

import org.eclipse.jetty.util.log.Logger;

/**
 *
 * @author ivoaf
 */
public class NoLogging implements Logger {
    @Override public String getName() { return "no"; }
    @Override public void warn(String msg, Object... args) { }
    @Override public void warn(Throwable thrown) { }
    @Override public void warn(String msg, Throwable thrown) { }
    @Override public void info(String msg, Object... args) { }
    @Override public void info(Throwable thrown) { }
    @Override public void info(String msg, Throwable thrown) { }
    @Override public boolean isDebugEnabled() { return false; }
    @Override public void setDebugEnabled(boolean enabled) { }
    @Override public void debug(String msg, Object... args) { }
    @Override public void debug(Throwable thrown) { }
    @Override public void debug(String msg, Throwable thrown) { }
    @Override public Logger getLogger(String name) { return this; }
    @Override public void ignore(Throwable ignored) { }
    @Override public void debug(String string, long l) { }
}