/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.cc007.eventsk;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionType;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class EventSK extends JavaPlugin {

    private FileConfiguration config = null;
    private File configFile = null;

    @Override
    public void onEnable() {
        /* Config stuffs */
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        /* Skript stuff */
        Skript.registerAddon(this);
        for (Iterator<?> it = getConfig().getList("events").iterator(); it.hasNext();) {
            try {
                String fullEventName = (String) it.next();
                Class<? extends Event> eventClass;
                eventClass = Class.forName(fullEventName).asSubclass(Event.class);
                Skript.registerEvent("Bukkit event", BukkitEvent.class, eventClass, "bukkit event %strings%");

                //Determine the simple event name
                String[] splitName = fullEventName.split("\\.");
                String eventName = splitName[splitName.length - 1];
                //loop through all the methods of the event to make skript variables for the getters
                for (Method method : eventClass.getMethods()) {

                    if (method.getName().equals("isAsynchronous")
                            || method.getName().equals("getHandlers")
                            || method.getName().equals("getHandlerList")
                            || method.getName().equals("wait")
                            || method.getName().equals("equals")
                            || method.getName().equals("hashCode")
                            || method.getName().equals("notify")
                            || method.getName().equals("notifyAll")) {
                        continue;
                    }
                    //Bukkit.getLogger().log(Level.INFO, "Return type of " + method.getName() + ": " + method.getReturnType().getName());
                    Skript.registerExpression(ExprBukkitEventVariable.class, method.getReturnType(), ExpressionType.SIMPLE, eventName + "-" + method.getName());
                }
            } catch (ClassNotFoundException ex) {
                getLogger().warning("[EventSK] The event named " + ex.getMessage() + " doesn't exist or can't be reached. Make sure you spelled the event name correctly and that the event is available on the server.");
            }
        }

    }

    public static EventSK getPlugin() {
        Plugin eventSK = Bukkit.getServer().getPluginManager().getPlugin("EventSK");
        if (eventSK != null && eventSK.isEnabled() && eventSK instanceof EventSK) {
            return (EventSK) eventSK;
        } else {
            Bukkit.getLogger().log(Level.WARNING, "EventSK has not been enabled yet");
            return null;
        }
    }

    /**
     * Method to reload the config.yml config file
     */
    @Override
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(this.getResource("config.yml"), "UTF8");
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
    }

    /**
     * Method to get YML content of the config.yml config file
     *
     * @return YML content of the catagories.yml config file
     */
    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    /**
     * Method to save the config.yml config file
     */
    @Override
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    /**
     * Method to save the default config file
     */
    @Override
    public void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

}
