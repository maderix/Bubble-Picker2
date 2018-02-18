package com.igalata.bubblelist.rendering

import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.view.View
import com.github.debop.kodatimes.now
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.igalata.bubblelist.*
import com.igalata.bubblelist.adapter.BubbleTaskData
import com.igalata.bubblelist.model.Color
import com.igalata.bubblelist.model.PickerItem
import com.igalata.bubblelist.model.SavedBubble
import com.igalata.bubblelist.physics.Engine
import com.igalata.bubblelist.rendering.BubbleShader.A_POSITION
import com.igalata.bubblelist.rendering.BubbleShader.A_UV
import com.igalata.bubblelist.rendering.BubbleShader.U_BACKGROUND
import com.igalata.bubblelist.rendering.BubbleShader.fragmentShader
import com.igalata.bubblelist.rendering.BubbleShader.vertexShader
import org.jbox2d.common.Vec2
import java.io.File
import java.nio.FloatBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.collections.ArrayList

/**
 * Created by irinagalata on 1/19/17.
 */
class PickerRenderer(val glView: View) : GLSurfaceView.Renderer {

    var backgroundColor: Color? = null
    var maxSelectedCount: Int? = null
        set(value) {
            Engine.maxSelectedCount = value
        }
    var bubbleSize = 50
        set(value) {
            Engine.radius = value
        }
    var listener: BubblePickerListener? = null
    lateinit var items: ArrayList<PickerItem>
    val selectedItems: List<PickerItem?>
        get() = Engine.selectedBodies.map { synchronized(circles){circles.firstOrNull { circle -> circle.circleBody == it }?.pickerItem } }
    var centerImmediately = false
        set(value) {
            field = value
            Engine.centerImmediately = value
        }

    private var programId = 0
    private var verticesBuffer: FloatBuffer? = null
    private var uvBuffer: FloatBuffer? = null
    private var vertices: FloatArray? = null
    private var textureVertices: FloatArray? = null
    private var textureIds: IntArray? = null
    private var MAX_CIRCLES: Int = 512
    private val scaleX: Float
        get() = if (glView.width < glView.height) glView.height.toFloat() / glView.width.toFloat() else 1f
    private val scaleY: Float
        get() = if (glView.width < glView.height) 1f else glView.width.toFloat() / glView.height.toFloat()
    private val circles = ArrayList<Item>()
    private var restored:Boolean = false
    private val startX
        get() = if (Engine.centerImmediately) 0.5f else 0.7f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(backgroundColor?.red ?: 1f, backgroundColor?.green ?: 1f,
                backgroundColor?.blue ?: 1f, backgroundColor?.alpha ?: 1f)
        enableTransparency()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        initialize()
    }
    /*Synchronized to protect from concurrency exceptions*/
    @Synchronized override fun onDrawFrame(gl: GL10?) {
        calculateVertices()
        Engine.move()
        drawFrame()
    }

    private fun initialize() {
        //if (!restored) {
            clear()
            Engine.centerImmediately = centerImmediately
            Engine.init(scaleX, scaleY)
            if (textureIds == null) textureIds = IntArray(MAX_CIRCLES * 2)
//            for (i in 0 until 2){
//                addItem(items[i], scaleX, scaleY)
//            }
            restored = true
       // }
        restoreState()
        initializeArrays(MAX_CIRCLES)
        synchronized(circles) {
            items.forEach { if (it.isSelected) Engine.resize(circles.first { circle -> circle.pickerItem == it }) }
        }

    }

    fun addItem(pi:PickerItem, scaleX : Float, scaleY:Float, radius:Float = 0.0f,density:Float = 0.0f){
        var body = Engine.addBody(scaleX, scaleY, pi.x, pi.y, radius, density)
        var item = Item(pi,body)
        synchronized(circles) {
            circles.add(item)
        }
        //initializeItem(item,circles.size-1)
        //drawFrame()
    }

    private fun initializeArrays(arr_size:Int) {
        vertices = FloatArray(arr_size * 8)
        textureVertices = FloatArray(arr_size * 8)
        synchronized(circles) {
            circles.forEachIndexed { i, item -> initializeItem(item, i) }
        }
        verticesBuffer = vertices?.toFloatBuffer()
        uvBuffer = textureVertices?.toFloatBuffer()
    }

    private fun initializeItem(item: Item, index: Int) {
        initializeVertices(item, index)
        textureVertices?.passTextureVertices(index)
        item.bindTextures(textureIds ?: IntArray(0), index)
    }

    private fun calculateVertices() {
        synchronized(circles) {
            circles.forEachIndexed { i, item -> initializeVertices(item, i) }
        }
        vertices?.forEachIndexed { i, float -> verticesBuffer?.put(i, float) }
    }

    private fun initializeVertices(body: Item, index: Int) {
        val radius = body.radius
        val radiusX = radius * scaleX
        val radiusY = radius * scaleY

        body.initialPosition.apply {
            vertices?.put(8 * index, floatArrayOf(x - radiusX, y + radiusY, x - radiusX, y - radiusY,
                    x + radiusX, y + radiusY, x + radiusX, y - radiusY))
        }
    }

    private fun drawFrame() {
        glClear(GL_COLOR_BUFFER_BIT)
        glUniform4f(glGetUniformLocation(programId, U_BACKGROUND), 1f, 1f, 1f, 0f)
        verticesBuffer?.passToShader(programId, A_POSITION)
        uvBuffer?.passToShader(programId, A_UV)
        //circles.forEachIndexed { i, circle -> circle.drawItself(programId, i, scaleX, scaleY) }
        //TODO:Ugly hack.Ensure no errors result due to concurrent modifications
        synchronized(circles) {
            circles.forEachIndexed { i, value -> value.drawItself(programId, i, scaleX, scaleY) }
        }

    }

    private fun enableTransparency() {
        glEnable(GLES20.GL_BLEND)
        glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        attachShaders()
    }

    private fun attachShaders() {
        programId = createProgram(createShader(GL_VERTEX_SHADER, vertexShader),
                createShader(GL_FRAGMENT_SHADER, fragmentShader))
        glUseProgram(programId)
    }

    private fun createProgram(vertexShader: Int, fragmentShader: Int) = glCreateProgram().apply {
        glAttachShader(this, vertexShader)
        glAttachShader(this, fragmentShader)
        glLinkProgram(this)
    }

    private fun createShader(type: Int, shader: String) = GLES20.glCreateShader(type).apply {
        glShaderSource(this, shader)
        glCompileShader(this)
    }

    fun swipe(x: Float, y: Float):Boolean{
        var pos:Vec2 = Vec2(x,y).screenToWorld(Vec2(scaleX,scaleY), Vec2(glView.width.toFloat(),glView.height.toFloat()))
        return Engine.swipe(pos.x,pos.y)
9    }

    fun moveTo(x: Float, y: Float) {
        var pos:Vec2 = Vec2(x,y).screenToWorld(Vec2(scaleX,scaleY), Vec2(glView.width.toFloat(),glView.height.toFloat()))
        Engine.moveTo(pos.x,pos.y)
    }

    fun release() = Engine.release()

    private fun getItem(position: Vec2) = position.let {
        val x = it.x.convertPoint(glView.width, scaleX)
        val y = it.y.convertPoint(glView.height, scaleY)
        synchronized(circles) {
            circles.find { Math.sqrt(((x - it.x).sqr() + (y - it.y).sqr()).toDouble()) <= it.radius }
        }
    }

    fun resize(x: Float, y: Float) = getItem(Vec2(x, glView.height - y))?.apply {
        if (Engine.resize(this)) {
            listener?.let {
                if (circleBody.increased) it.onBubbleDeselected(pickerItem) else it.onBubbleSelected(pickerItem)
            }
        }
    }

    fun delete(x: Float, y:Float) = getItem(Vec2(x, glView.height - y))?.apply {
        this.circleBody.delete()
        Engine.bodies.remove(this.circleBody)
        synchronized(circles) {
            circles.remove(this)
        }
        listener?.let {

        }
    }

    private fun clear() {
        synchronized(circles) {
            circles.clear()
        }
        Engine.clear()
    }

    /*Save the state to a file */
    fun saveState(){
        var gson:Gson = Gson()
        var circleList = ArrayList<SavedBubble>().apply {
            synchronized(circles) {
                circles.forEach {
                    this.add(SavedBubble().apply {
                        this.title = it.pickerItem.title
                        this.x = it.x
                        this.y = it.y
                        this.density = it.circleBody.density
                        this.radius = it.radius / scaleX
                        this.gradient = it.pickerItem.gradient
                    })
                }
            }
        }
        if (circleList.isEmpty())return
        var json = gson.toJson(circleList)
        listener?.let {
            it.onSaveBubbles(json)
        }
        clear()
    }
    /* Restore the saved objects to surface view */
    fun restoreState(){
        var string = ""
        listener?.let {
            string = it.onLoadBubbles()
            if (string == "")return
        }
        Engine.createBorders()
        val gson = Gson()
        val circleList:ArrayList<SavedBubble> = gson.fromJson(string, object : TypeToken<ArrayList<SavedBubble>>(){}.type)
        circleList.forEach {
            val bubbleTaskData = BubbleTaskData(it.title as String,false, com.github.debop.kodatimes.now(),it.gradient!!.startColor,it.gradient!!.endColor)
            val pi = listener!!.addItem(bubbleTaskData)
            pi.x = it.x
            pi.y = it.y
            addItem(pi,scaleX,scaleY,it.radius,it.density)
        }
    }
}