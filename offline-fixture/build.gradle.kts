import java.util.Locale

plugins { alias(libs.plugins.android.application) }

android {
    namespace = "dev.renkinai.offlinefixture"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.renkinai.offlinefixture"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1"
    }
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/fixture-res"))
}
androidComponents.beforeVariants(androidComponents.selector().withBuildType("release")) { it.enable = false }

// Real Android resources, generated deterministically from geometry; no private artwork.
val generateFixtureResources by tasks.registering {
    val output = layout.buildDirectory.dir("generated/fixture-res")
    outputs.dir(output)
    doLast {
        val root = output.get().asFile
        val drawable = root.resolve("drawable").apply { mkdirs() }
        val xml = root.resolve("xml").apply { mkdirs() }
        val items = (0 until 60).joinToString("\n") { i ->
            val vertices = 3 + i % 7
            val radius = 40 + (i % 5) * 13
            val path = (0 until vertices).joinToString(" ") { n ->
                val angle = Math.toRadians(n * 360.0 / vertices + i * 17)
                val x = 128 + radius * kotlin.math.cos(angle)
                val y = 128 + radius * kotlin.math.sin(angle)
                String.format(Locale.ROOT, "%s%.2f,%.2f", if (n == 0) "M" else "L", x, y)
            } + " Z"
            val color = String.format("#FF%02X%02X%02X", (i * 47 + 30) % 256, (i * 79 + 60) % 256, (i * 131 + 90) % 256)
            drawable.resolve("fixture_$i.xml").writeText("""
                <vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="256dp" android:height="256dp" android:viewportWidth="256" android:viewportHeight="256">
                    <path android:fillColor="$color" android:pathData="$path"/>
                    <path android:fillColor="#FFFFFFFF" android:pathData="M${40+i},${60+i} h${20+i%20} v12 h-${20+i%20} Z"/>
                </vector>
            """.trimIndent())
            "<item component=\"ComponentInfo{dev.renkinai.synthetic$i/.Main}\" drawable=\"fixture_$i\"/>"
        }
        xml.resolve("appfilter.xml").writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources>$items</resources>")
    }
}
tasks.named("preBuild").configure { dependsOn(generateFixtureResources) }
