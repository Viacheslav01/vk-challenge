package ru.smityukh.vkc

import android.app.Application
import com.vk.sdk.VKAccessToken
import com.vk.sdk.VKAccessTokenTracker
import com.vk.sdk.VKSdk
import com.vk.sdk.api.VKApi
import com.vk.sdk.api.VKApiConst
import com.vk.sdk.api.VKParameters
import com.vk.sdk.api.VKRequest
import com.vk.sdk.api.VKError
import com.vk.sdk.api.VKResponse
import com.vk.sdk.api.VKRequest.VKRequestListener






class VKChallengeApp : Application() {
    private val vkAccessTokenTracker: VKAccessTokenTracker = object : VKAccessTokenTracker() {
        override fun onVKAccessTokenChanged(oldToken: VKAccessToken?, newToken: VKAccessToken?) {
            if (newToken == null) {
                //TODO: Add lost authorization here
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // VK SDK initialization
        vkAccessTokenTracker.startTracking();
        VKSdk.initialize(this);
    }
}