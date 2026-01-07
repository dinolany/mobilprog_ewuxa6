package com.example.shoppinglistapp.ui

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shoppinglistapp.data.AppDatabase
import com.example.shoppinglistapp.data.ListItem
import com.example.shoppinglistapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class ItemsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var listId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listId = intent.getIntExtra("LIST_ID", -1)
        val listName = intent.getStringExtra("LIST_NAME")

        // --- UI BEÁLLÍTÁSA ---
        binding.tvHeader.text = listName ?: "Termékek"
        binding.btnBack.visibility = View.VISIBLE

        // FONTOS: Statisztikák elrejtése ebben az activity-ben
        binding.statsContainer.visibility = View.GONE

        // RecyclerView láthatóvá tétele
        binding.rvToBuy.visibility = View.VISIBLE

        // Egyéb főoldali elemek elrejtése
        binding.tvToBuyLabel.visibility = View.GONE
        binding.tvBoughtLabel.visibility = View.GONE
        binding.rvBought.visibility = View.GONE
        binding.rvStores.visibility = View.GONE

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()

        database.shoppingDao().getItemsForList(listId).observe(this) { allItems ->
            (binding.rvToBuy.adapter as ListingAdapter).setData(allItems)
        }

        binding.fabAdd.setOnClickListener { showItemDialog(null) }
    }

    private fun setupRecyclerView() {
        binding.rvToBuy.layoutManager = GridLayoutManager(this, 2)
        binding.rvToBuy.adapter = createAdapter()
    }

    private fun createAdapter(): ListingAdapter {
        return ListingAdapter(
            onDelete = { item -> lifecycleScope.launch { database.shoppingDao().deleteItem(item) } },
            onStatusChange = { item -> lifecycleScope.launch { database.shoppingDao().updateItem(item) } },
            onEditClick = { item -> showItemDialog(item) }
        )
    }

    private fun showItemDialog(item: ListItem?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (item == null) "Új termék hozzáadása" else "Termék módosítása")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 30, 60, 30)
        }

        val nameInput = EditText(this).apply {
            hint = "Termék neve"; setText(item?.name ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val qtyInput = EditText(this).apply {
            hint = "Mennyiség"; inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(item?.quantity?.toString() ?: "")
        }
        val unitInput = EditText(this).apply {
            hint = "Egység (db, kg...)"; setText(item?.unit ?: "db")
        }
        val ctgoryInput = EditText(this).apply {
            hint = "Kategória"; setText(item?.category ?: "")
        }
        val isUrgentInput = CheckBox(this).apply {
            text = "Sürgős tétel"; isChecked = item?.isUrgent ?: false
        }

        layout.addView(nameInput); layout.addView(qtyInput); layout.addView(unitInput)
        layout.addView(ctgoryInput); layout.addView(isUrgentInput)
        builder.setView(layout)

        builder.setPositiveButton("Mentés") { _, _ ->
            val name = nameInput.text.toString()
            if (name.isNotEmpty()) {
                val newItem = ListItem(
                    id = item?.id ?: 0,
                    listId = listId,
                    name = name,
                    category = ctgoryInput.text.toString(),
                    quantity = qtyInput.text.toString().toIntOrNull() ?: 1,
                    unit = unitInput.text.toString(),
                    isUrgent = isUrgentInput.isChecked,
                    isBought = item?.isBought ?: false
                )
                lifecycleScope.launch {
                    if (item == null) database.shoppingDao().insertItem(newItem)
                    else database.shoppingDao().updateItem(newItem)
                }
            }
        }
        builder.setNegativeButton("Mégse", null)
        builder.show()
    }
}