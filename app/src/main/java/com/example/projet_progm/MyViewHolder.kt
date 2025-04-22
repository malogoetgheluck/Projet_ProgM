package com.example.projet_progm
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameTextView: TextView = itemView.findViewById(R.id.row_game_name)
    val scoreTextView: TextView = itemView.findViewById(R.id.row_game_score)
    val playButton: Button = itemView.findViewById(R.id.row_game_play_button)
}



