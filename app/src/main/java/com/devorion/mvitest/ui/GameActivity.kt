package com.devorion.mvitest.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.badoo.mvicore.android.lifecycle.CreateDestroyBinderLifecycle
import com.badoo.mvicore.binder.Binder
import com.devorion.mvitest.R
import com.devorion.mvitest.storage.GameStorage
import com.jakewharton.rxbinding2.view.touches
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class GameActivity : AppCompatActivity(), ObservableSource<Wish> {
    private val gameStorage: GameStorage by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val binder = Binder(CreateDestroyBinderLifecycle(lifecycle))
        val feature = GameFeature(
            gameStorage,
            resources.displayMetrics.widthPixels,
            3
        )
        binder.bind(feature to Consumer {
            Log.d("State", it.toString())
            when (it.gameState) {
                GameState.ReadyToStart -> showReadyToStart(it)
                GameState.CountingDown -> updateCountdown(it)
                GameState.WaitingToShowSquare -> clearAndShowStreak(it)
                GameState.ShowingSquare -> gameBoard.drawSquare(it.squareRect)
                GameState.GameOver -> showGameOver(it)
            }
        })

        binder.bind(this to feature)
    }

    override fun subscribe(observer: Observer<in Wish>) {
        gameBoard.touches().map { Wish.BoardPress(it.x, it.y) }.subscribe(observer)
    }

    private fun showReadyToStart(state: State) {
        countdown.text = "Press To Start"
        status.text = "Touch Squares to Increase Streak\nHighest Streak = ${state.highestStreak}"
        gameBoard.clearSquare()
    }

    private fun updateCountdown(state: State) {
        countdown.text = state.countdownValue.toString()
        status.text = ""
    }

    private fun clearAndShowStreak(state: State) {
        gameBoard.clearSquare()
        countdown.text = ""
        status.text = "Streak: ${state.streak}"
    }

    private fun showGameOver(it: State) {
        val sb = StringBuilder("Game Over\n").apply {
            if (it.streak == it.highestStreak) {
                append("New High Streak: ${it.streak}!")
            } else {
                append("Highest Streak: ${it.highestStreak}")
            }
        }
        countdown.text = "Press To Try Again"
        status.text = sb.toString()
        gameBoard.clearSquare()
    }
}
