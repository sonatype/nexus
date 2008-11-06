package org.sonatype.nexus.web;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Log4JConfigurationUtils {

	public static void configure(String logFile)
	{
		Logger rootLogger = Logger.getRootLogger();
		
		rootLogger.setLevel( Level.INFO);
		
		// log level for specific levels
		rootLogger.getLoggerRepository().getLogger("org.apache.commons").setLevel(Level.INFO);
		
		rootLogger.getLoggerRepository().getLogger("httpclient").setLevel(Level.INFO);
		
		rootLogger.getLoggerRepository().getLogger("org.apache.http").setLevel(Level.INFO);
		
		rootLogger.getLoggerRepository().getLogger("org.jsecurity").setLevel(Level.WARN);
		
		rootLogger.getLoggerRepository().getLogger("org.restlet").setLevel(Level.WARN);
		
		
		// append DailyRollingFileAppender
		
		DailyRollingFileAppender dailyRollingFileAppender = new DailyRollingFileAppender();
		
		dailyRollingFileAppender.setName("DailyRollingFileAppender");
		
		dailyRollingFileAppender.setFile(logFile);
		
		dailyRollingFileAppender.setAppend(true);
		
		dailyRollingFileAppender.setDatePattern("'.'yyyy-MM-dd");
		
		PatternLayout filePatternLayout = new PatternLayout();
		
		filePatternLayout.setConversionPattern("%4d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%t:%x] - %c: %m%n");
		
		dailyRollingFileAppender.setLayout(filePatternLayout);
		
		dailyRollingFileAppender.activateOptions();
		
		rootLogger.addAppender(dailyRollingFileAppender );
		
		// append ConsoleAppender
		
		ConsoleAppender consoleAppender = new ConsoleAppender();
		
		consoleAppender.setName("ConsoleAppender");
		
		PatternLayout consolePatternLayout = new PatternLayout();
		
		consolePatternLayout.setConversionPattern("%d %p [%c] - %m%n");
		
		consoleAppender.setLayout(consolePatternLayout);
		
		consoleAppender.activateOptions();
		
		rootLogger.addAppender(consoleAppender );
	}
	
}
