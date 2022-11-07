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
package org.linphone.activities.main.chat.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.linphone.activities.main.chat.data.ChatMessageReactionsListData
import org.linphone.core.ChatMessage
import org.linphone.databinding.ChatMessageReactionsListDialogBinding

class ChatMessageReactionsListDialogFragment(
    private val chatMessage: ChatMessage
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "ChatMessageReactionsListDialogFragment"
    }

    private lateinit var data: ChatMessageReactionsListData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ChatMessageReactionsListDialogBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner

        data = ChatMessageReactionsListData(chatMessage)
        binding.data = data

        return binding.root
    }

    override fun onDestroy() {
        data.onDestroy()
        super.onDestroy()
    }
}
