/*
 * Copyright (c) 2010-2022 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.activities.main.chat.data

import androidx.lifecycle.MutableLiveData
import org.linphone.core.Address
import org.linphone.core.ChatMessage
import org.linphone.core.ChatMessageListenerStub
import org.linphone.core.tools.Log

class ChatMessageReactionsListData(private val chatMessage: ChatMessage) {
    val reactions = MutableLiveData<ArrayList<ChatMessageReactionData>>()

    val listener = object : ChatMessageListenerStub() {
        override fun onReactionReceived(message: ChatMessage, address: Address, reaction: String) {
            Log.i("[Chat Message Reactions List] Reaction received [$reaction] from [${address.asStringUriOnly()}]")
            updateReactionsList(message)
        }
    }

    init {
        chatMessage.addListener(listener)
        updateReactionsList(chatMessage)
    }

    fun onDestroy() {
        chatMessage.removeListener(listener)
    }

    private fun updateReactionsList(chatMessage: ChatMessage) {
        reactions.value.orEmpty().forEach(ChatMessageReactionData::destroy)

        val reactionsList = arrayListOf<ChatMessageReactionData>()
        for (reaction in chatMessage.reactions) {
            val data = ChatMessageReactionData(reaction)
            reactionsList.add(data)
        }
        reactions.value = reactionsList
    }
}
