package org.swingexplorer.intellij;

import com.intellij.openapi.project.Project;

import javax.management.*;
import java.util.HashMap;

/**
 * Listener, um die Kommunikation des SwingExplorers mit IntelliJ möglich zu machen
 *
 * @author w.glanzer, 25.09.2015
 */
class Listener implements NotificationListener
{
  private final Project project;

  public Listener(Project project)
  {
    this.project = project;
  }

  public void handleNotification(Notification notification, Object handback)
  {
    HashMap data = (HashMap) notification.getUserData();
    String className = (String) data.get("className");
    int lineNumber = (Integer) data.get("lineNumber");

    IntelliJUtil.navigate(project, className, lineNumber);
  }

}