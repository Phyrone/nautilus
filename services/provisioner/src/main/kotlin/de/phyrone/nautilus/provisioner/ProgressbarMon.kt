package de.phyrone.nautilus.provisioner

import me.tongfei.progressbar.InteractiveConsoleProgressBarConsumer
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.Closeable

class ProgressbarMon(
    private val title: String,
) : ProgressMonitor, Closeable {
    private val progressBars = mutableMapOf<String, ProgressBar>()

    private var currentTask: ProgressBar? = null
    override fun start(totalTasks: Int) {

    }

    override fun beginTask(title: String, totalWork: Int) {
        currentTask = progressBars.getOrPut(title) {
            ProgressBarBuilder()
                .setTaskName(this.title)
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .setInitialMax(totalWork.toLong())
                .continuousUpdate()
                .setUpdateIntervalMillis(100)
                .showSpeed()
                .setConsumer(InteractiveConsoleProgressBarConsumer(System.out))
                .build().also {
                    it.setExtraMessage(" $title")
                }
        }
    }

    override fun update(completed: Int) {
        currentTask?.stepBy(completed.toLong())
    }

    override fun endTask() {
        currentTask = null

    }

    override fun isCancelled(): Boolean {
        return false
    }

    override fun showDuration(enabled: Boolean) {}

    override fun close() {
        progressBars.values.forEach {
            it.close()
        }
    }

}