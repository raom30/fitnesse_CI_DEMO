package fitnesse.testsystems.slim;

import fitnesse.slim.*;
import fitnesse.testsystems.*;
import fitnesse.wiki.ReadOnlyPageData;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

public class SlimClientBuilder extends ClientBuilder<SlimCommandRunningClient> {

  public static final String SLIM_PORT = "SLIM_PORT";
  public static final String SLIM_HOST = "SLIM_HOST";
  public static final String SLIM_FLAGS = "SLIM_FLAGS";

  private static final AtomicInteger slimPortOffset = new AtomicInteger(0);

  private final int slimPort;

  public SlimClientBuilder(Descriptor descriptor) {
    super(descriptor);
    slimPort = getNextSlimPort();
  }

  public static String defaultTestRunner() {
    return "fitnesse.slim.SlimService";
  }

  @Override
  public SlimCommandRunningClient build() throws IOException {
    CommandRunner commandRunner;

    if (fastTest) {
      commandRunner = new MockCommandRunner();
      final String slimArguments = buildArguments();
      createSlimService(slimArguments);
    }
    else if (manualStart) {
      commandRunner = new MockCommandRunner();
    } else {
      commandRunner = new CommandRunner(buildCommand(), "", descriptor.createClasspathEnvironment(descriptor.getClassPath()));
    }

    return new SlimCommandRunningClient(descriptor.getTestRunner(), commandRunner, determineSlimHost(), getSlimPort());
  }

  public String buildCommand() {
    String slimArguments = buildArguments();
    String slimCommandPrefix = super.buildCommand(descriptor.getCommandPattern(), descriptor.getTestRunner(), descriptor.getClassPath());
    return String.format("%s %s", slimCommandPrefix, slimArguments);
  }

  private String buildArguments() {
    int slimSocket = getSlimPort();
    String slimFlags = getSlimFlags();
    return String.format("%s %d", slimFlags, slimSocket);
  }

  //For testing only.  Makes responder faster.
  void createSlimService(String args) throws SocketException {
    while (!tryCreateSlimService(args))
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
  }

  // For testing only
  private boolean tryCreateSlimService(String args) throws SocketException {
    try {
      SlimService.parseCommandLine(args.trim().split(" "));
      SlimService.startWithFactoryAsync(new JavaSlimFactory());
      return true;
    } catch (SocketException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public int getSlimPort() {
    return slimPort;
  }

  private int findFreePort() {
    int port;
    try {
      ServerSocket socket = new ServerSocket(0);
      port = socket.getLocalPort();
      socket.close();
    } catch (Exception e) {
      port = -1;
    }
    return port;
  }

  private int getNextSlimPort() {
    int base;

    if (System.getProperty("slim.port") != null) {
      base = Integer.parseInt(System.getProperty("slim.port"));
    } else {
      base = getSlimPortBase();
    }

    if (base == 0) {
      return findFreePort();
    }

    synchronized (slimPortOffset) {
      int offset = slimPortOffset.get();
      offset = (offset + 1) % 10;
      slimPortOffset.set(offset);
      return offset + base;
    }
  }

  public static void clearSlimPortOffset() {
    slimPortOffset.set(0);
  }

  private int getSlimPortBase() {
    int base = 8085;
    try {
      String slimPort = descriptor.getVariable(SLIM_PORT);
      if (slimPort != null) {
        int slimPortInt = Integer.parseInt(slimPort);
        base = slimPortInt;
      }
    } catch (NumberFormatException e) {
      // stick with default
    }
    return base;
  }

  String determineSlimHost() {
    String slimHost = descriptor.getVariable(SLIM_HOST);
    return slimHost == null ? "localhost" : slimHost;
  }

  String getSlimFlags() {
    String slimFlags = descriptor.getVariable(SLIM_FLAGS);
    return slimFlags == null ? "" : slimFlags;
  }

}
