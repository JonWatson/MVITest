package com.devorion.mvitest.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.badoo.mvicore.android.lifecycle.CreateDestroyBinderLifecycle
import com.badoo.mvicore.binder.Binder
import com.devorion.mvitest.R
import com.devorion.mvitest.storage.GameStorage
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class GameActivity : AppCompatActivity(), ObservableSource<Wish>, Consumer<State> {
    private val gameStorage: GameStorage by inject()
    private val wishPublisher: PublishSubject<Wish> = PublishSubject.create()
    private val gameViewModel: GameViewModel by lazy {
        ViewModelProvider(
            this,
            GameViewModelFactory(gameStorage)
        ).get(GameViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val binder = Binder(CreateDestroyBinderLifecycle(lifecycle))

        binder.bind(gameViewModel.gameFeature to this)
        binder.bind(this to gameViewModel.gameFeature)
        gameBoard.touchObservable.map {
            Wish.BoardPress(
                it.x, it.y
            )
        }.subscribe(wishPublisher)
    }

    class GameViewModelFactory(private val gameStorage: GameStorage) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GameViewModel(
                GameFeature(
                    gameStorage,
                    RELATIVE_BOARD_SIZE,
                    COUNT_DOWN_MAX
                )
            ) as T
        }
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
            GameState.ShowingSquare -> showSquare(state)
            GameState.GameOver -> showGameOver(state)
        }
    }

    private fun showSquare(state: State) {
        countdown.text = ""
        status.text = getStreakString(state)
        gameBoard.drawSquare(
            state.relativeSquareRect,
            state.relativeBoardSize
        )
    }

    private fun showReadyToStart(state: State) {
        countdown.text = "Press To Start"
        status.text = "Touch Squares to Increase Streak\nHighest Streak = ${state.highestStreak}"
        gameBoard.clearSquare()
    }

    private fun updateCountdown(state: State) {
        countdown.text = state.countdownValue.toString()
        status.text = ""
        gameBoard.clearSquare()
    }

    private fun clearAndShowStreak(state: State) {
        countdown.text = ""
        status.text = getStreakString(state)
        gameBoard.clearSquare()
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

    private fun getStreakString(state: State) = "Streak: ${state.streak}"

    override fun onPause() {
        super.onPause()
        wishPublisher.onNext(Wish.Pause)
    }

    override fun onResume() {
        super.onResume()
        wishPublisher.onNext(Wish.Resume)
    }

    companion object {
        const val RELATIVE_BOARD_SIZE = 100
        const val COUNT_DOWN_MAX = 3
    }
}
