package com.example.testtaskplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.vcsUtil.VcsUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import git4idea.history.GitFileHistory


class StatisticsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: run {
            Messages.showMessageDialog(project, "Psi File not found", "Statistics", null)
            return
        }
        val infoBuilder = StringBuilder()

        infoBuilder.append("Number of methods in the opened file: ")
            .append(numberMethods(psiFile).toString()).append("\n")

        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        editor?.let {
            infoBuilder.append("Number of variables defined in the selected method: ")
                .append(numberVars(psiFile, editor).toString()).append("\n")
        }

        val last = lastModified(psiFile, project)
        last?.let {
            infoBuilder
                .append("The last person that modified this file: ").append(last.author ?: "").append("\n")
                .append("The date of this modification : ").append(last.revisionDate).append("\n")
        }

        Messages.showMessageDialog(project, infoBuilder.toString(), "Statistics", null)
    }

    private fun numberMethods(psiFile: PsiFile) : Int {
        var count = 0
        psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is KtNamedFunction || element is PsiMethod) count++
                super.visitElement(element)
            }
        })
        return count
    }

    private fun numberVars(psiFile: PsiFile, editor: Editor) : Int {
        val offset = editor.caretModel.offset
        val currentPsiElement = psiFile.findElementAt(offset)
        val currentPsiFunction = PsiTreeUtil.findFirstParent(currentPsiElement) {
            it is KtNamedFunction || it is PsiMethod
        }
        var countVars = 0
        currentPsiFunction?.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is KtProperty || element is PsiDeclarationStatement) countVars++
                super.visitElement(element)
            }
        })
        return countVars
    }

    private fun lastModified(psiFile: PsiFile, project: Project) : VcsFileRevision? {
        return try {
            val vFile = psiFile.originalFile.virtualFile
            val history = GitFileHistory.collectHistory(project, VcsUtil.getFilePath(vFile))
            history.lastOrNull()
        } catch (_ : VcsException) {
            null
        }
    }
}
