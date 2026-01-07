package com.example.shoppinglistapp.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglistapp.R
import com.example.shoppinglistapp.data.Whislist
import java.text.SimpleDateFormat
import java.util.*

class StoreAdapter(
    private val onDelete: (Whislist) -> Unit,
    private val onClick: (Whislist) -> Unit
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    private var lists = emptyList<Whislist>()

    class StoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvListName)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDate)
        val tvProgress: TextView = view.findViewById(R.id.tvProgress)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteList)
        val cardView: CardView = view.findViewById(R.id.listCardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        val current = lists[position]
        holder.tvName.text = current.name
        holder.tvProgress.text = "${current.boughtItems}/${current.totalItems}"

        val isDone = current.totalItems > 0 && current.boughtItems == current.totalItems
        val isEmpty = current.totalItems == 0

        var isPast = false
        var isUrgent = false

        if (current.dueDate != "Nincs határidő") {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dueDate = sdf.parse(current.dueDate)
                val today = Date()

                if (dueDate != null) {
                    // Ha a határidő a mai napnál korábbi
                    if (dueDate.before(today)) {
                        isPast = true
                    }
                    // Sürgős, ha 3 napon belül van
                    val diff = dueDate.time - today.time
                    val diffDays = diff / (1000 * 60 * 60 * 24)
                    if (diffDays in 0..3) {
                        isUrgent = true
                    }
                }
            } catch (e: Exception) {}
        }

        // --- VIZUÁLIS PRIORITÁSOK ÉS SZÍNEZÉS ---
        when {
            // 1. HA LEJÁRT VAGY SÜRGŐS -> PIROS (Ez a legmagasabb prioritás)
            isPast || isUrgent -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
                holder.tvDueDate.setTextColor(Color.RED)
                holder.tvDueDate.text = "⚠️ SÜRGŐS/LEJÁRT: ${current.dueDate}"
                holder.cardView.alpha = 1.0f
            }

            // 2. HA ÜRES ÉS NEM SÜRGŐS -> SÁRGA
            isEmpty -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
                holder.tvDueDate.text = current.dueDate
                holder.tvDueDate.setTextColor(Color.GRAY)
                holder.cardView.alpha = 1.0f
            }

            // 3. HA KÉSZ -> SZÜRKE
            isDone -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"))
                holder.cardView.alpha = 0.6f
                holder.tvDueDate.text = current.dueDate
                holder.tvDueDate.setTextColor(Color.GRAY)
            }

            // 4. ALAPÉRTELMEZETT (FOLYAMATBAN) -> KÉK
            else -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#BBDEFB"))
                holder.tvDueDate.text = current.dueDate
                holder.tvDueDate.setTextColor(Color.GRAY)
                holder.cardView.alpha = 1.0f
            }
        }

        holder.btnDelete.setOnClickListener { onDelete(current) }
        holder.itemView.setOnClickListener { onClick(current) }
    }

    override fun getItemCount() = lists.size

    fun setData(newLists: List<Whislist>) {
        // Rendezés javítva: Sürgős/Lejárt legelőre, utána üres, végül a kész listák
        this.lists = newLists.sortedWith(
            compareByDescending<Whislist> {
                // Segédfüggvény nélkül is: aki sürgős, az kerül előre
                isUrgentLogic(it)
            }.thenBy {
                it.totalItems > 0 && it.boughtItems == it.totalItems // Készek hátra
            }.thenBy {
                it.dueDate
            }
        )
        notifyDataSetChanged()
    }


    private fun isUrgentLogic(list: Whislist): Boolean {
        if (list.dueDate == "Nincs határidő") return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(list.dueDate)
            d != null && (d.before(Date()) || (d.time - Date().time) <= 3 * 24 * 60 * 60 * 1000)
        } catch (e: Exception) { false }
    }
}