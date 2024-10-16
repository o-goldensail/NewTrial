package jira.phinia.tciz.utils;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Logging {
    private static Logger log = Logger.getLogger(Logging.class.getName());
    private final static String AppenderFileName = "FileLogger";
    private final static String AppenderFileNameForStatistics = "FileLoggerForStatictics";

    private final static String DeveloperLogFileName = "JiraDevLog.log";
    private final static String StatisticsLogFileName = "JiraStatistics.log";

    private static boolean initializeLogger() {
        Appender currentAppender = Logger.getRootLogger().getAppender(AppenderFileName);

        if (currentAppender != null) {
            return true;
        }

        String logDirectory;
        try {
            logDirectory = ComponentAccessor.getComponentOfType(JiraHome.class).getLogDirectory().getCanonicalPath();

            Path path = Paths.get(logDirectory);

            if (!Files.exists(path)) {
                System.out.println("Incorrect log file directory");
                return false;
            }

            Logger.getRootLogger().getLoggerRepository().resetConfiguration();

            Logger.getRootLogger().removeAllAppenders();

            FileAppender fa = new FileAppender();
            fa.setName(AppenderFileName);
            fa.setFile(String.format("%s%s%s", logDirectory, File.separator, DeveloperLogFileName));
            fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
            fa.setThreshold(LoggingLevel.JIRATOOLSLEVEL);
            fa.setAppend(true);
            fa.activateOptions();
            log.setLevel(LoggingLevel.JIRATOOLSLEVEL);
            Logger.getRootLogger().addAppender(fa);

            return true;
        } catch (IOException ex) {
            System.out.println("Incorrect log file directory");
            return false;
        }
    }

    private static boolean initializeLoggerForStatistics() {
        Appender currentStatisticsAppender = Logger.getRootLogger().getAppender(AppenderFileNameForStatistics);

        if (currentStatisticsAppender != null) {
            return true;
        }

        String logDirectory;
        try {
            logDirectory = ComponentAccessor.getComponentOfType(JiraHome.class).getLogDirectory().getCanonicalPath();

            Path path = Paths.get(logDirectory);

            if (!Files.exists(path)) {
                System.out.println("Incorrect log file directory");
                return false;
            }

            Logger.getRootLogger().getLoggerRepository().resetConfiguration();

            Logger.getRootLogger().removeAllAppenders();

            FileAppender faStatictics = new FileAppender();
            faStatictics.setName(AppenderFileNameForStatistics);
            faStatictics.setFile(String.format("%s%s%s", logDirectory, File.separator, StatisticsLogFileName));
            faStatictics.setLayout(new PatternLayout("%d;%-5p|||%c{1};%m%n")); // d -> date, p -> priority, c -> class, m -> message, n -> newline
            faStatictics.setThreshold(LoggingLevel.JIRASTATISTICSLEVEL);
            faStatictics.setAppend(true);
            faStatictics.activateOptions();
            log.setLevel(LoggingLevel.JIRASTATISTICSLEVEL);
            Logger.getRootLogger().addAppender(faStatictics);

            return true;
        } catch (IOException ex) {
            System.out.println("Incorrect log file directory");
            return false;
        }
    }

    /**
     * Logs given message to log file
     *
     * @param logMessage a message to be logged
     */
    public static void log(String logMessage) {
        if (initializeLogger()) {
            log.log(LoggingLevel.JIRATOOLSLEVEL, logMessage);
        }
    }

    public static void logStatictics(String logMessage)
    {
        try
        {
            if (initializeLoggerForStatistics())
            {
                log.log(LoggingLevel.JIRASTATISTICSLEVEL, logMessage);
            }
        }
        catch (Exception ex)
        {
            log(ex.getMessage());
        }
    }

    public static void logStatisticsFromReports(String reportName, ApplicationUser currentLoggedUser, String projectKey, String parameters)
    {
        logStatictics(getFormattedLogMessage("Report", reportName, currentLoggedUser, projectKey, parameters));
    }

    public static void logStatisticsFromGadgets(String reportName, ApplicationUser currentLoggedUser, String projectKey, String parameters) {
        logStatictics(getFormattedLogMessage("Gadget", reportName, currentLoggedUser, projectKey, parameters));
    }

    /**
     * Logs all attributes with given type of class for each item of a list
     *
     * @param list a list instance to be logged
     */
    public static void log(List<?> list) {

        if (list == null || list.size() == 0) return;

        Field[] fields;
        try {
            fields = Class.forName(list.get(0).getClass().getTypeName()).getDeclaredFields();

            if (fields == null || fields.length == 0) return;

            Object value;

            for (Object aList : list) {
                for (Field field : fields) {
                    value = getInvokeValue(field, aList);
                    log(String.format("Class: %s -- Index: %d -- Field: %s -- Value: %s", aList.getClass().getTypeName(), list.indexOf(aList), field.getName(), value));
                }
            }
        } catch (ClassNotFoundException e) {
            log(String.format("%s > %s > %s", "LoggingUtils > public static void log(List<?> list) ", e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    /**
     * Logs class' attributes to log file
     *
     * @param clazz a class instance to be logged
     */
    public static void log(Object clazz) {
        Object value;

        Field[] fields = clazz.getClass().getDeclaredFields();

        if (fields == null || fields.length == 0) return;

        for (Field field : fields) {
            value = getInvokeValue(field, clazz);
            log(String.format("Class: %s -- Field: %s -- Value: %s", clazz.getClass().getTypeName(), field.getName(), value));
        }
    }

    private static Object getInvokeValue(Field field, Object clazz) {
        String fieldName = field.getName();
        Method getter;

        try {
            getter = new PropertyDescriptor(fieldName, clazz.getClass(), "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null).getReadMethod();

            if (getter == null) return null;

            return getter.invoke(clazz);
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            log(String.format("%s > %s > %s", "LoggingUtils > private static Object getInvokeValue(Field field, Object clazz) ", e.getClass().getSimpleName(), e.getMessage()));
            return null;
        }
    }

    private static String getFormattedLogMessage(String type, String reportName, ApplicationUser currentLoggedUser, String projectKey, String parameters) {
        return  String.format("%s;%s;%s;%s;%s", type, reportName, currentLoggedUser.getUsername(), projectKey, parameters);
    }
}
