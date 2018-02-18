package com.igalata.bubblelist.physics

import android.text.style.LineHeightSpan
import android.util.Log
import com.igalata.bubblelist.rendering.Item
import com.igalata.bubblelist.sqr
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.World
import java.util.*

/**
 * Created by irinagalata on 1/26/17.
 */
object Engine {

    val selectedBodies: List<CircleBody>
        get() = bodies.filter { it.increased || it.toBeIncreased || it.isIncreasing }
    var maxSelectedCount: Int? = null
    var radius = 20
        set(value) {
            field = value
            bubbleRadius = interpolate(0.1f, 0.25f, value / 100f)
            gravity = interpolate(20f, 80f, value / 100f)
            standardIncreasedGravity = interpolate(500f, 800f, value / 100f)
        }
    var centerImmediately = false
    private var standardIncreasedGravity = interpolate(500f, 800f, 0.5f)
    private var bubbleRadius = 0.17f

    private val world = World(Vec2(0f, 0f), false)
    private val step = 0.0005f
    val bodies: ArrayList<CircleBody> = ArrayList()
    private var borders: ArrayList<Border> = ArrayList()
    private val resizeStep = 0.005f
    private var scaleX = 0f
    private var scaleY = 0f
    private var touch = false
    private var gravity = 6f
    private var increasedGravity = 55f
    private var gravityCenter = Vec2(0f, 0f)
    private val currentGravity: Float
        get() = if (touch) increasedGravity else gravity
    private val toBeResized = ArrayList<Item>()
    private val startX
        get() = if (centerImmediately) 0.5f else 0.7f
    private var stepsCount = 0

    private fun buildBody(scaleX:Float,scaleY: Float, x:Float, y:Float, radius: Float = 0.0f, density: Float = 0.0f):CircleBody{
        var randRadius = if (radius != 0.0f) radius else  0.1f + Random().nextFloat()*(0.155f - 0.1f)
        var randDensity = if (density != 0.0f) density else (0.5f - interpolate(0.3f, 0.5f, (randRadius - 0.1f)/(0.155f - 0.1f)))
        return CircleBody(world, Vec2(x, y), randRadius * scaleX, (randRadius * scaleX) * 1.3f, randDensity)
    }

    fun addBody(scaleX: Float,scaleY: Float,x: Float,y: Float, radius:Float=0.0f,density: Float = 0.0f):CircleBody = buildBody(scaleX,scaleY,x,y,radius,density).let {
        bodies.add(it)
        return it
    }

    fun build(bodiesCount: Int, scaleX: Float, scaleY: Float): List<CircleBody> {
        for (i in 0 until bodiesCount-1) {
            //addBody(scaleX,scaleY)
        }

        return bodies
    }

    fun init(scaleX: Float, scaleY: Float){
        this.scaleX = scaleX
        this.scaleY = scaleY
    }

    @Synchronized fun move() {
        toBeResized.forEach { it.circleBody.resize(resizeStep) }
        world.step(if (centerImmediately) 0.035f else step, 11, 11)
        bodies.forEach { move(it) }
        toBeResized.removeAll(toBeResized.filter { it.circleBody.finished })
        stepsCount++
        if (stepsCount >= 10) {
            centerImmediately = false
        }
    }

    fun swipe(x: Float, y: Float):Boolean {
        if (!selectedBodies.isEmpty())return false

       gravityCenter.apply {
           this.x = x
           this.y = y
       }
        increasedGravity = standardIncreasedGravity * Math.abs(x * 1.5f) * Math.abs(y * 1.5f)
        touch = true
        return true
    }

    fun moveTo(x: Float, y: Float){
        touch = false
        selectedBodies.forEach {
            it.physicalBody.fixtureList.isSensor = true
            //it.physicalBody.type = BodyType.KINEMATIC
            it.physicalBody.setTransform(Vec2(x,y), Math.toRadians(0.0).toFloat())
            //it.position =  Vec2(x*10,y*10)
            //Log.d("moveTo:", "x," + x + "y" + y)
            //it.physicalBody.fixtureList.isSensor = false
        }
    }
    fun release() {
        //if (selectedBodies.isEmpty()) {
            gravityCenter.setZero()
        //}
        //gravityCenter.set(0f,1f)
        Log.d("engine:","release ")
        selectedBodies.forEach {
            it.physicalBody.fixtureList.isSensor = false
        }
        touch = false

        //increasedGravity = standardIncreasedGravity
    }

    fun clear() {
        borders.forEach { world.destroyBody(it.itemBody) }
        bodies.forEach { world.destroyBody(it.physicalBody) }
        borders.clear()
        bodies.clear()
    }

    fun resize(item: Item): Boolean {
        if (selectedBodies.size >= maxSelectedCount ?: bodies.size && !item.circleBody.increased) return false

        if (item.circleBody.isBusy) return false
        //item.circleBody.physicalBody.applyForce(Vec2(0f,5000f), Vec2(0f,100f))
        item.circleBody.defineState()
        toBeResized.add(item)
        return true
    }

    fun delete(item: Item){
        item.circleBody.delete()
    }

    fun createBorders() {
        borders = arrayListOf(
                Border(world, Vec2(0f, 0.5f / scaleY), Border.HORIZONTAL),
                Border(world, Vec2(0f, -1.0f / scaleY), Border.HORIZONTAL),
                Border(world, Vec2(-0.5f, 0f / scaleY), Border.VERTICAL),
                Border(world, Vec2(0.5f, 0f / scaleY), Border.VERTICAL)
        )
    }

    private fun move(body: CircleBody) {
        body.physicalBody.apply {
            body.isVisible = centerImmediately.not()
            val direction = gravityCenter.sub(position)
            val distance = direction.length()
            val gravity = if (body.increased) 1.3f * currentGravity else currentGravity
            if (distance > step * 200) {
                applyForce(direction.mul(gravity / distance.sqr()), position)
            }
        }
    }

    private fun interpolate(start: Float, end: Float, f: Float) = start + f * (end - start)

}