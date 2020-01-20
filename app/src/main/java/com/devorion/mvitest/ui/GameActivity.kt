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
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class GameActivity : AppCompatActivity(), ObservableSource<Wish>, Consumer<State> {
    private val gameStorage: GameStorage by inject()
    private val wishPublisher: PublishSubject<Wish> = PublishSubject.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val binder = Binder(CreateDestroyBinderLifecycle(lifecycle))
        val feature = GameFeature(
            gameStorage,
            resources.displayMetrics.widthPixels,
            3
        )

        binder.bind(feature to this)
        binder.bind(this to feature)
        gameBoard.touches().map { Wish.BoardPress(it.x, it.y) }.subscribe(wishPublisher)
    }

    override fun subscribe(observer: Observer<in Wish>) {
        wishPublisher.subscribe(observer)
    }

    override fun accept(state: State) {
        Log.d("State", state.toString())
        when (state.gameState) {
            GameState.ReadyToStart -> showReadyToStart(state)
            GameState.CountingDown -> updateCountdown(state)
            GameState.WaitingToShowSquare -> clearAndShowStreak(state)
            GameState.ShowingSquare -> gameBoard.drawSquare(state.squareRect)
            GameState.GameOver -> showGameOver(state)
        }
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

    override fun onPause() {
        super.onPause()
        wishPublisher.onNext(Wish.Pause)
    }

    override fun onResume() {
        super.onResume()
        wishPublisher.onNext(Wish.Resume)
    }
}
