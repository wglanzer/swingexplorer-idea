package org.swingexplorer.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.*;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.plugins.*;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.*;

import javax.management.*;
import javax.management.remote.*;
import java.io.*;
import java.net.ServerSocket;

/**
 * Wird aufgerufen, wenn das Plugin ausgeführt werden soll
 *
 * @author w.glanzer, 25.09.2015
 */
public class Runner extends DefaultJavaProgramRunner
{
  private VirtualFile swagJarFile;
  private VirtualFile swexplJarFile;
  private Project project;
  private int port;

  @NotNull
  @Override
  public String getRunnerId()
  {
    return IStaticIDs.EXECUTOR_CONTEXT_ACTION_ID;
  }

  @Nullable
  public static VirtualFile getPluginVirtualDirectory()
  {
    IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId(IStaticIDs.PLUGIN_ID));
    if (descriptor != null)
    {
      File pluginPath = descriptor.getPath();
      String url = VfsUtil.pathToUrl(pluginPath.getAbsolutePath());

      return VirtualFileManager.getInstance().findFileByUrl(url);
    }

    return null;
  }

  @Override
  public boolean canRun(@NotNull String s, @NotNull RunProfile runProfile)
  {
    return s.equals(IStaticIDs.RUNNER_ID) && (runProfile instanceof ApplicationConfiguration);
  }

  @Override
  public void execute(@NotNull ExecutionEnvironment environment, @Nullable Callback callback, @NotNull RunProfileState state) throws ExecutionException
  {
    ExecutionEnvironment fixedEnv = RunContentBuilder.fix(environment, this);

    // IntelliJ-Projekt holen und alle offenen Dokumente speichern
    project = fixedEnv.getProject();
    FileDocumentManager.getInstance().saveAllDocuments();

    // Java-Settings initialisieren
    _initJavaSettings(state);

    // Ausführen
    super.execute(environment, new Callback()
    {
      @Override
      public void processStarted(RunContentDescriptor pRunContentDescriptor)
      {
        // Listener initialisieren, damit Benachrichtigungen vom SwingExplorer ankommen
        _initListener();
      }
    }, state);
  }

  /**
   * Initialisiert den Classpath, die MainRoutine und VMOptions für Java in Verbindung mit dem SwingExplorer
   *
   * @param pProfileState Profile-State
   * @throws ExecutionException
   */
  private void _initJavaSettings(RunProfileState pProfileState) throws ExecutionException
  {
    if (pProfileState instanceof ApplicationConfiguration.JavaApplicationCommandLineState)
    {
      ApplicationConfiguration.JavaApplicationCommandLineState profileState = (ApplicationConfiguration.JavaApplicationCommandLineState) pProfileState;

      _initJarFiles();
      _initPort();
      _appendSwingExplorerJarsToClassPath(profileState);

      // VMParameter hinzufügen
      JavaParameters javaParameters = profileState.getJavaParameters();
      ParametersList vmParametersList = javaParameters.getVMParametersList();
      vmParametersList.add("-javaagent:" + swagJarFile.getPath());
      vmParametersList.add("-Xbootclasspath/a:" + swagJarFile.getPath());
      vmParametersList.add("-Dswex.mport=" + port);
      vmParametersList.add("-Dcom.sun.management.jmxremote");

      // Main-Klasse austauschen gegen die des SwingExplorers. Der SE zieht die andere Main-Klasse selbst hoch!
      String mainClass = javaParameters.getMainClass();
      javaParameters.setMainClass("org.swingexplorer.Launcher");
      javaParameters.getProgramParametersList().addAt(0, mainClass);
    }
  }

  /**
   * Holt einen RANDOM-Port für die Verwendung im SwingExplorer heran
   *
   * @throws ExecutionException Wenn kein Port gefunden wurde
   */
  private void _initPort() throws ExecutionException
  {
    try
    {
      ServerSocket serverSocket = new ServerSocket(0);
      port = serverSocket.getLocalPort();
      serverSocket.close();
    }
    catch (IOException e)
    {
      throw new ExecutionException("Could not open port!");
    }
  }

  /**
   * Initialisiert den Listener, wenn im SwingExplorer etwas passiert.
   * Allerdings in einem neuen Thread, weil sich sonst IntelliJ aufhängen kann
   */
  private void _initListener()
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + port + "/server");
          JMXConnector jmxc = _connectToSwingExplorer(url);
          MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

          ObjectName name = new ObjectName("org.swingexplorer:name=IDESupport");
          mbsc.invoke(name, "connect", new Object[0], new String[0]);

          Listener listener = new Listener(project);
          mbsc.addNotificationListener(name, listener, null, null);

          IntelliJUtil.notifiy(NotificationType.INFORMATION, "Connection successfully established!");
        }
        catch (Exception e)
        {
          IntelliJUtil.notifiy(NotificationType.ERROR, "Connection could not be established! (" + e.getMessage() + ")");
        }
      }
    }, "tConnectionThread").start();
  }

  /**
   * Verbindet sich zum schon laufenden SwingExplorer
   *
   * @param pURL URL, auf die sich verbunden werden soll
   * @return JMX-Verbindung
   * @throws Exception Wenn sich entweder nicht zum SwingExplorer verbunden werden kann, oder der Thread.sleep nicht ausgeführt werden konnte
   */
  private JMXConnector _connectToSwingExplorer(JMXServiceURL pURL) throws Exception
  {
    for (int retries = 0; retries < 10; retries++)
    {
      try
      {
        return JMXConnectorFactory.connect(pURL, null);
      }
      catch (IOException e)
      {
        // Einfach nochmal probieren
        Thread.sleep(500);
      }
    }

    throw new ExecutionException("Could not connect to SwingExplorer!");
  }

  /**
   * Fügt die JAR-Dateien zum Classpath des auszuführenden Programms hinzu
   *
   * @param profileState Profil, bei dem es hinzugefügt werden soll
   * @throws ExecutionException Wenn die JavaParameter nicht geladen werden konnten
   */
  private void _appendSwingExplorerJarsToClassPath(ApplicationConfiguration.JavaApplicationCommandLineState profileState) throws ExecutionException
  {
    PathsList classPath = profileState.getJavaParameters().getClassPath();

    classPath.add(swagJarFile);
    classPath.add(swexplJarFile);
  }

  /**
   * Sucht die swag.jar und swexpl.jar und speichert diese in den beiden VirtualFile-Variablen
   *
   * @throws ExecutionException Wenn die JARs nicht gefunden wurden
   */
  private void _initJarFiles() throws ExecutionException
  {
    VirtualFile pluginDir = getPluginVirtualDirectory();
    boolean ok = false;

    if (pluginDir != null)
    {
      VirtualFile lib = pluginDir.findChild("lib");
      if (lib != null && lib.isDirectory())
      {
        VirtualFile libraryDirectory = lib.findChild("org/swingexplorer/intellij/libs");
        if (libraryDirectory != null && libraryDirectory.isDirectory())
        {
          swagJarFile = libraryDirectory.findChild("swag.jar");
          swexplJarFile = libraryDirectory.findChild("swexpl.jar");

          ok = swagJarFile != null && swexplJarFile != null;
        }
      }
    }

    if (!ok)
      throw new ExecutionException("SwingExplorer jars could not be found! " + pluginDir);
  }

}
