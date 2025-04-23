package com.example.projet_progm

import Games
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MyAdapter(
    private val games: List<Games>,
    private val onItemClick: (Games) -> Unit
) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gameitem, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val game = games[position]

        if (game.highScore == null) {
            holder.nameTextView.text = "Unknown Game"
            holder.scoreTextView.text = "???"
            holder.playButton.visibility = View.GONE
        } else {
            holder.nameTextView.text = game.gameName ?: "No name"
            holder.scoreTextView.text = "H.S : "+game.highScore.toString()
            holder.playButton.visibility = View.VISIBLE
        }

        holder.playButton.setOnClickListener {
            onItemClick(game)
        }
    }

    override fun getItemCount() = games.size
}
