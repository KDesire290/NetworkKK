package ci.miage.mob.networkKK.controller

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ci.miage.mob.networkKK.R
import ci.miage.mob.networkKK.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var btnCancelConnection: Button
    private var isConnectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        btnCancelConnection = binding.root.findViewById(R.id.btn_cancel_connection)
        btnCancelConnection.setOnClickListener {
            binding.graphView.setConnectionMode(false)
            isConnectionMode = false
            btnCancelConnection.visibility = Button.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add_node -> {
                showAddNodeDialog()
                true
            }
            R.id.menu_reset_graph -> {
                binding.graphView.resetAll()
                true
            }
            R.id.menu_add_connection -> {
                binding.graphView.setConnectionMode(true)
                isConnectionMode = true
                btnCancelConnection.visibility = Button.VISIBLE
                true
            }
            R.id.menu_save_network -> {
                showSaveNetworkDialog()
                true
            }
            R.id.menu_load_network -> {
                showLoadNetworkDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddNodeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_node, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Ajouter un nœud")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val nodeName = dialogView.findViewById<EditText>(R.id.editTextNodeName).text.toString()
                if (nodeName.isNotEmpty()) {
                    binding.graphView.addNode(nodeName)
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    private fun showSaveNetworkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_network, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Sauvegarder le réseau")
            .setView(dialogView)
            .setPositiveButton("Sauvegarder") { _, _ ->
                val filename = dialogView.findViewById<EditText>(R.id.editTextFileName).text.toString()
                if (filename.isNotEmpty()) {
                    if (binding.graphView.saveNetworkToFile(filename)) {
                        Toast.makeText(this, "Réseau sauvegardé", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }

    private fun showLoadNetworkDialog() {
        val files = filesDir.listFiles()?.filter { it.isFile }?.map { it.name }
        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "Aucun réseau sauvegardé", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Charger un réseau")
            .setItems(files.toTypedArray()) { _, which ->
                val filename = files[which]
                if (binding.graphView.loadNetworkFromFile(filename)) {
                    Toast.makeText(this, "Réseau chargé", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erreur lors du chargement", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()
        dialog.show()
    }
}