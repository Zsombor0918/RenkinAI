package dev.renkinProject.renkin.testing

import android.app.Application
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class WindowsAtomicFileTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun commit(atomic: AtomicFile, value: String) {
        val output = atomic.startWrite()
        output.write(value.toByteArray())
        atomic.finishWrite(output)
    }

    @Test fun secondCommitAtomicallyReplacesThePriorBase() {
        val base = File(temporary.root, "record")
        val atomic = AtomicFile(base)
        commit(atomic, "first")
        commit(atomic, "second")
        assertEquals("second", atomic.openRead().use { it.reader().readText() })
        assertFalse(File(base.path + ".new").exists())
    }

    @Test fun failedAndInterruptedWritesPreserveTheCommittedBase() {
        val base = File(temporary.root, "record")
        val atomic = AtomicFile(base)
        commit(atomic, "committed")
        atomic.startWrite().also { it.write("failed".toByteArray()); atomic.failWrite(it) }
        assertEquals("committed", base.readText())
        assertFalse(File(base.path + ".new").exists())
        atomic.startWrite().use { it.write("interrupted".toByteArray()) }
        assertEquals("committed", atomic.openRead().use { it.reader().readText() })
        assertFalse(File(base.path + ".new").exists())
    }

    @Test fun unfinishedFirstWriteNeverBecomesReadableCommittedData() {
        val base = File(temporary.root, "record")
        val atomic = AtomicFile(base)
        atomic.startWrite().use { it.write("unfinished".toByteArray()) }
        assertThrows(FileNotFoundException::class.java) { atomic.openRead() }
        assertFalse(base.exists())
        assertTrue(File(base.path + ".new").exists())
    }

    @Test fun legacyBackupRecoveryReplacesTheBaseAndDropsUnfinishedNewData() {
        val base = File(temporary.root, "record").apply { writeText("untrusted current") }
        File(base.path + ".bak").writeText("durable backup")
        File(base.path + ".new").writeText("unfinished")
        assertEquals("durable backup", AtomicFile(base).openRead().use { it.reader().readText() })
        assertFalse(File(base.path + ".bak").exists())
        assertFalse(File(base.path + ".new").exists())
    }
}
