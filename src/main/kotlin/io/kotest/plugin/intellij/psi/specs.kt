package io.kotest.plugin.intellij.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.kotest.plugin.intellij.styles.SpecStyle
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import kotlin.time.ExperimentalTime

/**
 * Returns any [KtClassOrObject]s located in this [PsiElement]
 */
fun PsiElement.classes(): List<KtClassOrObject> {
   return this.getChildrenOfType<KtClassOrObject>().asList()
}

/**
 * Returns any [KtClassOrObject] children of this [PsiFile] that are specs.
 */
fun PsiFile.specs(): List<KtClassOrObject> {
   return this.classes().filter { it.isSubclassOfSpec() }
}

/**
 * Returns true if this [KtClassOrObject] is a subclass of any Spec.
 * This method will not recursively check parents, and relies only on the simple name.
 */
@OptIn(ExperimentalTime::class)
fun KtClassOrObject.isSubclassOfSpec(): Boolean = this.specStyle() != null

fun KtClassOrObject.isSpecSubclass(fqn: FqName): Boolean {
   return when (val simpleName = getSuperClassSimpleName()) {
      null -> false
      else -> fqn.shortName().asString() == simpleName
   }
}

/**
 * Efficiently locates the spec style this class is from, or null if it's not a spec.
 */
fun KtClassOrObject.specStyle(): SpecStyle? {
   return when (val simpleName = getSuperClassSimpleName()) {
      null -> null
      else -> SpecStyle.styles.find { it.fqn().shortName().asString() == simpleName }
   }
}

fun KtCallExpression.isDslInvocation(): Boolean {
   return children.size == 2
      && children[0] is KtNameReferenceExpression
      && children[1] is KtLambdaArgument
}

/**
 * Returns any test lifecycle callbacks defined in this class.
 */
fun KtClassOrObject.callbacks(): List<Callback> {

   val body = this.getChildrenOfType<KtClassBody>().firstOrNull()
   if (body != null) return body.callbacks()

   val superlist = this.getChildrenOfType<KtSuperTypeList>().firstOrNull()
   if (superlist != null) return superlist.callbacks()

   return emptyList()
}

fun KtClassBody.callbacks(): List<Callback> {
   val init = getChildrenOfType<KtClassInitializer>().firstOrNull()
   if (init != null) {
      val block = init.getChildrenOfType<KtBlockExpression>().firstOrNull()
      if (block != null) {
         return block.callbacks()
      }
   }
   return emptyList()
}

fun KtSuperTypeList.callbacks(): List<Callback> {
   val entry = getChildrenOfType<KtSuperTypeCallEntry>().firstOrNull()
   if (entry != null) {
      val argList = entry.getChildrenOfType<KtValueArgumentList>().firstOrNull()
      if (argList != null) {
         val valueArg = argList.getChildrenOfType<KtValueArgument>().firstOrNull()
         if (valueArg != null) {
            val lambda = valueArg.getChildrenOfType<KtLambdaExpression>().firstOrNull()
            if (lambda != null) {
               val fliteral = lambda.getChildrenOfType<KtFunctionLiteral>().firstOrNull()
               if (fliteral != null) {
                  val block = fliteral.getChildrenOfType<KtBlockExpression>().firstOrNull()
                  if (block != null) {
                     return block.callbacks()
                  }
               }
            }

         }
      }
   }
   return emptyList()
}

/**
 * If this call expression is an include(factory) or include(factory()) then will
 * return an [Include] describing that.
 *
 * Otherwise returns null.
 */
fun KtCallExpression.include(): Include? {
   if (children.isNotEmpty() &&
      children[0] is KtNameReferenceExpression &&
      children[0].text == "include") {
      val args = valueArgumentList
      if (args != null) {
         val maybeKtValueArgument = args.arguments.firstOrNull()
         if (maybeKtValueArgument is KtValueArgument) {
            when (val param = maybeKtValueArgument.children.firstOrNull()) {
               is KtCallExpression -> {
                  val name = param.children[0].text
                  return Include(name, IncludeType.Function, param)
               }
               is KtNameReferenceExpression -> {
                  val name = param.text
                  return Include(name, IncludeType.Value, param)
               }
            }
         }
      }
   }
   return null
}

/**
 * Returns any include operations defined in this class.
 */
fun KtClassOrObject.includes(): List<Include> {

   val body = this.getChildrenOfType<KtClassBody>().firstOrNull()
   if (body != null) return body.includes()

   val superlist = this.getChildrenOfType<KtSuperTypeList>().firstOrNull()
   if (superlist != null) return superlist.includes()

   return emptyList()
}

fun KtClassBody.includes(): List<Include> {
   val init = getChildrenOfType<KtClassInitializer>().firstOrNull()
   if (init != null) {
      val block = init.getChildrenOfType<KtBlockExpression>().firstOrNull()
      if (block != null) {
         return block.includes()
      }
   }
   return emptyList()
}

fun KtSuperTypeList.includes(): List<Include> {
   val entry = getChildrenOfType<KtSuperTypeCallEntry>().firstOrNull()
   if (entry != null) {
      val argList = entry.getChildrenOfType<KtValueArgumentList>().firstOrNull()
      if (argList != null) {
         val valueArg = argList.getChildrenOfType<KtValueArgument>().firstOrNull()
         if (valueArg != null) {
            val lambda = valueArg.getChildrenOfType<KtLambdaExpression>().firstOrNull()
            if (lambda != null) {
               val fliteral = lambda.getChildrenOfType<KtFunctionLiteral>().firstOrNull()
               if (fliteral != null) {
                  val block = fliteral.getChildrenOfType<KtBlockExpression>().firstOrNull()
                  if (block != null) {
                     return block.includes()
                  }
               }
            }

         }
      }
   }
   return emptyList()
}

fun KtBlockExpression.includes(): List<Include> {
   val calls = getChildrenOfType<KtCallExpression>()
   return calls.mapNotNull { it.include() }
}


fun KtBlockExpression.callbacks(): List<Callback> {
   val calls = getChildrenOfType<KtCallExpression>()
   return calls
      .filter { it.isDslInvocation() }
      .mapNotNull { call ->
         val fname = call.functionName()
         CallbackType.values().find { it.text == fname }?.let { Callback(it, call) }
      }
}

enum class IncludeType { Value, Function }

data class Include(val name: String, val type: IncludeType, val psi: PsiElement)

data class Callback(val type: CallbackType, val psi: PsiElement)

enum class CallbackType {

   BeforeTest {
      override val text = "beforeTest"
   },
   AfterTest {
      override val text = "afterTest"
   },
   BeforeSpec {
      override val text = "beforeSpec"
   },
   AfterSpec {
      override val text = "afterSpec"
   };

   abstract val text: String
}

/**
 * Returns true if this [PsiElement] is contained within a class that is a subclass
 * of the given spec FQN
 */
fun PsiElement.isContainedInSpec(fqn: FqName): Boolean {
   val enclosingClass = getParentOfType<KtClassOrObject>(true) ?: return false
   return enclosingClass.isSpecSubclass(fqn)
}

/**
 * Returns true if this [PsiElement] is inside any spec class.
 */
fun PsiElement.isContainedInSpec(): Boolean {
   val enclosingClass = getParentOfType<KtClassOrObject>(true) ?: return false
   return enclosingClass.isSubclassOfSpec()
}

