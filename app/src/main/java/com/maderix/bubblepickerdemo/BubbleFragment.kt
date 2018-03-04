package com.maderix.bubblepickerdemo

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import com.github.debop.kodatimes.now
import com.github.debop.kodatimes.toDateTime
import com.github.debop.kodatimes.tomorrow
import com.igalata.bubblelist.adapter.BubbleTaskData

import kotlinx.android.synthetic.main.fragment_bubble.*
import kotlinx.android.synthetic.main.fragment_bubble.view.*
import org.joda.time.DateTime
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [BubbleFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [BubbleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BubbleFragment : DialogFragment() {

    // TODO: Rename and change types of parameters
    private var bubbleTaskName: String? = null
    private var bubbleTaskUrgent: Boolean = false
    private var bubbleTaskDueDate: DateTime? = null
    private var bubbleTaskColor:Int  = 0

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            bubbleTaskName = arguments!!.getString(BUBBLE_TASK_NAME)
            bubbleTaskDueDate = arguments!!.getString(BUBBLE_TASK_DUE_DATE).toDateTime()
            bubbleTaskColor = arguments!!.getString(BUBBLE_TASK_COLOR).toInt()
            bubbleTaskUrgent = arguments!!.getString(BUBBLE_TASK_URGENT).toBoolean()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var rootView:View =  inflater.inflate(R.layout.fragment_bubble, container, false)
        rootView.bt_add_bubble.setOnClickListener {
            var title:String = ed_task_title.text.toString()
            var urgent:Boolean = cb_urgent.isChecked
            var dateTime:DateTime = DateTime()
            if (tb_today.isChecked)
                dateTime = now()
            else if(tb_today.isChecked)
                dateTime = tomorrow()
            Random().let {
                var startColor: Int = it.nextInt(10)
                var endColor: Int = it.nextInt(10)
                onButtonPressed(BubbleTaskData(title, urgent, dateTime, startColor, endColor))
            }

            dismiss()
        }
        rootView.tb_today.setOnCheckedChangeListener{buttonView, isChecked ->
            if (isChecked)
                buttonView.setBackgroundColor(resources.getColor(R.color.blueStart))
            else
                buttonView.setBackgroundColor(resources.getColor(R.color.blueEnd))
            rootView.tb_tomorrow.isChecked = false
        }
        rootView.tb_tomorrow.setOnCheckedChangeListener{ buttonView, isChecked ->
            if (isChecked)
                buttonView.setBackgroundColor(resources.getColor(R.color.blueStart))
            else
                buttonView.setBackgroundColor(resources.getColor(R.color.blueEnd))
            rootView.tb_today.isChecked = false
        }
        rootView.tb_someday.setOnClickListener{
            mListener!!.onFragmentInteraction(true)
            dismiss()
        }

        return rootView
    }

    private fun onButtonPressed(bubbleTaskData: BubbleTaskData) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(bubbleTaskData)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(bubbleTaskData: BubbleTaskData)
        fun onFragmentInteraction(enabled:Boolean)
    }

    companion object {
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
        fun newInstance(bubbleTaskData: BubbleTaskData): BubbleFragment {
            val fragment = BubbleFragment()
            val args = Bundle().apply {
                this.putString(BUBBLE_TASK_NAME, bubbleTaskData.taskName)
                this.putString(BUBBLE_TASK_URGENT, bubbleTaskData.urgent.toString())
                this.putString(BUBBLE_TASK_COLOR, bubbleTaskData.startColor.toString())
                this.putString(BUBBLE_TASK_DUE_DATE, bubbleTaskData.taskDueDate.toString())
            }

            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
