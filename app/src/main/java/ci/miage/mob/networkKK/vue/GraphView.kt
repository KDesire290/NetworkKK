package ci.miage.mob.networkKK.vue


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import ci.miage.mob.networkKK.R
import ci.miage.mob.networkKK.model.Graph
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val graph = Graph()
    private val nodeRadius = 50f

    // Paints
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val selectedNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 0, 255, 0)
        style = Paint.Style.FILL
    }

    private val connectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val selectedConnectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 0, 0, 255)
        strokeWidth = 15f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    // États
    private var backgroundImage: Bitmap? = null
    private val backgroundPaint = Paint()
    private var selectedNode: Graph.Node? = null
    private var selectedConnection: Graph.Connection? = null
    private var draggedNode: Graph.Node? = null
    private var isConnectionMode = false
    private var tempLineStart: Pair<Float, Float>? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            handleLongPress(e.x, e.y)
        }
    })

        //gestion images
    init {

        try {
            backgroundImage = BitmapFactory.decodeResource(resources, R.drawable.house_plan3)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        backgroundImage = backgroundImage?.let { original ->
            Bitmap.createScaledBitmap(original, w, h, true)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundImage?.let { bitmap ->

            val destRect = Rect(0, 0, width, height)
            canvas.drawBitmap(bitmap, null, destRect, backgroundPaint)
        } ?: run {

            canvas.drawColor(Color.LTGRAY)
        }
    }



    override fun onDraw(canvas: Canvas) {

        drawBackground(canvas)

        super.onDraw(canvas)


        graph.connections.forEach { connection ->
            val paint = if (connection == selectedConnection) selectedConnectionPaint else connectionPaint
            paint.color = connection.color
            paint.strokeWidth = connection.thickness

            canvas.drawLine(
                connection.from.x, connection.from.y,
                connection.to.x, connection.to.y,
                paint
            )

            // Étiquette
            val midX = (connection.from.x + connection.to.x) / 2
            val midY = (connection.from.y + connection.to.y) / 2
            canvas.drawText(connection.label, midX, midY, textPaint)
        }

        // Dessin des nœuds
        graph.nodes.forEach { node ->
            if (node == selectedNode) {
                canvas.drawCircle(node.x, node.y, nodeRadius + 15, selectedNodePaint)
            }

            canvas.drawCircle(node.x, node.y, nodeRadius, nodePaint)
            canvas.drawText(node.name, node.x, node.y - nodeRadius - 10, textPaint)
        }

        // Dessin de la ligne temporaire
        tempLineStart?.let { (startX, startY) ->
            connectionPaint.color = Color.GRAY
            canvas.drawLine(startX, startY, lastTouchX, lastTouchY, connectionPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        lastTouchX = event.x
        lastTouchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchedNode = findNodeAt(event.x, event.y)
                if (touchedNode != null) {
                    if (isConnectionMode) {
                        tempLineStart = touchedNode.x to touchedNode.y
                    } else {
                        draggedNode = touchedNode
                    }
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isConnectionMode) {
                    invalidate()
                } else {
                    draggedNode?.let {
                        it.x = event.x
                        it.y = event.y
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isConnectionMode) {
                    val node = findNodeAt(event.x, event.y)
                    if (node != null && tempLineStart != null) {
                        showConnectionDialog(tempLineStart!!.first, tempLineStart!!.second, node.x, node.y)
                    }
                    tempLineStart = null
                } else {
                    draggedNode = null
                }
                invalidate()
            }
        }
        return true
    }

    private fun handleLongPress(x: Float, y: Float) {
        if (isConnectionMode) return

        val node = findNodeAt(x, y)
        val connection = findConnectionAt(x, y)

        when {
            node != null -> {
                selectedNode = node
                showNodeContextMenu(node)
            }
            connection != null -> {
                selectedConnection = connection
                showConnectionContextMenu(connection)
            }
            else -> {
                addNode("Nœud ${graph.nodes.size + 1}")
            }
        }
        invalidate()
    }

    private fun showNodeContextMenu(node: Graph.Node) {
        val popup = PopupMenu(context, this).apply {
            menuInflater.inflate(R.menu.menu_node_options, menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_delete_node -> {
                        graph.removeNode(node)
                        invalidate()
                        true
                    }
                    R.id.menu_edit_node -> {
                        showEditNodeDialog(node)
                        true
                    }
                    R.id.menu_color_red -> { nodePaint.color = Color.RED; invalidate(); true }
                    R.id.menu_color_green -> { nodePaint.color = Color.GREEN; invalidate(); true }
                    R.id.menu_color_blue -> { nodePaint.color = Color.BLUE; invalidate(); true }
                    R.id.menu_color_orange -> { nodePaint.color = Color.parseColor("#FFA500"); invalidate(); true }
                    R.id.menu_color_cyan -> { nodePaint.color = Color.CYAN; invalidate(); true }
                    R.id.menu_color_magenta -> { nodePaint.color = Color.MAGENTA; invalidate(); true }
                    R.id.menu_color_black -> { nodePaint.color = Color.BLACK; invalidate(); true }
                    else -> false
                }
            }

            setOnDismissListener {
                selectedNode = null
                invalidate()
            }
        }

        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName("com.android.internal.view.menu.MenuPopupHelper")
                    val methodSetForceShowIcon = classPopupHelper.getMethod(
                        "setForceShowIcon",
                        Boolean::class.javaPrimitiveType
                    )
                    methodSetForceShowIcon.invoke(menuPopupHelper, true)

                    // Positionnement du menu
                    val methodShow = classPopupHelper.getMethod(
                        "show",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    val location = IntArray(2)
                    getLocationOnScreen(location)
                    methodShow.invoke(
                        menuPopupHelper,
                        (node.x + location[0]).toInt(),
                        (node.y - nodeRadius * 3 + location[1]).toInt()
                    )
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            popup.show()
        }
    }

    private fun showConnectionContextMenu(connection: Graph.Connection) {
        val popup = PopupMenu(context, this).apply {
            menuInflater.inflate(R.menu.menu_connection_options, menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_delete_connection -> {
                        graph.connections.remove(connection)
                        invalidate()
                        true
                    }
                    R.id.menu_edit_connection_label -> {
                        showEditConnectionDialog(connection)
                        true
                    }
                    R.id.menu_color_red -> { connection.color = Color.RED; invalidate(); true }
                    R.id.menu_color_green -> { connection.color = Color.GREEN; invalidate(); true }
                    R.id.menu_color_blue -> { connection.color = Color.BLUE; invalidate(); true }
                    R.id.menu_color_orange -> { connection.color = Color.parseColor("#FFA500"); invalidate(); true }
                    R.id.menu_color_cyan -> { connection.color = Color.CYAN; invalidate(); true }
                    R.id.menu_color_magenta -> { connection.color = Color.MAGENTA; invalidate(); true }
                    R.id.menu_color_black -> { connection.color = Color.BLACK; invalidate(); true }
                    R.id.menu_thickness_1 -> { connection.thickness = 1f; invalidate(); true }
                    R.id.menu_thickness_3 -> { connection.thickness = 3f; invalidate(); true }
                    R.id.menu_thickness_5 -> { connection.thickness = 5f; invalidate(); true }
                    R.id.menu_thickness_8 -> { connection.thickness = 8f; invalidate(); true }
                    R.id.menu_thickness_10 -> { connection.thickness = 10f; invalidate(); true }
                    else -> false
                }
            }

            setOnDismissListener {
                selectedConnection = null
                invalidate()
            }
        }


        val midX = (connection.from.x + connection.to.x) / 2
        val midY = (connection.from.y + connection.to.y) / 2


        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName("com.android.internal.view.menu.MenuPopupHelper")
                    val methodSetForceShowIcon = classPopupHelper.getMethod(
                        "setForceShowIcon",
                        Boolean::class.javaPrimitiveType
                    )
                    methodSetForceShowIcon.invoke(menuPopupHelper, true)


                    val methodShow = classPopupHelper.getMethod(
                        "show",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    val location = IntArray(2)
                    getLocationOnScreen(location)
                    methodShow.invoke(
                        menuPopupHelper,
                        (midX + location[0]).toInt(),
                        (midY - 100 + location[1]).toInt() // Ajustez le décalage (-100) selon vos besoins
                    )
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            popup.show()
        }

    }



    private fun showConnectionDialog(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val fromNode = findNodeAt(fromX, fromY)
        val toNode = findNodeAt(toX, toY)

        if (fromNode != null && toNode != null) {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_connection, null)
            val editText = view.findViewById<EditText>(R.id.editTextLabel)

            AlertDialog.Builder(context)
                .setTitle("Nouvelle connexion")
                .setView(view)
                .setPositiveButton("Créer") { _, _ ->
                    val label = editText.text.toString()
                    graph.addConnection(fromNode, toNode, label)
                    invalidate()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    private fun showEditNodeDialog(node: Graph.Node) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_node, null)
        val editText = view.findViewById<EditText>(R.id.editTextNodeName).apply {
            setText(node.name)
        }

        AlertDialog.Builder(context)
            .setTitle("Modifier le nœud")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                node.name = editText.text.toString()
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showEditConnectionDialog(connection: Graph.Connection) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_connection, null)
        val editText = view.findViewById<EditText>(R.id.editTextLabel).apply {
            setText(connection.label)
        }

        AlertDialog.Builder(context)
            .setTitle("Modifier la connexion")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                connection.label = editText.text.toString()
                invalidate()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun findNodeAt(x: Float, y: Float): Graph.Node? {
        return graph.nodes.find { node ->
            sqrt((node.x - x).pow(2) + (node.y - y).pow(2)) <= nodeRadius
        }
    }

    private fun findConnectionAt(x: Float, y: Float): Graph.Connection? {
        return graph.connections.find { connection ->
            val midX = (connection.from.x + connection.to.x) / 2
            val midY = (connection.from.y + connection.to.y) / 2
            sqrt((x - midX).pow(2) + (y - midY).pow(2)) < 50
        }
    }

    fun setConnectionMode(enabled: Boolean) {
        isConnectionMode = enabled
        tempLineStart = null
    }

    fun addNode(name: String) {
        val x = (Math.random() * width).toFloat()
        val y = (Math.random() * height).toFloat()
        if (graph.addNode(x, y, name)) {
            invalidate()
        }
    }

    fun resetConnections() {
        graph.connections.clear()
        invalidate()
    }

    fun resetAll(){
        graph.clear()
        invalidate()
    }

    fun saveNetworkToFile(filename: String): Boolean {
        return try {
            File(context.filesDir, filename).writeText(graph.toJson())
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadNetworkFromFile(filename: String): Boolean {
        return try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                graph.fromJson(file.readText())
                invalidate()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}