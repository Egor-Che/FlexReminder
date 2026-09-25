package com.example.flexreminder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.example.flexreminder.R
import java.security.MessageDigest

object NotificationChannels {

    const val CHANNEL_LOUD = "reminders_loud"
    const val CHANNEL_SILENT = "reminders_silent"

    private const val PREFIX_CUSTOM = "reminders_custom_"
    private const val PREFIX_DEFAULT = "reminders_default_"

    fun computeCustomChannelId(uri: String): String =
        PREFIX_CUSTOM + shortHash(uri)

    fun computeDefaultChannelId(uri: String): String =
        PREFIX_DEFAULT + shortHash(uri)

    private fun shortHash(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(s.toByteArray(Charsets.UTF_8))
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    fun getOrCreateCustomChannel(context: Context, uri: String): String {
        val id = computeCustomChannelId(uri)
        ensureSoundChannel(context, id, uri)
        return id
    }

    fun getOrCreateDefaultChannel(context: Context, uri: String): String {
        val id = computeDefaultChannelId(uri)
        ensureSoundChannel(context, id, uri)
        return id
    }

    fun findChannelIdForUri(context: Context, uri: String?): String {
        if (uri.isNullOrBlank()) return CHANNEL_LOUD

        val mgr = context.getSystemService(NotificationManager::class.java)
            ?: return CHANNEL_LOUD

        val customId = computeCustomChannelId(uri)
        if (mgr.getNotificationChannel(customId) != null) return customId

        val defaultId = computeDefaultChannelId(uri)
        if (mgr.getNotificationChannel(defaultId) != null) return defaultId

        return CHANNEL_LOUD
    }

    private fun ensureSoundChannel(context: Context, id: String, uriString: String) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(id) != null) return

        val uri = Uri.parse(uriString)

        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channel = NotificationChannel(
            id,
            context.getString(R.string.notification_channel_custom_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_custom_desc)
            setSound(uri, attrs)
        }
        mgr.createNotificationChannel(channel)
    }

    fun deleteChannel(context: Context, id: String) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.deleteNotificationChannel(id)
    }

    fun deleteChannelsForUri(context: Context, uri: String) {
        deleteChannel(context, computeCustomChannelId(uri))
        deleteChannel(context, computeDefaultChannelId(uri))
    }

    fun listCustomChannelIds(context: Context): List<String> {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return emptyList()
        return mgr.notificationChannels
            .map { it.id }
            .filter { it.startsWith(PREFIX_CUSTOM) || it.startsWith(PREFIX_DEFAULT) }
    }

    fun cleanupUnusedChannels(
        context: Context,
        activeChannelIds: Set<String>
    ): List<String> {
        val existing = listCustomChannelIds(context)
        val toDelete = existing.filter { it !in activeChannelIds }
        toDelete.forEach { deleteChannel(context, it) }
        return toDelete
    }
}