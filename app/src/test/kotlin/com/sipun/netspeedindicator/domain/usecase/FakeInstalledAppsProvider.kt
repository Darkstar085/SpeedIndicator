package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.InstalledAppInfo
import com.sipun.netspeedindicator.domain.repository.InstalledAppsProvider

/** Hand-written test double so view-model tests don't need a mocking library. */
class FakeInstalledAppsProvider : InstalledAppsProvider {

    var apps: Map<Int, InstalledAppInfo> = emptyMap()

    override fun launcherApps(): Map<Int, InstalledAppInfo> = apps

    override fun appInfoForUid(uid: Int): InstalledAppInfo? = apps[uid]
}
