package io.kotest.plugin.intellij.toolwindow

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.plugin.intellij.psi.callbacks
import io.kotest.plugin.intellij.psi.specStyle
import io.kotest.plugin.intellij.styles.TestElement
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

/**
 * Creates a [TreeModel] for the given [VirtualFile].
 */
fun createTreeModel(file: VirtualFile,
                    project: Project,
                    specs: List<KtClassOrObject>,
                    module: Module): TreeModel {

   fun addTests(node: DefaultMutableTreeNode,
                parent: NodeDescriptor<Any>,
                specDescriptor: SpecNodeDescriptor,
                tests: List<TestElement>) {

      val groups = tests.groupBy { it.test.name }

      tests.forEach { test ->
         val isUnique = groups[test.test.name]?.size ?: 0 < 2
         val testDescriptor = TestNodeDescriptor(project, parent, test.psi, test, specDescriptor, isUnique, module)
         val testNode = DefaultMutableTreeNode(testDescriptor)
         node.add(testNode)
         addTests(testNode, testDescriptor, specDescriptor, test.tests)
      }
   }

   val kotest = TestFileNodeDescriptor(file, project)
   val root = DefaultMutableTreeNode(kotest)
   specs.forEach { spec ->

      val fqn = spec.getKotlinFqName()
      val style = spec.specStyle()
      if (fqn != null && style != null) {

         val specDescriptor = SpecNodeDescriptor(project, kotest, spec, fqn, style, module)
         val specNode = DefaultMutableTreeNode(specDescriptor)
         root.add(specNode)

         if (!TestExplorerState.filterCallbacks) {
            val callbacks = spec.callbacks()
            callbacks.forEach {
               val callbackDescriptor = CallbackNodeDescriptor(project, specDescriptor, it.psi, it)
               val callbackNode = DefaultMutableTreeNode(callbackDescriptor)
               specNode.add(callbackNode)
            }
         }

         val tests = style.tests(spec)
         addTests(specNode, specDescriptor, specDescriptor, tests)
      }
   }

   return DefaultTreeModel(root)
}