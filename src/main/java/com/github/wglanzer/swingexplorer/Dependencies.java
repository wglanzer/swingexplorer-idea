package com.github.wglanzer.swingexplorer;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.plugins.*;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Dependency to external jar files
 *
 * @author w.glanzer, 12.04.2020
 */
class Dependencies
{

  private VirtualFile agentJarFile;
  private VirtualFile explorerJarFile;

  /**
   * Returns the agent jar file
   *
   * @return the agent file
   * @throws ExecutionException if file could not be found
   */
  @NotNull
  public VirtualFile getAgentFile() throws ExecutionException
  {
    _checkValid();
    return agentJarFile;
  }

  /**
   * Returns the explorer jar file
   *
   * @return the explorer file
   * @throws ExecutionException if file could not be found
   */
  @NotNull
  public VirtualFile getExplorerFile() throws ExecutionException
  {
    _checkValid();
    return explorerJarFile;
  }

  /**
   * @throws ExecutionException if this object is not valid and the dependencies could not be found
   */
  private void _checkValid() throws ExecutionException
  {
    if (agentJarFile == null || explorerJarFile == null)
      _initJarFiles();
    if (agentJarFile == null || explorerJarFile == null)
      throw new ExecutionException("SwingExplorer jars could not be found");
  }

  /**
   * Searches the dependency files in plugin directory
   */
  private void _initJarFiles()
  {
    IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId(IStaticIDs.PLUGIN_ID));
    if (descriptor != null)
    {
      File pluginPath = descriptor.getPath();
      String url = VfsUtil.pathToUrl(pluginPath.getAbsolutePath());

      VirtualFile pluginDir = VirtualFileManager.getInstance().findFileByUrl(url);
      if (pluginDir != null)
      {
        VirtualFile lib = pluginDir.findChild("lib");
        if (lib != null && lib.isDirectory())
        {
          agentJarFile = lib.findChild("swingexplorer-agent-1.7.0.jar");
          explorerJarFile = lib.findChild("swingexplorer-core-1.7.0.jar");
        }
      }
    }
  }

}
