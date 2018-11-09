/*
 * MainActivity.kt
 *
 * This file is a part of the Yandex Search for Android project.
 *
 * (C) Copyright 2018 Yandex, LLC. All rights reserved.
 *
 * Author: Viacheslav Smityukh <smityukh@yandex-team.ru>
 */

package ru.smityukh.vkc

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.vk.sdk.VKAccessToken
import com.vk.sdk.VKCallback
import com.vk.sdk.VKSdk
import com.vk.sdk.api.VKError
import kotlinx.android.synthetic.main.activity_main.*
import ru.smityukh.vkc.data.VkClient


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginButton.setOnClickListener { VKSdk.login(this, "wall") }

        actionButtonLike.setOnClickListener { cardJugglerView.likeCurrentCard() }
        actionButtonSkip.setOnClickListener { cardJugglerView.skipCurrentCard() }
    }

    override fun onResume() {
        super.onResume()

        if (!VKSdk.isLoggedIn()) {
            loginButton.visibility = VISIBLE
            mainApp.visibility = GONE
            return;
        }

        loginButton.visibility = GONE
        mainApp.visibility = VISIBLE

        loadData();
    }

    private fun loadData() {
        val vkClient = VkClient()
        vkClient.run()

        cardJugglerView.dataSource = vkClient
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val authCallback = object : VKCallback<VKAccessToken> {
            override fun onResult(res: VKAccessToken) {
                loadData()
            }

            override fun onError(error: VKError) {
                Toast.makeText(this@MainActivity, "Вход в Вконтакте не выполнен", Toast.LENGTH_SHORT).show()
            }
        }

        if (VKSdk.onActivityResult(requestCode, resultCode, data, authCallback)) {
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}

