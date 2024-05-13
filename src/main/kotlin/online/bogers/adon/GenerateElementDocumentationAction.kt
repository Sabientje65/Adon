package online.bogers.adon

import com.intellij.codeInsight.editorActions.FixDocCommentAction
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.LanguageDocumentation
import com.intellij.lang.documentation.CodeDocumentationProvider
import com.intellij.lang.documentation.CompositeDocumentationProvider
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*

class GenerateElementDocumentationAction : AnAction() {
    // based on:
    // * https://github.com/JetBrains/intellij-community/blob/idea/241.15989.150/platform/lang-impl/src/com/intellij/refactoring/actions/RenameElementAction.java
    // * https://github.com/JetBrains/intellij-community/blob/master/java/java-impl/src/com/intellij/codeInsight/intention/impl/AddJavadocIntention.java

    init {
        setInjectedContext(true)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext

        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return

        // element we're placing our documentation on, needs to support documentation though!
        val target = dataContext.getData(CommonDataKeys.PSI_ELEMENT) ?: return

        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        if (!psiDocumentManager.commitAllDocumentsUnderProgress()) return

        val factory = JavaPsiFacade.getElementFactory(project)
        val docLanguage = target.language

        val documentationProvider = when(
            val langDocumentationProvider = LanguageDocumentation.INSTANCE.forLanguage(docLanguage)
        ) {
            is CompositeDocumentationProvider -> langDocumentationProvider.firstCodeDocumentationProvider
            is CodeDocumentationProvider -> langDocumentationProvider
            else -> null
        }

        val commenter = LanguageCommenters.INSTANCE.forLanguage(docLanguage) ?: return

        if (commenter !is CodeDocumentationAwareCommenter) return
        if (documentationProvider == null) return

        var docContext = documentationProvider.parseContext(target) ?: return

        // Ensure we've got a comment stub if none was present
        if (docContext.second == null) {
            LOG.debug("No docstring found on element, generating stub!")

            FixDocCommentAction.generateOrFixComment(target, project, editor)
            docContext = documentationProvider.parseContext(target) ?: return
        }

        val currentComment = docContext.second ?: return

        LOG.debug("Generating new docstring!")
        val newCommentContent = generateDocString(docLanguage) ?: return
        val newComment = factory.createCommentFromText(newCommentContent, null)

        // We aren't allowed to modify our document with pending changes present
        if (!psiDocumentManager.commitAllDocumentsUnderProgress()) return

        WriteCommandAction.runWriteCommandAction(project) {
            currentComment.replace(newComment)
        }
    }

    /**
     * Generate a docstring in the provided language
     */
    private fun generateDocString(lang: Language): String? {
        val commenter = LanguageCommenters.INSTANCE.forLanguage(lang)
        if (commenter !is CodeDocumentationAwareCommenter) return null

        val commentBuffer = StringBuffer()

        val commentPrefix = commenter.documentationCommentPrefix
        val commentSuffix = commenter.documentationCommentSuffix
        val commentLinePrefix = commenter.documentationCommentLinePrefix

        if (commentPrefix != null) commentBuffer.append(commentPrefix).append('\n')
        if (commentLinePrefix != null) commentBuffer.append(commentLinePrefix)

        // Start off with a simple hello world, just get something to render on screen
        commentBuffer.append(" Hello world!").append('\n')
        if (commentSuffix != null) commentBuffer.append(commentSuffix)

        return commentBuffer.toString()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isAvailable(e.dataContext)
    }


    private fun isAvailable(dataContext: DataContext): Boolean {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return false
        dataContext.getData(CommonDataKeys.EDITOR) ?: return false

        return true
    }

    companion object {
        private val LOG = Logger.getInstance(
            GenerateElementDocumentationAction::class.java
        )
    }
}
