package com.devorion.mvitest.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.badoo.mvicore.android.lifecycle.CreateDestroyBinderLifecycle
import com.badoo.mvicore.binder.Binder
import com.devorion.mvitest.R
import com.jakewharton.rxbinding2.view.touches
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*

class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val binder = Binder(CreateDestroyBinderLifecycle(lifecycle))

        val feature = GameFeature(resources.displayMetrics.widthPixels, 3)

        binder.bind(feature to Consumer {
            Log.d("Test", it.toString())
            when (it.gameState) {
                GameState.ReadyToStart -> showReadyToStart(it)
                GameState.CountingDown -> updateCountdown(it)
                GameState.WaitingToShowSquare -> clearAndShowStreak(it)
                GameState.ShowingSquare -> gameBoard.drawSquare(it.squareRect)
                GameState.GameOver -> showGameOver(it)
            }
        })

        gameBoard.touches().map { Wish.BoardClick(it.x, it.y) }.subscribe(feature)
    }

    private fun showReadyToStart(state: State) {
        countdown.text = ""
        streak.text = ""
        status.text = "Touch to start\nHighest Streak = ${state.highestStreak}"
        gameBoard.clearSquare()
    }

    private fun updateCountdown(state: State) {
        countdown.text = state.countdownValue.toString()
        streak.text = ""
        status.text = ""
    }

    private fun clearAndShowStreak(state: State) {
        gameBoard.clearSquare()
        countdown.text = ""
        streak.text = getStreakString(state.streak)
        status.text = ""
    }

    private fun showGameOver(it: State) {
        streak.text = ""
        status.text = "Game Over"
    }

    private fun getStreakString(streak: Int): String = "Streak: $streak"
}
