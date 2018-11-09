package ru.smityukh.vkc.data

import android.os.Parcel
import android.text.TextUtils
import com.vk.sdk.api.*
import com.vk.sdk.api.model.*
import org.json.JSONObject
import ru.smityukh.vkc.views.cardview.CardJugglerDataSource
import ru.smityukh.vkc.views.cardview.CardViewData
import ru.smityukh.vkc.views.cardview.VkPostData
import ru.smityukh.vkc.views.imagescarousel.ImagesCarouselData
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VkClient : CardJugglerDataSource {
    val lock: ReentrantLock = ReentrantLock()

    val posts: ArrayDeque<VkPostData> = ArrayDeque()
    val callbacks: ArrayDeque<DataRequestCallback<VkPostData>> =
        ArrayDeque()

    private var currentRequest: VKRequest? = null
    private var lastNextFrom: String? = null

    companion object {
        const val POST_PER_REQUEST = 5
        const val MIN_PRELOADED_ITEMS = 10
    }

    override fun getPost(callback: DataRequestCallback<VkPostData>) {
        lock.withLock {
            if (!posts.isEmpty()) {
                val postData = posts.poll()
                callback.success(postData)

                if (posts.count() < MIN_PRELOADED_ITEMS) {
                    requestPosts()
                }

                return
            }

            callbacks.push(callback)
        }
    }

    private fun publish(vkPostData: VkPostData) {
        lock.withLock {
            if (!callbacks.isEmpty()) {
                callbacks.poll().success(vkPostData)
                return
            }

            posts.push(vkPostData)
        }
    }

    fun run() {
        requestPosts()
    }

    private fun requestPosts() {
        lock.withLock {
            if (currentRequest != null) {
                return;
            }

            val params = VKParameters.from(
                VKApiConst.COUNT,
                POST_PER_REQUEST,
                VKApiConst.EXTENDED,
                "1"
            )
            if (lastNextFrom != null) {
                params.put("start_from", lastNextFrom)
            }

            val request = VKRequest("newsfeed.getDiscoverForContestant", params)

            request.setResponseParser(object : VKParser() {
                override fun createModel(json: JSONObject): Any {
                    return VKApiDiscoverContent().also { it.parse(json) }
                }
            })

            request.executeWithListener(object : VKRequest.VKRequestListener() {
                override fun onComplete(response: VKResponse) {
                    lock.withLock { currentRequest = null }
                    transformData(response.parsedModel as VKApiDiscoverContent)
                }

                override fun onError(error: VKError?) {
                    lock.withLock { currentRequest = null }
                    // Repeat request?
                }
            })

            currentRequest = request
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val postDate = Date(timestamp * 1000)
        val today = Date()

        if (postDate.year == today.year && postDate.month == today.month && postDate.day == today.day) {
            return SimpleDateFormat("сегодня в HH:mm", Locale.forLanguageTag("RU")).format(postDate)
        }

        return SimpleDateFormat("d MMM в HH:mm", Locale.forLanguageTag("RU")).format(postDate)
    }

    private fun nullIfEmpty(string: String): String? {
        if (TextUtils.isEmpty(string)) {
            return null
        }

        return string
    }

    private fun getAttachmentUrl(attachment: VKAttachments.VKApiAttachment): String? {
        val photo = attachment as? VKApiPhoto
        if (photo != null) {
            return nullIfEmpty(photo.photo_807) ?: nullIfEmpty(photo.photo_604) ?: nullIfEmpty(photo.photo_130)
        }

        val video = attachment as? VKApiVideo
        if (video != null) {
            return nullIfEmpty(video.photo_640) ?: nullIfEmpty(video.photo_320) ?: nullIfEmpty(video.photo_130)
        }

        // Unfortunately Link from SDK is incompatible and I have not time to do it
//        val link = attachment as? VKApiLink
//        if (link != null) {
//            return link.image_src
//        }

        return null
    }

    private fun transformData(vkData: VKApiDiscoverContent) {
        lastNextFrom = vkData.nextFrom

        val usersMap = vkData.users.map { it.id to it }.toMap()
        val groupsMap = vkData.groups.map { it.id to it }.toMap()

        for (item in vkData.posts) {
            if (item.attachments == null || item.attachments.count == 0) {
                continue
            }

            val attachments = item.attachments
                .map { getAttachmentUrl(it) }
                .filter { it != null }
                .map {
                    object : ImagesCarouselData.ImageInfo {
                        override val url: String
                            get() = it!!
                    }
                }
                .toList()

            if (attachments.isEmpty()) {
                continue
            }

            val images = object : ImagesCarouselData {
                override val count: Int
                    get() = attachments.count()

                override fun getImageInfo(index: Int): ImagesCarouselData.ImageInfo {
                    return attachments[index]
                }
            }

            val id = item.id
            var avatarUri: String
            var userName: String
            val timestamp: String = formatTimestamp(item.date)
            val description: String = item.text

            if (item.sourceId > 0) {
                val vkApiUser = usersMap[item.sourceId]
                if (vkApiUser == null) {
                    continue
                }

                //TODO: Flex selection absed on DPI is required
                avatarUri = vkApiUser.photo_100
                userName = vkApiUser.first_name + " " + vkApiUser.last_name
            } else {
                val vkApiCommunity = groupsMap[-item.sourceId]
                if (vkApiCommunity == null) {
                    continue
                }

                avatarUri = vkApiCommunity.photo_100
                userName = vkApiCommunity.name
            }

            val cardViewData = object : CardViewData {
                override fun like() {
                    this@VkClient.like(id)
                }

                override fun skip() {
                    this@VkClient.skip(id)
                }

                override val avatarUri: String
                    get() = avatarUri
                override val userName: String
                    get() = userName
                override val timestamp: String
                    get() = timestamp
                override val carouselData: ImagesCarouselData
                    get() = images
                override val description: String
                    get() = description
            }

            val vkPostData = object : VkPostData {
                override val cardViewData: CardViewData
                    get() = cardViewData
            }

            publish(vkPostData)
        }
    }

    private fun like(id: Int) {
        val params = VKParameters.from(
            "type", "post",
            "item_id", id
        )
        val request = VKRequest("likes.add", params)

        request.executeWithListener(object : VKRequest.VKRequestListener() {})
    }

    private fun skip(id: Int) {
        val params = VKParameters.from(
            "type", "post",
            "item_id", id
        )
        val request = VKRequest("newsfeed.ignoreItem", params)

        request.executeWithListener(object : VKRequest.VKRequestListener() {})
    }

    class VKApiPostEx : VKApiPost {
        var sourceId: Int = 0

        constructor(from: JSONObject?) : super(from)
        constructor(parcel: Parcel?) : super(parcel)

        override fun parse(source: JSONObject): VKApiPost {
            sourceId = source.optInt("source_id")
            return super.parse(source)
        }
    }

    class VKApiDiscoverContent {
        lateinit var posts: VKList<VKApiPostEx>
        lateinit var users: VKList<VKApiUser>
        lateinit var groups: VKList<VKApiCommunity>
        lateinit var nextFrom: String

        fun parse(json: JSONObject) {
            val response = json.getJSONObject("response")

            posts = VKList(response.getJSONArray("items"), VKApiPostEx::class.java)
            users = VKList(
                response.getJSONArray("profiles"),
                VKApiUser::class.java
            )
            groups = VKList(
                response.getJSONArray("groups"),
                VKApiCommunity::class.java
            )
            nextFrom = response.getString("next_from")
        }
    }
}