package com.github.wglanzer.swingexplorer;

import com.intellij.execution.executors.DefaultRunExecutor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Defines a custom action to run swing explorer and connects it to our Runner
 *
 * @author w.glanzer, 25.09.2015
 * @see Runner
 */
public class Executor extends DefaultRunExecutor
{

  public static final String ID = "Run-with-SE";
  private static final ImageIcon _ICON = new ImageIcon(Executor.class.getResource("execute.png"));

  @NotNull
  @Override
  public String getId()
  {
    return ID;
  }

  @Override
  public String getContextActionId()
  {
    return Runner.ID;
  }

  @NotNull
  @Override
  public Icon getIcon()
  {
    return _ICON;
  }

}
