package com.gs.leumi.adabase;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
@EnableAutoConfiguration
@SpringBootApplication
public class AdabaseApplication implements CommandLineRunner, LeaderSelectorListener {

	public static void main(String[] args) {
		try {
		SpringApplication.run(AdabaseApplication.class, args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	private static Logger logger = LoggerFactory.getLogger(AdabaseApplication.class);
	public static boolean STOP = false;
	@Autowired
	private ApplicationContext appContext;
	@Autowired
	private com.gs.leumi.adabase.config.Configuration config;
	@Autowired
	private MQClient mqClient;

	@Override
	public void run(String... args) throws Exception {
		/*
		Options options = new Options();
		Option spaceName = new Option("s", "space", true, "Space name");
		Option MQhostname = new Option("h", "hostname", true, "Mq server  hostname");
		Option channel = new Option("c", "channel", true, "MQ channel");
		Option port = new Option("p", "port", true, "port number");
		Option qManager = new Option("m", "qmanager", true, "Queue manager");
		Option qName = new Option("q", "queue", true, "Queue name");
		Option sslChipherSuite = new Option("e", "sslCipherSuite", true, "ssl Cipher Suite if not default");
		Option filePath = new Option("f", "file", true, "full path to events xml file");
		Option maxEvents = new Option("l", "maxevents", true, "Max events to consume from the queue. -1 for unbound");
		options.addOption(spaceName).addOption(MQhostname).addOption(channel).addOption(port)
				.addOption(qManager).addOption(qName).addOption(filePath).addOption(sslChipherSuite).addOption(maxEvents);
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
			if(!((cmd.hasOption('h') && cmd.hasOption('c') && cmd.hasOption('p') &&
					cmd.hasOption('m') && cmd.hasOption('q'))||cmd.hasOption('f'))){
				throw new ParseException("Missing arguments. Received: " + Arrays.toString(cmd.getOptions()));
			}
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			SpringApplication.exit(appContext, ()-> -1);
			System.exit(-1);
		}
*/
		System.out.println("config.getSchemasPath()" + config.getSchemasPath());
		final CuratorFramework client = CuratorFrameworkFactory.newClient(config.getZookeeper().getConnectionStr(),
				new ExponentialBackoffRetry(1000, 3));
		client.start();
		final LeaderSelector selector = new LeaderSelector(client, config.getZookeeper().getPath(), this);
		selector.start();

	}

	@Override
	public void takeLeadership(final CuratorFramework client) {
		boolean dontStop = true;
		while(dontStop) {
			try {
				mqClient.run();
				dontStop = false;
			} catch (Exception e) {
				logger.error("Failed to start MQ client ", e);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
		logger.info("Leader selection state changed to " + connectionState);
	}


	@PreDestroy
	private void onExit(){
		try {
			logger.info("\n\nRequesting parser to stop\n");
			System.out.println("\n\nRequesting parser to stop\n");
			STOP = true;
			Thread.sleep(1000);
			System.out.println("Shutting down....");
		}catch (InterruptedException e){
			Thread.currentThread().interrupt();
			logger.error("Interupted", e);
		}

	}

}
