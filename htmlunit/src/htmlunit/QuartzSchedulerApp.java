package htmlunit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzSchedulerApp {
    static Properties prop = new Properties();
    private static final String TRIGGER_NAME = "SVFE_GET_XLSX";
    private static final String GROUP = "SVFEGroup";
    private static final String JOB_NAME = "SVFEJob";
    private static Scheduler scheduler;

    static Logger logger = Logger.getLogger(QuartzJob.class);

    public static void main(String[] args) throws Exception {
        String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j.xml";
        DOMConfigurator.configure(log4jConfigFile);
        logger.info(" QuartzSchedulerApp main thread: " + Thread.currentThread().getName());
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        Trigger trigger = buildCronSchedulerTrigger();
        scheduleJob(trigger);
    }

    private static void scheduleJob(Trigger trigger) throws Exception {
        JobDetail someJobDetail = JobBuilder.newJob(QuartzJob.class).withIdentity(JOB_NAME, GROUP).build();
        scheduler.scheduleJob(someJobDetail, trigger);
    }

    private static Trigger buildCronSchedulerTrigger() {
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            System.exit(0);
        }
        String CRON_EXPRESSION = prop.getProperty("cron");

        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(TRIGGER_NAME, GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(CRON_EXPRESSION)).build();
        return trigger;
    }
}
