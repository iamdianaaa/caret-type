package com.github.iamdianaaa.pycharmcarettype.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import org.jetbrains.annotations.NonNls

class CaretTypeStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): @NonNls String {
        return "CaretTypeStatusBar"
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String {
        return "Caret Type Information in Status Bat"
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return CaretTypeStatusBarWidget(project)
    }
}