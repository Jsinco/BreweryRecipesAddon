package dev.jsinco.recipes;

import java.io.File;

/**
 * Creates a new config file that the owner can use to update their config file
 */
public class LazyConfigUpdater {

    private final static String latestVersion = "1.6-BETA";

    private final String configVersion = Config.get().getString("version");

    public LazyConfigUpdater() {
    }

    public boolean isConfigOutdated() {
        int configVersionInt = Integer.parseInt(this.configVersion.replace(".", "").replace("-BETA", ""));
        int latestVersionInt = Integer.parseInt(latestVersion.replace(".", "").replace("-BETA", ""));
        return configVersionInt < latestVersionInt;
    }

    public void createNewConfigFile() {
        File currentConfigFile = new File(Recipes.getAddonFileManager().getAddonFolder(), "recipesConfig.yml");
        if (!currentConfigFile.renameTo(new File(Recipes.getAddonFileManager().getAddonFolder(), "recipesConfig-OLD.yml"))) {
            Recipes.getAddonLogger().info("Failed to rename the old config file");
            return;
        }
        Recipes.getAddonFileManager().generateFile("recipesConfig.yml");

        Recipes.getAddonLogger().info("Created a new config file, please transfer all your old settings to the newly created file." +
                " Your old config file has been renamed to recipesConfig-OLD.yml");
    }
}
