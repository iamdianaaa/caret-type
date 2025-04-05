package com.github.iamdianaaa.carettype.statusbar

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.awt.Component
import com.intellij.openapi.application.ApplicationManager


class CaretTypeStatusBarWidget(private val project: Project?) : StatusBarWidget {
    private var textType = ""
    private var statusBar: StatusBar? = null
    private val caretListener: CaretListener


    init {
        this.caretListener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                updateType(event.editor)
            }
        }

        EditorFactory.getInstance().eventMulticaster.addCaretListener(caretListener, this)
    }


    private fun updateType(editor: Editor?) {
        if (editor == null || project == null) {
            textType = ""
            statusBar?.updateWidget(ID())
            return
        }


        ApplicationManager.getApplication().runReadAction {
            val psiFile = PsiManager.getInstance(project).findFile(editor.virtualFile) ?: return@runReadAction
            val caretPosition = editor.caretModel.offset
            val element = psiFile.findElementAt(caretPosition) ?: return@runReadAction

            textType = ""
            val variable = findReference(editor, element)

            if (variable != null) {
                textType = getPythonType(variable)
            }

            statusBar?.updateWidget(ID())
        }
    }


    private fun getPythonType(variable: PsiElement): String {
        val file = variable.containingFile
        val varName = variable.text

        if (varName.isNullOrEmpty()) return ""

        val fileText = file.text
        val varPosition = fileText.indexOf("$varName = ")

        if (varPosition >= 0) {
            val startOfAssignment = varPosition + varName.length + 3

            if (startOfAssignment < fileText.length) {
                val next = fileText[startOfAssignment]

                if (next == '"' || next == '\n') {
                    return "str"
                } else if (Character.isDigit(next)) {
                    if (fileText.substring(startOfAssignment).contains(".")) return "float"
                    return "int"
                } else if (fileText.substring(startOfAssignment).startsWith("True") || fileText.substring(
                        startOfAssignment
                    ).startsWith("False")
                ) {
                    return "bool"
                } else if (next == '[') {
                    return "list"
                } else if (next == '{') {
                    return "dict"
                } else if (next == '(') {
                    return "tuple"
                } else if (fileText.substring(startOfAssignment).startsWith("None")) {
                    return "None"
                }
            } else {
                return ""
            }
        }

        return "unknown"
    }

    private fun findReference(editor: Editor, element: PsiElement): PsiElement? {
        if (isPythonVariable(element)) return element

        val caretOffset = editor.caretModel.offset

        var parent = element.parent
        while (parent != null) {
            if (isPythonVariable(parent)) {
                val varRange = parent.textRange
                if (varRange.startOffset <= caretOffset && caretOffset <= varRange.endOffset) {
                    return parent
                }
            }

            parent = parent.parent
        }

        return null
    }

    private fun isPythonVariable(element: PsiElement): Boolean {
        val text = element.text ?: return false

        if (text.isEmpty()) return false

        val checkSpace = text.contains(" ") // variable cannot contain spaces
        val checkFirstLetter = Character.isLetter(text[0]) // variable cannot start with a number
        val checkCommonKeywords = isPythonCommonKeyword(text)

        return checkFirstLetter && !checkSpace && !checkCommonKeywords
    }

    private fun isPythonCommonKeyword(text: String?): Boolean {
        val commonKeywords = arrayOf(
            "if", "else", "elif", "for", "while", "def", "class", "is", "True", "False",
            "return", "import", "from", "as", "try", "except", "finally"
        )

        for (keyword in commonKeywords) {
            if (keyword == text) return true
        }
        return false
    }


    override fun ID(): String {
        return "CaretTypeStatusBar"
    }

    override fun dispose() {
        EditorFactory.getInstance().eventMulticaster.removeCaretListener(caretListener)
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return object : StatusBarWidget.TextPresentation {
            override fun getText(): String {
                return textType
            }

            override fun getAlignment(): Float {
                return Component.CENTER_ALIGNMENT
            }

            override fun getTooltipText(): String {
                return "Detected variable type at caret position"
            }
        }
    }

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar

        val editor = FileEditorManager.getInstance(project!!).selectedTextEditor
        editor?.let { updateType(it) }
    }
}