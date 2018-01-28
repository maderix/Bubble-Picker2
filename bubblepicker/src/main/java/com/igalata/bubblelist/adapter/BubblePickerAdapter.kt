package com.igalata.bubblelist.adapter

import com.igalata.bubblelist.model.PickerItem

/**
 * Created by irinagalata on 5/22/17.
 */
interface BubblePickerAdapter {

    val totalCount: Int

    fun getItem(position: Int): PickerItem

    fun addItem(text: String): PickerItem

}