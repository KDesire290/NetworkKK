package ci.miage.mob.networkKK.model
import android.graphics.Color
import org.json.JSONObject

class Graph {
    data class Node(val id: Int, var x: Float, var y: Float, var name: String)
    data class Connection(
        val from: Node,
        val to: Node,
        var label: String,
        var color: Int = Color.BLACK,
        var thickness: Float = 5f
    )

    val nodes = mutableListOf<Node>()
    val connections = mutableListOf<Connection>()

    fun addNode(x: Float, y: Float, name: String): Boolean {
        val id = nodes.size + 1
        val newNode = Node(id, x, y, name)

        if (nodes.any { Math.hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= 50 }) {
            return false
        }

        nodes.add(newNode)
        return true
    }

    fun addConnection(from: Node, to: Node, label: String) {
        if (!connections.any { it.from == from && it.to == to }) {
            connections.add(Connection(from, to, label))
        }
    }

    fun removeNode(node: Node) {
        nodes.remove(node)
        connections.removeAll { it.from == node || it.to == node }
    }

    fun clear() {
        nodes.clear()
        connections.clear()
    }

    fun toJson(): String {
        val nodesJson = nodes.joinToString(",", "[", "]") { node ->
            """{"id":${node.id},"x":${node.x},"y":${node.y},"name":"${node.name}"}"""
        }
        val connectionsJson = connections.joinToString(",", "[", "]") { conn ->
            """{"from":${conn.from.id},"to":${conn.to.id},"label":"${conn.label}","color":${conn.color},"thickness":${conn.thickness}}"""
        }
        return """{"nodes":$nodesJson,"connections":$connectionsJson}"""
    }

    fun fromJson(json: String) {
        val jsonObject = JSONObject(json)
        val nodesJson = jsonObject.getJSONArray("nodes")
        val connectionsJson = jsonObject.getJSONArray("connections")

        clear()

        for (i in 0 until nodesJson.length()) {
            val nodeJson = nodesJson.getJSONObject(i)
            nodes.add(
                Node(
                    nodeJson.getInt("id"),
                    nodeJson.getDouble("x").toFloat(),
                    nodeJson.getDouble("y").toFloat(),
                    nodeJson.getString("name")
                )
            )
        }

        for (i in 0 until connectionsJson.length()) {
            val connJson = connectionsJson.getJSONObject(i)
            val from = nodes.find { it.id == connJson.getInt("from") }!!
            val to = nodes.find { it.id == connJson.getInt("to") }!!
            connections.add(
                Connection(
                    from, to,
                    connJson.getString("label"),
                    connJson.optInt("color", Color.BLACK),
                    connJson.optDouble("thickness", 5.0).toFloat()
                )
            )
        }
    }
}