package com.example.shoppinglistapp.ui

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglistapp.R
import com.example.shoppinglistapp.data.ListItem

class ListingAdapter(
    private val onDelete: (ListItem) -> Unit,
    private val onStatusChange: (ListItem) -> Unit,
    private val onEditClick: (ListItem) -> Unit
) : RecyclerView.Adapter<ListingAdapter.ShoppingViewHolder>() {

    private var items = emptyList<ListItem>()

    // ViewHolder frissítve az új XML ID-khoz és a CardView-hoz
    class ShoppingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvQty: TextView = view.findViewById(R.id.tvQuantity)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val cbBought: CheckBox = view.findViewById(R.id.cbBought)
        val btnDel: ImageButton = view.findViewById(R.id.btnDelete)
        val cardView: CardView = view.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ShoppingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val current = items[position]

        holder.tvName.text = current.name
        holder.tvQty.text = "${current.quantity} ${current.unit}"
        holder.tvCategory.text = current.category

        // --- VIZUÁLIS MEGJELENÉS ÉS RENDEZÉS LOGIKÁJA ---

        if (current.isBought) {
            // 1. KIPPIPÁLT ÁLLAPOT: Halványszürke, áthúzott szöveg
            holder.cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5")) // Világosszürke csempe
            holder.cardView.alpha = 0.5f // Halványítás (átlátszóság)

            holder.tvName.paintFlags = holder.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvName.setTextColor(Color.GRAY)
            holder.tvCategory.setTextColor(Color.GRAY)
        } else {
            // 2. AKTÍV ÁLLAPOT: Normál vagy Sürgős
            holder.cardView.alpha = 1.0f
            holder.tvName.paintFlags = holder.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvCategory.setTextColor(Color.DKGRAY)

            if (current.isUrgent) {
                // Sürgős: Piros kiemelés
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Halvány piros csempe
                holder.tvName.setTextColor(Color.RED)
            } else {
                // Normál: Fehér csempe
                holder.cardView.setCardBackgroundColor(Color.WHITE)
                holder.tvName.setTextColor(Color.BLACK)
            }
        }

        // CheckBox eseménykezelő
        holder.cbBought.setOnCheckedChangeListener(null)
        holder.cbBought.isChecked = current.isBought
        holder.cbBought.setOnCheckedChangeListener { _, isChecked ->
            onStatusChange(current.copy(isBought = isChecked))
        }

        // Kattintás a csempére a szerkesztéshez
        holder.cardView.setOnClickListener { onEditClick(current) }

        // Törlés gomb
        holder.btnDel.setOnClickListener { onDelete(current) }
    }

    override fun getItemCount() = items.size

    // ADATFRISSÍTÉS ÉS RENDEZÉS
    fun setData(newItems: List<ListItem>) {
        // Rendezés:
        // 1. isBought (false < true, tehát a meg nem vettek kerülnek előre)
        // 2. isUrgent (a meg nem vetteken belül a sürgősek legyenek legfelül)
        this.items = newItems.sortedWith(
            compareBy<ListItem> { it.isBought }
                .thenByDescending { it.isUrgent }
                .thenBy { it.name }
        )
        notifyDataSetChanged()
    }
}