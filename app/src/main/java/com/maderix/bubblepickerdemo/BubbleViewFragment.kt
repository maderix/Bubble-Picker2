package com.maderix.bubblepickerdemo


import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.debop.kodatimes.now
import com.igalata.bubblelist.BubblePickerListener
import com.igalata.bubblelist.adapter.BubblePickerAdapter
import com.igalata.bubblelist.adapter.BubbleTaskData
import com.igalata.bubblelist.model.BubbleGradient
import com.igalata.bubblelist.model.PickerItem

import kotlinx.android.synthetic.main.fragment_bubble_view.*
import org.joda.time.DateTime
import java.nio.charset.Charset


/**
 * A simple [Fragment] subclass.
 */
class BubbleViewFragment : Fragment() {

    private val boldTypeface by lazy { Typeface.createFromAsset(activity?.assets, ROBOTO_BOLD) }
    private val mediumTypeface by lazy { Typeface.createFromAsset(activity?.assets, ROBOTO_MEDIUM) }
    private val regularTypeface by lazy { Typeface.createFromAsset(activity?.assets, ROBOTO_REGULAR) }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_bubble_view, container, false)

        val titles = resources.getStringArray(R.array.countries)
        val colors = resources.obtainTypedArray(R.array.colors)
        val images = resources.obtainTypedArray(R.array.images)
        bubblePicker.adapter = object : BubblePickerAdapter {
            override val totalCount = titles.size
            override fun getItem(position: Int): PickerItem {
                return PickerItem().apply {
                    title = titles[position]
                    gradient = BubbleGradient(colors.getColor((position * 2) % 8, 0),
                            colors.getColor((position * 2) % 8 + 1, 0), BubbleGradient.VERTICAL)
                    typeface = mediumTypeface
                    textColor = ContextCompat.getColor(rootView.context, android.R.color.white)
                    backgroundImage = ContextCompat.getDrawable(rootView.context, images.getResourceId(position, 0))
                }
            }
        }

        //Add floating action button listener
        fab_bubble.setOnClickListener{
            BubbleFragment.newInstance(BubbleTaskData("Hello", false, DateTime(now().toDateTime()), 0, 0)).apply{
                activity?.supportFragmentManager.let {
                    it?.beginTransaction()?.add(this, "AddBubbleFragment")?.commit()
                }
            }
        }
        bubblePicker.bubbleSize = 10
        bubblePicker.listener = object : BubblePickerListener {
            override fun onBubbleSelected(item: PickerItem) {

                //toast("${item.title} selected")
            }

            override fun onBubbleDeselected(item: PickerItem) {
                //toast("${item.title} deselected")
            }

            override fun onSaveBubbles(json:String){
                val filename = "bubbles.save"
                activity?.applicationContext?.deleteFile(filename)
                activity?.applicationContext?.openFileOutput(filename, Context.MODE_PRIVATE).use {
                    it?.write(json.toByteArray())
                }
            }

            override fun onLoadBubbles(): String {
                val fileName = "bubbles.save"
                var string = ""
                var file = activity?.getFileStreamPath(fileName)
                if (!file!!.exists())
                    return string

                activity?.openFileInput(fileName).use{
                    var byteArray = ByteArray(it!!.available())
                    while((it.read(byteArray)) != -1)
                        string += byteArray.toString(Charset.defaultCharset())
                }
                return string
            }

            override fun addItem(bubbleTaskData: BubbleTaskData): PickerItem {
                return PickerItem().apply {
                    title = bubbleTaskData.taskName
                    gradient = BubbleGradient(bubbleTaskData.startColor ,
                            bubbleTaskData.endColor, BubbleGradient.VERTICAL)
                    typeface = mediumTypeface
                    textColor = ContextCompat.getColor(activity!!, android.R.color.white)
                    backgroundImage = ContextCompat.getDrawable(activity!!, images.getResourceId(1, 0))
                }
            }
        }
        return rootView
    }

    companion object {
        private const val ROBOTO_BOLD = "roboto_bold.ttf"
        private const val ROBOTO_MEDIUM = "roboto_medium.ttf"
        private const val ROBOTO_REGULAR = "roboto_regular.ttf"
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val BUBBLE_TASK_NAME = "param1"
        private val BUBBLE_TASK_URGENT = "param2"
        private val BUBBLE_TASK_DUE_DATE = "param3"
        private val BUBBLE_TASK_COLOR = "param4"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BubbleFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(fragmentNumber: Number): BubbleViewFragment {
            val fragment = BubbleViewFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor
