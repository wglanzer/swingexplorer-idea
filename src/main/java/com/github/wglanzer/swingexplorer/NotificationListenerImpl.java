package com.github.wglanzer.swingexplorer;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.*;
import javax.management.remote.*;
import java.io.IOException;
import java.util.Map;

/**
 * Listener that gets called, if an action happened in swingexplorer frame (something like "navigate to")
 *
 * @author w.glanzer, 25.09.2015
 */
class NotificationListenerImpl implements Runnable, NotificationListener
{
  private final Project project;
  private final int port;

  /**
   * Creates a new NotificationListener and connects it automatically to the running instance
   *
   * @param pProject Current Project
   * @param pPort    Port to connect to
   */
  public static void create(@NotNull Project pProject, int pPort)
  {
    new Thread(new NotificationListenerImpl(pProject, pPort), "tSwingExplorerConnectionThread").start();
  }

  private NotificationListenerImpl(@NotNull Project pProject, int pPort)
  {
    project = pProject;
    port = pPort;
  }

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
      mbsc.addNotificationListener(name, this, null, null);
    }
    catch (Exception e)
    {
      throw new RuntimeException("failed to connect to running instance", e);
    }
  }

  @Override
  public void handleNotification(Notification notification, Object handback)
  {
    //noinspection unchecked
    Map<Object, Object> data = (Map<Object, Object>) notification.getUserData();
    String className = (String) data.get("className");
    int lineNumber = (Integer) data.get("lineNumber");

    if (className != null)
      _navigate(project, className, lineNumber);
  }

  /**
   * Connects to a running instance of Swing Explorer
   *
   * @param pURL URL to connect to
   * @return the connected Connector, not null
   * @throws Exception if unable to connect
   */
  @NotNull
  private JMXConnector _connectToSwingExplorer(@NotNull JMXServiceURL pURL) throws Exception
  {
    Exception ex = null;
    for (int retries = 0; retries < 10; retries++)
    {
      try
      {
        return JMXConnectorFactory.connect(pURL, null);
      }
      catch (IOException e)
      {
        ex = e;

        // Retry
        Thread.sleep(500);
      }
    }

    throw new ExecutionException("Could not connect to SwingExplorer", ex);
  }

  /**
   * Navigates to a location in the given class
   *
   * @param pProject    Project to search in
   * @param pClass      FQN of a class to search in
   * @param pLineNumber line number to scroll to
   */
  private void _navigate(@NotNull Project pProject, @NotNull String pClass, int pLineNumber)
  {
    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runReadAction(() -> {
      if (!pProject.isInitialized())
        return;

      PsiClass psiClass = JavaPsiFacade.getInstance(pProject).findClass(pClass, GlobalSearchScope.allScope(pProject));

      if (psiClass != null)
      {
        FileEditorProviderManager editorProviderManager = FileEditorProviderManager.getInstance();
        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null || editorProviderManager.getProviders(pProject, virtualFile).length == 0)
          return;

        Editor editor = FileEditorManager.getInstance(pProject).openTextEditor(new OpenFileDescriptor(pProject, virtualFile), true);
        if (editor != null)
        {
          if (pLineNumber > editor.getDocument().getLineCount())
            return;

          CaretModel caretModel = editor.getCaretModel();
          LogicalPosition pos = new LogicalPosition(pLineNumber - 1, 0);
          caretModel.moveToLogicalPosition(pos);
          ApplicationManager.getApplication().invokeLater(() -> editor.getScrollingModel().scrollToCaret(ScrollType.CENTER));
        }

        ProjectUtil.focusProjectWindow(pProject, true);
      }
      else
      {
        NotificationGroup.balloonGroup(IStaticIDs.NOTIFICATION_ID_WINDOW)
            .createNotification("Swing Explorer", "Class not found: " + pClass, NotificationType.ERROR, null)
            .notify(pProject);
      }
    }));
  }
}