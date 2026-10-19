package dev.renkinProject.renkin.testing;

import android.util.AtomicFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/** Preserve Android's atomic replace semantics on a Windows JVM test filesystem. */
@Implements(value = AtomicFile.class, minSdk = 30)
public class WindowsAtomicFileShadow {
    @Implementation(minSdk = 30)
    protected static void rename(File source, File target) throws IOException {
        if (!System.getProperty("os.name", "").startsWith("Windows")) {
            Shadow.directlyOn(AtomicFile.class, "rename", ClassParameter.from(File.class, source),
                    ClassParameter.from(File.class, target));
            return;
        }
        // AOSP AtomicFile.rename relies on POSIX rename replacing an existing base.
        // Do not delete that base or fall back to a non-atomic copy when this fails.
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    }
}
