package com.igalata.bubblelist

import com.igalata.bubblelist.adapter.BubbleTaskData
import com.igalata.bubblelist.model.PickerItem

/**
 * Created by irinagalata on 3/6/17.
 */
interface BubblePickerListener {

    fun onBubbleSelected(item: PickerItem)

    fun onBubbleDeselected(item: PickerItem)

    fun onSaveBubbles(json: String)

    fun onLoadBubbles():String

    fun addItem(bubbleTaskData: BubbleTaskData): PickerItem

}