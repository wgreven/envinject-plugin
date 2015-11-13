package org.jenkinsci.plugins.envinject.service;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class PropertiesVariablesRetriever implements FilePath.FileCallable<Map<String, String>> {

    private String propertiesFilePath;

    private Map<String, String> propertiesContent;

    private Map<String, String> currentEnvVars;

    private EnvInjectLogger logger;

    public PropertiesVariablesRetriever(String propertiesFilePath, Map<String, String> propertiesContent, Map<String, String> currentEnvVars, EnvInjectLogger logger) {
        this.propertiesFilePath = propertiesFilePath;
        this.propertiesContent = propertiesContent;
        this.currentEnvVars = currentEnvVars;
        this.logger = logger;
    }

    public Map<String, String> invoke(File base, VirtualChannel channel) throws IOException, InterruptedException {
        Map<String, String> result = new LinkedHashMap<String, String>();

        try {

            PropertiesLoader loader = new PropertiesLoader();

            //Add the properties file
            if (propertiesFilePath != null) {
                String propertiesFilePathResolved = Util.replaceMacro(propertiesFilePath, currentEnvVars);
                propertiesFilePathResolved = propertiesFilePathResolved.replace("\\", "/");
                File propertiesFile = getFile(base, propertiesFilePathResolved);
                if (propertiesFile == null) {
                    String message = String.format("The given properties file path '%s' doesn't exist.", propertiesFilePathResolved);
                    logger.error(message);
                    String patternMessage = String.format("Missing file path was resolved from pattern '%s' .", propertiesFilePath);
                    logger.error(patternMessage);
                    throw new EnvInjectException(message);
                }
                logger.info(String.format("Injecting as environment variables the properties file path '%s'", propertiesFilePathResolved));
                result.putAll(loader.getVarsFromPropertiesFile(propertiesFile, currentEnvVars));
                logger.info("Variables injected successfully.");
            }

            //Add the properties content
            if (propertiesContent != null) {
                PropertiesGetter propertiesGetter = new PropertiesGetter();
                logger.info(String.format("Injecting as environment variables the properties content %n%s%n", propertiesGetter.getPropertiesContentFromMapObject(propertiesContent)));
                result.putAll(propertiesContent);
                logger.info("Variables injected successfully.");
            }

        } catch (EnvInjectException envEx) {
            throw new IOException(envEx.getMessage());
        }

        return result;
    }

    private File getFile(File base, String scriptFilePath) {
        File file = new File(new FilePath(base).child(scriptFilePath).getRemote());
        return file.exists() ? file : null;
    }

}
