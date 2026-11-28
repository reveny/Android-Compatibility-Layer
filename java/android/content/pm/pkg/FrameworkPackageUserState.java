package android.content.pm.pkg;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class FrameworkPackageUserState {

    public static final FrameworkPackageUserState DEFAULT = new FrameworkPackageUserState();

    public FrameworkPackageUserState() {
    }

    public boolean isInstalled() {
        return true;
    }

    public boolean isHidden() {
        return false;
    }

    public boolean isSuspended() {
        return false;
    }

    public boolean isStopped() {
        return false;
    }

    public boolean isNotLaunched() {
        return false;
    }

    public boolean isInstantApp() {
        return false;
    }

    public boolean isVirtualPreload() {
        return false;
    }

    public int getEnabledState() {
        return 0; // PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }

    public int getInstallReason() {
        return 0; // PackageManager.INSTALL_REASON_UNKNOWN
    }

    public int getUninstallReason() {
        return 0;
    }

    public Set<String> getEnabledComponents() {
        return Collections.emptySet();
    }

    public Set<String> getDisabledComponents() {
        return Collections.emptySet();
    }

    public Map<String, Object> getAllOverlayPaths() {
        return Collections.emptyMap();
    }

    public long getCeDataInode() {
        return 0;
    }

    public String getHarmfulAppWarning() {
        return null;
    }

    public String getLastDisableAppCaller() {
        return null;
    }

    public String getSplashScreenTheme() {
        return null;
    }

    public boolean isComponentEnabled(String componentName) {
        return false;
    }

    public boolean isComponentDisabled(String componentName) {
        return false;
    }
}
