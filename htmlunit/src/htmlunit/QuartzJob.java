package htmlunit;

import java.io.File;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class QuartzJob implements Job {
    Logger logger = Logger.getLogger(QuartzJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j.xml";
        DOMConfigurator.configure(log4jConfigFile);
        MyTask mytask = new MyTask();
        mytask.perform();
    }

}
