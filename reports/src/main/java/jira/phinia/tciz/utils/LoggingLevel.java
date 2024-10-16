package jira.phinia.tciz.utils;

import org.apache.log4j.Level;

public class LoggingLevel extends Level {
    private static final int JIRATOOLSLEVEL_INT = OFF_INT - 10;
    private static final int JIRASTATISTICSLEVELL_INT = OFF_INT - 9;

    public static final Level JIRATOOLSLEVEL = new LoggingLevel(JIRATOOLSLEVEL_INT, "JIRATOOLSLEVEL", 10);
    public static final Level JIRASTATISTICSLEVEL = new LoggingLevel(JIRASTATISTICSLEVELL_INT, "JIRASTATISTICSLEVEL", 10);

    private LoggingLevel(int arg0, String arg1, int arg2) {
        super(arg0, arg1, arg2);
    }
}
