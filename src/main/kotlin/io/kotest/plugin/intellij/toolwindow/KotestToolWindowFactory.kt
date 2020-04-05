package io.kotest.plugin.intellij.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class KotestToolWindowFactory : ToolWindowFactory {

   override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
      val explorer = TestExplorerWindow(toolWindow, project)
      val contentFactory = ContentFactory.SERVICE.getInstance()
      val content = contentFactory.createContent(explorer, "", false)
      toolWindow.contentManager.addContent(content)
   }
}
