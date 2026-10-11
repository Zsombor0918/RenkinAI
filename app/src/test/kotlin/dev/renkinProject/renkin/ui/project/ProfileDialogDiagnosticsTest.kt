package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.ui.ProfileDetailsDialog
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/** Temporary measured isolation: no clock, idling, focus or production changes. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w450dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileDialogDiagnosticsTest {
    @get:Rule(order = 0) val compose = createComposeRule()
    @get:Rule(order = 1) val probe = ComposeIdleProbe(compose)

    private fun content(body: @Composable () -> Unit) {
        compose.setContent { RenkinTheme(false) { SideEffect { probe.compositions.incrementAndGet() }; body() } }
    }

    @Composable
    private fun Field(labelled: Boolean = true, supporting: Boolean = false) {
        var value by remember { mutableStateOf("") }
        OutlinedTextField(value, { value = it },
            label = if (labelled) ({ Text("Profile name") }) else null,
            supportingText = if (supporting) ({ Text("Required") }) else null,
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("diagnostic-field"))
    }

    @Test fun plainContent() {
        content { Text("Plain", Modifier.testTag("diagnostic")) }
        compose.onNodeWithTag("diagnostic").assertIsDisplayed()
    }

    @Test fun emptyDialog() {
        content { RenkinAlertDialog({}, text = { Text("Dialog", Modifier.testTag("diagnostic")) }) }
        compose.onNodeWithTag("diagnostic").assertIsDisplayed()
    }

    @Test fun unlabelledFieldInDialog() {
        content { RenkinAlertDialog({}, text = { Field(labelled = false) }) }
        compose.onNodeWithTag("diagnostic-field").performTextInput("Night")
    }

    @Test fun labelledFieldOutsideDialog() {
        content { Field() }
        compose.onNodeWithTag("diagnostic-field").performTextInput("Night")
    }

    @Test fun labelledFieldInDialog() {
        content { RenkinAlertDialog({}, text = { Field() }) }
        compose.onNodeWithTag("diagnostic-field").performTextInput("Night")
    }

    @Test fun supportingFieldInDialog() {
        content { RenkinAlertDialog({}, text = { Field(supporting = true) }) }
        compose.onNodeWithTag("diagnostic-field").performTextInput("Night")
    }

    @Test fun threeFieldsInDialog() {
        content { RenkinAlertDialog({}, text = { Column(Modifier.fillMaxWidth()) {
            Field(supporting = true); Field(); Field(supporting = true)
        } }) }
        compose.waitForIdle()
    }

    @Test fun realProfileDialog() {
        content { ProfileDetailsDialog("Create new icon pack", "Create", { _, _, _ -> }, {}) }
        compose.onNodeWithTag("profile-name").performTextInput("Night")
    }

    @Test fun realProjectShell() {
        content { ProjectShell(
            state = ProjectHomeState(profiles = listOf(Profile(1, "Default")), availability = ProjectHomeAvailability.READY),
            operation = ProjectSessionState.Idle,
            onOpen = {}, onCreate = { _, _, _ -> true }, onSave = {}, onCancel = {},
            onAcknowledge = {}, onRetry = {}, onExit = {}, editor = { Text("Classic editor") }
        ) }
        compose.onNodeWithTag("project-create").performClick()
        compose.onNodeWithTag("profile-name").performTextInput("Night")
    }

    @Test fun realProfileDialogAfterPlatformDraw() {
        content {
            var show by remember { mutableStateOf(false) }
            TextButton(onClick = { show = true }, modifier = Modifier.testTag("show-dialog")) { Text("Create") }
            if (show) ProfileDetailsDialog("Create new icon pack", "Create", { _, _, _ -> }, {})
        }
        compose.onNodeWithTag("show-dialog").performClick()
        awaitDialogWindow()
        compose.runOnUiThread {
            shadowOf(Looper.getMainLooper()).idle()
            val view = requireNotNull(ShadowDialog.getLatestDialog()?.window?.decorView)
            check(view.width > 0 && view.height > 0) { "Dialog has not completed Android layout" }
            println("PLATFORM_DRAW before ${view.width}x${view.height} layoutRequested=${view.isLayoutRequested}")
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try { view.draw(Canvas(bitmap)) } finally { bitmap.recycle() }
            println("PLATFORM_DRAW after layoutRequested=${view.isLayoutRequested}")
        }
        compose.onNodeWithTag("profile-name").performTextInput("Night")
    }

    @Test @Config(sdk = [35]) fun realProfileDialogSdk35() {
        content { ProfileDetailsDialog("Create new icon pack", "Create", { _, _, _ -> }, {}) }
        compose.onNodeWithTag("profile-name").performTextInput("Night")
    }

    private fun awaitDialogWindow() {
        compose.waitUntil {
            compose.runOnUiThread {
                shadowOf(Looper.getMainLooper()).idle()
                val view = ShadowDialog.getLatestDialog()?.window?.decorView
                view != null && view.isAttachedToWindow && view.width > 0 && view.height > 0
            }
        }
    }

    @Test fun realProfileDialogWithMeasuredWindowBounds() {
        content {
            var show by remember { mutableStateOf(false) }
            TextButton(onClick = { show = true }, modifier = Modifier.testTag("show-dialog")) { Text("Create") }
            if (show) ProfileDetailsDialog("Create new icon pack", "Create", { _, _, _ -> }, {})
        }
        compose.onNodeWithTag("show-dialog").performClick()
        awaitDialogWindow()
        compose.runOnUiThread {
            val window = requireNotNull(ShadowDialog.getLatestDialog()?.window)
            println("WINDOW_BOUNDS before params=${window.attributes.width}x${window.attributes.height} measured=${window.decorView.width}x${window.decorView.height}")
            window.setLayout(window.decorView.width, window.decorView.height)
        }
        compose.onNodeWithTag("profile-name").performTextInput("Night")
    }

    @Test @GraphicsMode(GraphicsMode.Mode.LEGACY) fun realProfileDialogLegacyGraphicsControl() {
        content { ProfileDetailsDialog("Create new icon pack", "Create", { _, _, _ -> }, {}) }
        compose.onNodeWithTag("profile-name").performTextInput("Night")
    }
}

/** Reports the existing test rule's idler/root state before its teardown, without pumping it. */
class ComposeIdleProbe(private val compose: ComposeContentTestRule) : TestWatcher() {
    val compositions = AtomicInteger()
    private val writes = AtomicInteger()
    private val applies = AtomicInteger()
    private val samples = Collections.synchronizedList(mutableListOf<String>())
    private var writeHandle: ObserverHandle? = null
    private var applyHandle: ObserverHandle? = null

    override fun starting(description: Description) {
        writeHandle = Snapshot.registerGlobalWriteObserver { state ->
            val number = writes.incrementAndGet()
            if (number <= 120 || number % 10000 == 0) {
                val stack = Thread.currentThread().stackTrace
                    .filter { it.className.startsWith("androidx.compose") && !it.className.contains("snapshots") }
                    .take(9).joinToString(" <- ")
                synchronized(samples) {
                    if (number > 120 || samples.none { it.endsWith(stack) }) {
                        if (samples.size >= 12) samples.removeAt(0)
                        val proxy = (state as? androidx.compose.runtime.MutableState<*>)?.value
                        val layout = if (proxy?.javaClass?.name?.endsWith("TextLayoutResultProxy") == true) {
                            runCatching { proxy.javaClass.getMethod("getValue").invoke(proxy) as androidx.compose.ui.text.TextLayoutResult }.getOrNull()
                        } else null
                        samples += "write=$number ${state.toString().take(180)} constraints=${layout?.layoutInput?.constraints} size=${layout?.size}: $stack"
                    }
                }
            }
        }
        applyHandle = Snapshot.registerApplyObserver { _, _ -> applies.incrementAndGet() }
    }

    override fun failed(error: Throwable, description: Description) { report(description, "FAILED") }
    override fun succeeded(description: Description) { report(description, "PASSED") }

    private fun report(description: Description, result: String) {
        println("IDLE_PROBE ${description.methodName} $result compositions=${compositions.get()} writes=${writes.get()} applies=${applies.get()} pendingSnapshot=${Snapshot.current.hasPendingChanges()}")
        synchronized(samples) { samples.forEach { println("IDLE_WRITE $it") } }
        compose.runOnUiThread {
            // The test rule has no idle-dependent activity lookup; dialog inspection is separate.
            ShadowDialog.getLatestDialog()?.window?.decorView?.let { inspectView(it, "dialog") }
            // Walk the rule's small internal environment graph to read the actual installed idler flags.
            val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
            inspectObject(compose, 0, visited)
        }
    }

    private fun inspectView(view: View, window: String) {
        if (view.javaClass.name.contains("AndroidComposeView")) {
            val pending = view.javaClass.methods.firstOrNull { it.name.startsWith("getHasPendingMeasureOrLayout") && it.parameterCount == 0 }
                ?.let { runCatching { it.invoke(view) }.getOrNull() }
            println("IDLE_ROOT $window ${view.javaClass.simpleName} ${view.width}x${view.height} attached=${view.isAttachedToWindow} focused=${view.hasWindowFocus()} layoutRequested=${view.isLayoutRequested} pendingMeasure=$pending")
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) inspectView(view.getChildAt(index), window)
    }

    private fun inspectObject(value: Any, depth: Int, visited: MutableSet<Any>) {
        if (depth > 5 || !visited.add(value)) return
        if (value is androidx.activity.ComponentActivity) inspectView(value.window.decorView, "activity")
        if (value.javaClass.name.contains("ComposeIdlingResource")) {
            value.javaClass.declaredFields.forEach { field ->
                runCatching { field.isAccessible = true; field.get(value) }.getOrNull()?.let { fieldValue ->
                    if (fieldValue is Boolean || fieldValue is Number) println("IDLE_FLAG ${field.name}=$fieldValue")
                }
            }
            value.javaClass.methods.firstOrNull { it.name == "getDiagnosticMessageIfBusy" && it.parameterCount == 0 }
                ?.let { println("IDLE_DIAGNOSTIC ${runCatching { it.invoke(value) }.getOrNull()}") }
        }
        var type: Class<*>? = value.javaClass
        while (type != null && type.name != "java.lang.Object") {
            type.declaredFields.forEach { field ->
                val child = runCatching { field.isAccessible = true; field.get(value) }.getOrNull()
                if (child != null && (child.javaClass.name.startsWith("androidx.compose.ui.test") || child is ComponentActivity)) {
                    inspectObject(child, depth + 1, visited)
                }
            }
            type = type.superclass
        }
    }

    override fun finished(description: Description) { writeHandle?.dispose(); applyHandle?.dispose() }
}
