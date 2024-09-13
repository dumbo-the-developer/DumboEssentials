package com.meteordevelopments.dumboEssentials.configs;

import com.meteordevelopments.dumboEssentials.DumboEssentials;
import com.meteordevelopments.dumboEssentials.logger.Logger;
import lombok.Getter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@Getter
public class Config extends YamlConfiguration {
    private final DumboEssentials plugin;
    private final File file;

    public Config(File dataFolder , String name) {
        this.plugin = DumboEssentials.getPlugin();
        this.file = new File(dataFolder , name);

        if (!dataFolder.exists()){
            dataFolder.mkdir();
        }

        if (!file.exists()){
            options().copyDefaults(true);
            plugin.saveResource(name , false);
        }
        load();
    }


    public Config(String name){
        this(DumboEssentials.getPlugin().getDataFolder(), name);
    }

    private void load(){
        try {
            super.load(file);
        }catch (FileNotFoundException ex){
            Logger.warn("Configuration file not found: " + file.getName());
        }catch (IOException ex){
            Logger.severe("IO error while loading configuration file: " + file.getName());

        }catch (InvalidConfigurationException ex){
            Logger.severe( "Invalid configuration in file: " + file.getName());
        }
    }

    public void save(){
        try{
            super.save(file);
        }catch (IOException ex){
            Logger.severe("IO error while saving configuration file: " + file.getName());
        }
    }

    public void reload(){
        load();
    }
}