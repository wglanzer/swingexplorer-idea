package com.github.wglanzer.swingexplorer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.*;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfileState;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Gets called if the executor wants to execute a java program with swing explorer integration
 *
 * @author w.glanzer, 25.09.2015
 */
public class Runner extends DefaultJavaProgramRunner
{

  public static final String ID = "RunWithSEClass";

  @NotNull
  @Override
  public String getRunnerId()
  {
    return ID;
  }

  @Override
  public boolean canRun(@NotNull String pExecutorID, @NotNull RunProfile pProfile)
  {
    return pExecutorID.equals(Executor.ID) && (pProfile instanceof ApplicationConfiguration);
  }

  @NotNull
  @Override
  protected Promise<RunContentDescriptor> doExecuteAsync(@NotNull TargetEnvironmentAwareRunProfileState pState,
                                                         @NotNull ExecutionEnvironment pEnv) throws ExecutionException
  {
    Project project = RunContentBuilder.fix(pEnv, this).getProject();
    if (!(pState instanceof JavaCommandLineState))
      return super.doExecuteAsync(pState, pEnv);
    JavaCommandLineState state = (JavaCommandLineState) pState;
    int port = _getFreePort();
    Dependencies dependencies = new Dependencies();

    // 1) Save Project
    FileDocumentManager.getInstance().saveAllDocuments();

    // 2) Expand Settings with SwingExplorer ones
    _initJavaSettings(state, dependencies, port);

    // 3) Run and init listener
    Promise<RunContentDescriptor> descr = super.doExecuteAsync(pState, pEnv);
    descr.onSuccess(pDescr -> NotificationListenerImpl.create(project, port));
    return descr;
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState pState, @NotNull ExecutionEnvironment pEnv) throws ExecutionException
  {
    Project project = RunContentBuilder.fix(pEnv, this).getProject();
    if (!(pState instanceof JavaCommandLineState))
      return super.doExecute(pState, pEnv);
    JavaCommandLineState state = (JavaCommandLineState) pState;
    int port = _getFreePort();
    Dependencies dependencies = new Dependencies();

    // 1) Save Project
    FileDocumentManager.getInstance().saveAllDocuments();

    // 2) Expand Settings with SwingExplorer ones
    _initJavaSettings(state, dependencies, port);

    // 3) Run and init listener
    RunContentDescriptor descr = super.doExecute(pState, pEnv);
    if (descr != null)
      NotificationListenerImpl.create(project, port);

    return descr;
  }

  /**
   * Initializes the necessary java options in the commandLineState
   *
   * @param pProfileState State to modify
   * @param pDependencies Information about the necessary .jar files for SwingExplorer
   * @param pPort         port to bind communication
   */
  private void _initJavaSettings(@NotNull JavaCommandLineState pProfileState, @NotNull Dependencies pDependencies, int pPort) throws ExecutionException
  {
    pProfileState.getJavaParameters().getClassPath().add(pDependencies.getAgentFile());
    pProfileState.getJavaParameters().getClassPath().add(pDependencies.getExplorerFile());

    // Add VM Parameters
    JavaParameters javaParameters = pProfileState.getJavaParameters();
    ParametersList vmParametersList = javaParameters.getVMParametersList();
    vmParametersList.add("-javaagent:" + pDependencies.getAgentFile().getPath());
    vmParametersList.add("-Xbootclasspath/a:" + pDependencies.getAgentFile().getPath());
    vmParametersList.add("-Dswex.mport=" + pPort);
    vmParametersList.add("-Dcom.sun.management.jmxremote");

    // Replace main class with swingexplorer and add real main class as an argument
    String mainClass = javaParameters.getMainClass();
    javaParameters.setMainClass("org.swingexplorer.Launcher");
    javaParameters.getProgramParametersList().addAt(0, mainClass);
  }

  /**
   * @return a free, random port
   */
  private int _getFreePort() throws ExecutionException
  {
    try (ServerSocket serverSocket = new ServerSocket(0))
    {
      return serverSocket.getLocalPort();
    }
    catch (IOException e)
    {
      throw new ExecutionException("Could not open port!");
    }
  }

}
