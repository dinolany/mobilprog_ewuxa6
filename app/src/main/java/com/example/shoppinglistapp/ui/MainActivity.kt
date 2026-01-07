package com.example.shoppinglistapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglistapp.data.AppDatabase
import com.example.shoppinglistapp.data.Whislist
import com.example.shoppinglistapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var currentFilter: String = "ALL"
    private var allLists: List<Whislist> = emptyList()
    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Magyar lokalizáció kényszerítése
        val locale = Locale("hu", "HU")
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvHeader.text = "Saját listák:"
        binding.rvStores.visibility = View.VISIBLE
        binding.btnBack.visibility = View.GONE

        setupRecyclerView()
        binding.fabAdd.setOnClickListener { showAddStoreDialog() }
    }

    override fun onResume() {
        super.onResume()
        refreshListData() // Frissítés visszatéréskor
    }

    private fun refreshListData() {
        database.shoppingDao().getAllLists().observe(this) { lists ->
            lifecycleScope.launch {
                val updatedLists = lists.map { list ->
                    val items = database.shoppingDao().getItemsForListSync(list.id)
                    list.copy(
                        totalItems = items.size,
                        boughtItems = items.count { it.isBought }
                    )
                }
                updateStatistics(updatedLists) // Statisztikai sor frissítése
            }
        }
    }

    private fun updateStatistics(updatedLists: List<Whislist>) {
        this.allLists = updatedLists

        val totalCount = updatedLists.size
        val emptyCount = updatedLists.count { it.totalItems == 0 }
        val inProgressCount = updatedLists.count { it.totalItems > 0 && it.boughtItems < it.totalItems }
        val doneCount = updatedLists.count { it.totalItems > 0 && it.boughtItems == it.totalItems }

        // Sürgős számolása (nem üres és nem kész listák)
        val urgentCount = updatedLists.count { isUrgent(it) }

        // UI elemek szövegének beállítása
        binding.statTotal.text = "📊 Összes: $totalCount (Szűrő törlése)"
        binding.statEmpty.text = "🟡 Üres: $emptyCount"
        binding.statInProgress.text = "🔵 Folyamatban: $inProgressCount"
        binding.statUrgent.text = "🔴 Sürgős: $urgentCount"
        binding.statDone.text = "⚪ Kész: $doneCount"

        // Kattintási események szűréshez
        binding.statTotal.setOnClickListener { applyFilter("ALL") }
        binding.statEmpty.setOnClickListener { applyFilter("EMPTY") }
        binding.statInProgress.setOnClickListener { applyFilter("PROGRESS") }
        binding.statUrgent.setOnClickListener { applyFilter("URGENT") }
        binding.statDone.setOnClickListener { applyFilter("DONE") }

        applyFilter(currentFilter)
    }
    private fun isUrgent(list: Whislist): Boolean {
        if (list.dueDate == "Nincs határidő") return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(list.dueDate)
            // Ha lejárt VAGY 3 napon belül esedékes
            d != null && (d.before(Date()) || (d.time - Date().time) <= 3 * 24 * 60 * 60 * 1000)
        } catch (e: Exception) { false }
    }


    private fun applyFilter(filter: String) {
        currentFilter = filter
        val filteredList = when (filter) {
            "EMPTY" -> allLists.filter { it.totalItems == 0 }
            "PROGRESS" -> allLists.filter { it.totalItems > 0 && it.boughtItems < it.totalItems }
            "DONE" -> allLists.filter { it.totalItems > 0 && it.boughtItems == it.totalItems }
            "URGENT" -> allLists.filter { isUrgent(it) }
            else -> allLists
        }

        (binding.rvStores.adapter as StoreAdapter).setData(filteredList)

        updateFilterUI(filter)
    }

    private fun setupRecyclerView() {
        val adapter = StoreAdapter(
            onDelete = { list -> lifecycleScope.launch { database.shoppingDao().deleteList(list) } },
            onClick = { list -> handleListClick(list) }
        )
        binding.rvStores.layoutManager = LinearLayoutManager(this)
        binding.rvStores.adapter = adapter
    }

    private fun handleListClick(list: Whislist) {
        val isEmpty = list.totalItems == 0
        val urgent = isUrgent(list) || (list.dueDate != "Nincs határidő" && isPastDue(list.dueDate))

        if (isEmpty && urgent) {
            AlertDialog.Builder(this)
                .setTitle("Ez most komoly?")
                .setMessage("Ez a lista üres és a határidő közeli/lejárt. Mit szeretnél tenni?")
                .setPositiveButton("Feltöltöm") { _, _ -> openItemsActivity(list) }
                .setNegativeButton("Inkább töröld") { _, _ ->
                    lifecycleScope.launch { database.shoppingDao().deleteList(list) }
                }
                .show()
        } else {
            openItemsActivity(list)
        }
    }

    private fun isPastDue(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(dateStr)
            d != null && d.before(Date())
        } catch (e: Exception) { false }
    }

    private fun openItemsActivity(list: Whislist) {
        val intent = Intent(this, ItemsActivity::class.java).apply {
            putExtra("LIST_ID", list.id)
            putExtra("LIST_NAME", list.name)
        }
        startActivity(intent)
    }

    private fun showAddStoreDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 20, 60, 20)
        }
        val nameInput = EditText(this).apply { hint = "Lista neve" }
        val dateInput = EditText(this).apply {
            hint = "Határidő (opcionális)"
            isFocusable = false
            setOnClickListener {
                val cal = Calendar.getInstance()
                android.app.DatePickerDialog(this@MainActivity, { _, y, m, d ->
                    val date = "$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}"
                    setText(date)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        layout.addView(nameInput)
        layout.addView(dateInput)

        AlertDialog.Builder(this)
            .setTitle("Új lista hozzáadása")
            .setView(layout)
            .setPositiveButton("Hozzáadás") { _, _ ->
                val name = nameInput.text.toString()
                val date = dateInput.text.toString()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        database.shoppingDao().insertList(Whislist(
                            name = name,
                            dueDate = if (date.isEmpty()) "Nincs határidő" else date
                        ))
                    }
                }
            }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun updateFilterUI(filter: String) {
        val normal = android.graphics.Typeface.NORMAL
        val bold = android.graphics.Typeface.BOLD

        // Szövegstílus frissítése (félkövér a kiválasztott)
        binding.statTotal.setTypeface(null, if (filter == "ALL") bold else normal)
        binding.statEmpty.setTypeface(null, if (filter == "EMPTY") bold else normal)
        binding.statInProgress.setTypeface(null, if (filter == "PROGRESS") bold else normal)
        binding.statUrgent.setTypeface(null, if (filter == "URGENT") bold else normal)
        binding.statDone.setTypeface(null, if (filter == "DONE") bold else normal)

        // Opcionális: Szövegszín beállítása a kért prioritások szerint
        binding.statUrgent.setTextColor(if (filter == "URGENT") android.graphics.Color.RED else android.graphics.Color.BLACK)
        binding.statEmpty.setTextColor(if (filter == "EMPTY") android.graphics.Color.parseColor("#FBC02D") else android.graphics.Color.BLACK)
    }
}