package com.gigaspaces.odsx.noderebalancer;

import com.gigaspaces.odsx.noderebalancer.admin.model.AdminConfiguration;
import com.gigaspaces.odsx.noderebalancer.engine.FlowEngine;
import com.gigaspaces.odsx.noderebalancer.policy.Policy;
import com.gigaspaces.odsx.noderebalancer.policy.PolicyAssociation;
import com.gigaspaces.odsx.noderebalancer.policy.PolicyConfiguration;
import com.gigaspaces.odsx.noderebalancer.policy.ServerConfiguration;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class RunRebalancer {
  protected String[] locators = new String[] { "" };
  
  protected String lookupGroup;
  
  protected String userName;
  
  protected String password;
  
  protected String passwordFilename;
  
  protected String hostIp;
  
  protected String zone;
  
  protected Integer gscCount;
  
  static Logger logger = Logger.getLogger(Launcher.class.getName());
  
  public static void printUsage() {
    System.out.println("This program is used to listen for Admin Events and Rebalance (= primary secondary) containers on the target host.");
    System.out.println("Available arguments: are -locators x -spaceName x -username x -password x -passwordFilename x -hostIp x -zone x -gscCount x");
    System.out.println("Or -help to print this help.");
    System.out.println("  -lookupgroup,      lookup group name.");
    System.out.println("  -locators,      lookup locators. Typically you will specify 3, one for each data center, separated with commas.");
    System.out.println("       Example: -locators server1:4174,server2:4174,server3:4174");
    System.out.println("  -username,      username. Include if the XAP cluster is secured.");
    System.out.println("  -password,      password. Include if the XAP cluster is secured.");
    System.out.println("  -passwordFilename, </path/to/password/file>.");
    System.out.println("       Filename of file containing password. Use this if you want the program to read the password from a file.");
    System.out.println("  -hostIp the server host, the containers on which will be rebalanced.");
    System.out.println("  -zone  the container zone.");
    System.out.println("  -gscCount count of containers");
    System.exit(0);
  }
  
  private void processArgs(String[] args) {
    int index = args.length;
    if (index >= 2)
      while (index >= 2) {
        String property = args[index - 2];
        String value = args[index - 1];
        if (property.equalsIgnoreCase("-locators")) {
          this.locators = value.split(",");
        } else if (property.equalsIgnoreCase("-username")) {
          this.userName = value;
        } else if (property.equalsIgnoreCase("-passwordFilename")) {
          this.passwordFilename = value;
        } else if (property.equalsIgnoreCase("-password")) {
          this.password = value;
        } else if (property.equalsIgnoreCase("-lookupgroup")) {
          this.lookupGroup = value;
        } else if (property.equalsIgnoreCase("-hostIp")) {
          this.hostIp = value;
        } else if (property.equalsIgnoreCase("-zone")) {
          this.zone = value;
        } else if (property.equalsIgnoreCase("-gscCount")) {
          this.gscCount = Integer.valueOf(value, 10);
        } else {
          System.out.println("Please enter valid arguments.");
          printUsage();
          System.exit(0);
        } 
        index -= 2;
      }  
  }
  
  public static void main(String[] args) {
    System.out.println("Latest from CSM Tools---------------------------------------------------------------->");
    RunRebalancer launcher = new RunRebalancer();
    launcher.processArgs(args);
    if (!launcher.validateArguments()) {
      logger.severe("Insufficient arguments launcher.userName, launcher.password, launcher.locators, launcher.lookupGroup provided. Please run again with required information.");
      System.out.println("Insufficient arguments provided. Please run again with required information. Please check logs for more details.");
      System.exit(-1);
    }
    final PolicyConfiguration policyConfiguration = new PolicyConfiguration();
    Policy policy = new Policy("CustomRebalancerPolicy", "space", "com.gigaspaces.odsx.noderebalancer.leumiflow.SpaceServerBalancerFlow");
    policyConfiguration.addPolicy(policy);
    PolicyAssociation policyAssociation = new PolicyAssociation(policy);
    ServerConfiguration serverConfiguration = new ServerConfiguration(launcher.hostIp, new ArrayList());
    if (launcher.zone != null && launcher.gscCount.intValue() > 0)
      serverConfiguration.addZone(launcher.zone, launcher.gscCount.intValue()); 
    policyAssociation.addSeverConfiguration(serverConfiguration);
    policyConfiguration.addPolicyAssociation(policyAssociation);

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(PolicyConfiguration.class, () -> policyConfiguration, new org.springframework.beans.factory.config.BeanDefinitionCustomizer[0]);
    //context.registerBean(PolicyConfiguration.class, () -> policyConfiguration);
    //context.registerBean(AdminConfiguration.class, new Object[] { launcher.userName, launcher.password, launcher.locators, launcher.lookupGroup });
    context.registerBean(AdminConfiguration.class,launcher.userName, launcher.password, launcher.locators, launcher.lookupGroup);
    context.registerBean(ApplicationBeans.class, new org.springframework.beans.factory.config.BeanDefinitionCustomizer[0]);
    context.refresh();

    System.out.println("Lookup Group: " + launcher.lookupGroup);
    logger.info("Starting the rebalance process, Lookup Group is: " + launcher.lookupGroup);
    final FlowEngine flowEngine = (FlowEngine)context.getBean(FlowEngine.class);
    Thread engineRunner = new Thread() {
        public void run() {
          flowEngine.run();
        }
      };
    engineRunner.run();
    context.registerShutdownHook();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
              System.out.println("Shutdown Hook Invoked");
              RunRebalancer.logger.info("Shutdown hook invoked.");
              flowEngine.initiateShutDown();
            }
          }));
  }
  
  private boolean validateArguments() {
    boolean hasRequiredArguments = true;
    if (isNullOrEmpty(this.hostIp)) {
      logger.warning(" Missing required argument : hostIp - target server IP Address.");
      hasRequiredArguments = false;
    } 
    if (isNullOrEmpty(this.locators) && isNullOrEmpty(this.lookupGroup)) {
      logger.warning(" Missing required argument : Either lookupGroup or locator must be provided.");
      hasRequiredArguments = false;
    } 
    return hasRequiredArguments;
  }
  
  private boolean isNullOrEmpty(String string) {
    return (string == null || string.length() == 0);
  }
  
  private boolean isNullOrEmpty(String[] strings) {
    return (strings == null || strings.length == 0 || strings[0].length() == 0);
  }
}
