/*
 * The MIT License
 *
 * Copyright 2016 Rik Schaaf aka CC007 (http://coolcat007.nl/).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.cc007.eventsk;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.Event;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class ExprBukkitEventVariable<T> extends SimpleExpression<T> {

    private Class<? extends Event> eventClass;
    private Method eventMethod;

    @Override
    protected T[] get(Event e) {
        try {
            return (T[]) new Object[]{eventClass.isInstance(e) ? eventMethod.invoke(e) : null};
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new SkriptAPIException("Unable to get value", ex);
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends T> getReturnType() {
        return (Class<? extends T>) eventMethod.getReturnType();
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String eventName = parseResult.expr.split("-")[0];
        String methodName = parseResult.expr.split("-")[1];
        Class<? extends Event> evtClass = null;
        try {
            for (String event : EventSK.getPlugin().getConfig().getStringList("events")) {
                if (event.contains(eventName)) {
                    evtClass = Class.forName(event).asSubclass(Event.class);
                    break;
                }
            }
            if (evtClass == null) {
                Skript.error("Cannot use '" + parseResult.expr + "', its event isn't added to the config");
                return false;
            }
            if (!ScriptLoader.isCurrentEvent(evtClass)) {
                Skript.error("Cannot use '" + parseResult.expr + "' outside of " + eventName + " events");
                return false;
            }
            this.eventClass = evtClass;
            this.eventMethod = evtClass.getMethod(methodName);
            return true;
        } catch (ClassNotFoundException ex) {
            Skript.error("Cannot use '" + parseResult.expr + "', its corresponding bukkit event doesn't exist or can't be reached (check the spelling of the event!)");
        } catch (NoSuchMethodException ex) {
            Skript.error("Cannot use '" + parseResult.expr + "', its event doesn't have a method named " + methodName);
        } catch (SecurityException ex) {
            Logger.getLogger(ExprBukkitEventVariable.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public String toString(Event arg0, boolean arg1) {
        return eventClass.getName() + "-" + eventMethod.getName();
    }

}
