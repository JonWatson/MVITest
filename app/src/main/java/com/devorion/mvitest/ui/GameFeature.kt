package com.devorion.mvitest.ui

import android.graphics.Rect
import com.badoo.mvicore.element.Actor
import com.badoo.mvicore.element.PostProcessor
import com.badoo.mvicore.element.Reducer
import com.badoo.mvicore.feature.BaseFeature
import com.devorion.mvitest.storage.GameStorage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class GameFeature(
    gameStorage: GameStorage,
    boardSize: Int,
    countdownLength: Int
) : BaseFeature<Wish, Action, Effect, State, Nothing>(
    initialState = State(
        GameState.ReadyToStart,
        0,
        gameStorage.highestStreak,
        null,
        null
    ),
    wishToAction = { wish ->
        when (wish) {
            is Wish.BoardPress -> Action.BoardPress(
                wish.x.toInt(),
                wish.y.toInt()
            )
        }
    },
    actor = GameActor(
        gameStorage,
        boardSize,
        countdownLength
    ),
    reducer = GameReducer(),
    postProcessor = GamePostProcessor()
) {

    // Actor
    class GameActor(
        private val gameStorage: GameStorage,
        private val boardSize: Int,
        private val countdownLength: Int
    ) : Actor<State, Action, Effect> {
        var squareShowDisposable: Disposable? = null

        override fun invoke(state: State, action: Action): Observable<out Effect> {
            return when (action) {
                is Action.BoardPress -> {
                    when (state.gameState) {
                        GameState.ReadyToStart, GameState.GameOver -> startCountdown()
                        GameState.CountingDown -> Observable.empty()
                        GameState.WaitingToShowSquare -> Observable.empty()
                        GameState.ShowingSquare -> checkBoardPress(state, action)
                    }
                }
                Action.StartSquareDelay ->
                    // After a short time, display the square(gives the user a break between squares)
                    Observable.timer(
                        getSquareShowDelay(state),
                        TimeUnit.MILLISECONDS
                    ).map {
                        Effect.DrawSquare(
                            getRandomSquareRect(
                                getNewSquareSize(state)
                            )
                        )
                    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                Action.StartSquareDuration ->
                    // Start a timer for how long the user has to click the square.  Game Over if Fail Effect triggers
                    Observable.timer(
                        getSquareShowDuration(state),
                        TimeUnit.MILLISECONDS
                    ).map {
                        gameStorage.highestStreak = max(gameStorage.highestStreak, state.streak)
                        Effect.Fail
                    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            // Store the disposable so we can cancel it if the user presses the square in time
                            squareShowDisposable = it
                        }
            }
        }

        // When starting a new game, a countdown is displayed before the first square is shown
        private fun startCountdown(): Observable<out Effect> {
            return Observables.zip(
                Observable.range(0, countdownLength + 1),
                Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
            ) { countdownVal: Int, _ ->
                countdownLength - countdownVal
            }.map {
                if (it == 0) {
                    Effect.GameStarted
                } else {
                    Effect.UpdateStartCountdownValue(it)
                }
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }

        // Check if the users press falls within the bounds of the shown square
        private fun checkBoardPress(
            state: State,
            boardPressAction: Action.BoardPress
        ): Observable<out Effect> {
            return if (state.squareRect != null &&
                state.squareRect.contains(boardPressAction.x, boardPressAction.y)
            ) {
                squareShowDisposable?.dispose()
                Observable.just(Effect.Success)
            } else {
                Observable.empty()
            }
        }

        // Determine size of the next square.
        // This can be used to manage game difficulty as a streak grows
        private fun getNewSquareSize(state: State): Int {
            return max(((boardSize / 8) - state.streak), boardSize / 18)
        }

        // Pick a random point on the board to show the next square
        private fun getRandomSquareRect(
            squareSize: Int
        ): Rect {
            val rand = Random(System.currentTimeMillis())
            val x = rand.nextInt(boardSize - squareSize)
            val y = rand.nextInt(boardSize - squareSize)
            return Rect(x, y, x + squareSize, y + squareSize)
        }

        // Determine the delay between a successful hit and the next square being shown.
        // This can be used to manage game difficulty as a streak grows
        private fun getSquareShowDelay(state: State): Long {
            return max(1250 - state.streak * 10, 500).toLong()
        }

        // Determine the time the player has to press the square being shown.
        // This can be used to manage game difficulty as a streak grows
        private fun getSquareShowDuration(state: State): Long {
            return max(1250 - state.streak * 10, 500).toLong()
        }
    }

    // Reducer
    class GameReducer : Reducer<State, Effect> {
        override fun invoke(
            state: State,
            effect: Effect
        ): State {
            return when (effect) {
                Effect.GameStarted ->
                    state.copy(
                        gameState = GameState.WaitingToShowSquare,
                        streak = 0
                    )
                is Effect.UpdateStartCountdownValue ->
                    state.copy(
                        gameState = GameState.CountingDown,
                        countdownValue = effect.countdownValue
                    )
                is Effect.DrawSquare ->
                    state.copy(
                        gameState = GameState.ShowingSquare,
                        squareRect = effect.squareRect
                    )
                Effect.Success ->
                    state.copy(
                        gameState = GameState.WaitingToShowSquare,
                        streak = state.streak + 1,
                        squareRect = null
                    )
                Effect.Fail ->
                    state.copy(
                        gameState = GameState.GameOver,
                        squareRect = null,
                        highestStreak = max(state.streak, state.highestStreak)
                    )

            }
        }
    }

    // Post Processor
    class GamePostProcessor : PostProcessor<Action, Effect, State> {
        override fun invoke(action: Action, effect: Effect, state: State): Action? {
            return when {
                // These effects need to trigger an action that starts the delay between square draws
                effect is Effect.GameStarted || effect is Effect.Success -> Action.StartSquareDelay
                // If we drew the square, need to trigger an action that starts the timer for how long the user has to press the square
                effect is Effect.DrawSquare && state.gameState == GameState.ShowingSquare -> Action.StartSquareDuration
                else -> null
            }
        }
    }
}

data class State(
    val gameState: GameState,
    val streak: Int,
    val highestStreak: Int,
    val squareRect: Rect?,
    val countdownValue: Int?
)

sealed class GameState {
    object ReadyToStart : GameState()
    object CountingDown : GameState()
    object WaitingToShowSquare : GameState()
    object ShowingSquare : GameState()
    object GameOver : GameState()
}

sealed class Wish {
    data class BoardPress(val x: Float, val y: Float) : Wish()
}

sealed class Action {
    data class BoardPress(val x: Int, val y: Int) : Action()
    object StartSquareDelay : Action()
    object StartSquareDuration : Action()
}

sealed class Effect {
    data class UpdateStartCountdownValue(val countdownValue: Int) : Effect()
    data class DrawSquare(val squareRect: Rect) : Effect()
    object GameStarted : Effect()
    object Success : Effect()
    object Fail : Effect()
}