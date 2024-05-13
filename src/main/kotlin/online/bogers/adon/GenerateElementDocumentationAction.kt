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
import com.intellij.psi.util.PsiTreeUtil

class GenerateElementDocumentationAction : AnAction() {
    // based on: https://github.com/JetBrains/intellij-community/blob/idea/241.15989.150/platform/lang-impl/src/com/intellij/refactoring/actions/RenameElementAction.java
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
        val target = dataContext.getData(CommonDataKeys.PSI_ELEMENT) ?: return

        val document = editor.document
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val psiFile = psiDocumentManager.getPsiFile(document)

        if (!psiDocumentManager.commitAllDocumentsUnderProgress()) return

        FixDocCommentAction.generateOrFixComment(target, project, editor)

//        val element = CommonRefactoringUtil.getElementAtCaret(editor, psiFile)
//        val method = element.parent as PsiMethod

//        val method = parent as PsiDocCommentOwner
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

//        documentationProvider.generateDoc()

        if (documentationProvider == null) return

//        val currentComment2 = documentationProvider.findExistingDocComment(target) ?: return


        val docContext = documentationProvider.parseContext(target) ?: return

//        val commentElement = docContext.first
        val currentComment = docContext.second ?: return

        val newCommentContent = generateComment(docLanguage) ?: return
        val newComment = factory.createCommentFromText(newCommentContent, null)

        if (!psiDocumentManager.commitAllDocumentsUnderProgress()) return

        WriteCommandAction.runWriteCommandAction(project) {
            currentComment.replace(newComment)
        }


//        val commentBuffer = StringBuffer()
//        val commentPrefix = commenter.documentationCommentPrefix
//        var commentSuffix = commenter.documentationCommentSuffix
//
//        if (commentPrefix != null) commentBuffer.append(commentPrefix).append('\n')
//
//        val commentLinePrefix = commenter.documentationCommentLinePrefix
//        if (commentLinePrefix != null) commentBuffer.append(commentLinePrefix)
//
//        commentBuffer.append("Hello world!").append('\n')
//        if (commentSuffix != null) commentBuffer.append(commentSuffix).append('\n')
//
//        document.insertString(0, commentBuffer)

//        var documentationStub = documentationProvider.generateDocumentationContentStub(currentComment)


//        val comment = factory.createCommentFromText("""
//            /**
//            * Hello world!
//            */
//        """.trimIndent(), null)



        // see: https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/codeInsight/editorActions/FixDocCommentAction.java#L82
//        val language = method.language
//        val langDocumentationProvider = LanguageDocumentation.INSTANCE.forLanguage(language)

//        DocumentationTarget

//        FixDocCommentAction.generateOrFixComment(method, project, editor);

//        JavadocGenerationManager

//        WriteCommandAction.runWriteCommandAction(project) {
//            method.docComment?.replace(comment)
//        }

//        element.docComment?.replace(comment)

        // todo: should migrate away from DocumentationProvider, obsolete!
//        fun getDocumentationProvider(language: Language): DocumentationProvider? {
//            @Suppress("MoveVariableDeclarationIntoWhen")
//            val langDocumentationProvider = LanguageDocumentation.INSTANCE.forLanguage(language)
//
//            return when (langDocumentationProvider) {
//                is CompositeDocumentationProvider -> langDocumentationProvider.firstCodeDocumentationProvider
//                is CodeDocumentationProvider -> langDocumentationProvider
//
//                else -> null
//            }
//        }
    }

    fun generateComment(lang: Language): String? {
        val commenter = LanguageCommenters.INSTANCE.forLanguage(lang)
        if (commenter !is CodeDocumentationAwareCommenter) return null

        val commentBuffer = StringBuffer()


        val commentPrefix = commenter.documentationCommentPrefix
        var commentSuffix = commenter.documentationCommentSuffix
        val commentLinePrefix = commenter.documentationCommentLinePrefix

        if (commentPrefix != null) commentBuffer.append(commentPrefix).append('\n')
        if (commentLinePrefix != null) commentBuffer.append(commentLinePrefix)
        commentBuffer.append("Hello world!").append('\n')
        if (commentSuffix != null) commentBuffer.append(commentSuffix)

        return commentBuffer.toString()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isAvailable(e.dataContext)
    }


    private fun isAvailable(dataContext: DataContext): Boolean {
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return false
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return false

//        val document = editor.document
//        val psiDocumentManager = PsiDocumentManager.getInstance(project)
//        val psiFile = psiDocumentManager.getPsiFile(document)
//        val element = CommonRefactoringUtil.getElementAtCaret(editor, psiFile)

//        if (element !is PsiMethod) return false

        return true
    }

    companion object {
        private val LOG = Logger.getInstance(
            GenerateElementDocumentationAction::class.java
        )
    }
}
