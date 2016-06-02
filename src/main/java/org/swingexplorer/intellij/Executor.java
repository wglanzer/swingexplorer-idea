package org.swingexplorer.intellij;

import com.intellij.execution.executors.DefaultRunExecutor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Gibt bestimmte Eigenschaften f�r den ausf�hrenden Button, den Executor, vor und beschreibt ihn.
 * Verbindet Executor und Runner
 *
 * @author w.glanzer, 25.09.2015
 * @see Runner
 */
public class Executor extends DefaultRunExecutor
{

  @NotNull
  @Override
  public String getId()
  {
    return IStaticIDs.RUNNER_ID;
  }

  @Override
  public String getContextActionId()
  {
    return IStaticIDs.EXECUTOR_CONTEXT_ACTION_ID;
  }

  @NotNull
  @Override
  public Icon getIcon()
  {
    return new ImageIcon(Executor.class.getResource("execute.png"));
  }

}
