package com.devorion.mvitest.ui

import android.graphics.Rect
import com.badoo.mvicore.element.Actor
import com.badoo.mvicore.element.PostProcessor
import com.badoo.mvicore.element.Reducer
import com.badoo.mvicore.feature.BaseFeature
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class GameFeature(
    boardSize: Int,
    countdownLength: Int
) : BaseFeature<Wish, Action, Effect, State, Nothing>(
    initialState = State(
        GameState.ReadyToStart,
        0,
        0,  // TODO persistence
        null,
        null
    ),
    wishToAction = { wish ->
        when (wish) {
            is Wish.BoardClick -> Action.BoardClick(
                wish.x.toInt(),
                wish.y.toInt()
            )
        }
    },
    actor = GameActor(
        boardSize,
        countdownLength
    ),
    reducer = GameReducer(),
    postProcessor = GamePostProcessor()
) {

    // Actor
    class GameActor(
        private val boardSize: Int,
        private val countdownLength: Int
    ) : Actor<State, Action, Effect> {
        var squareShowDisposable: Disposable? = null

        override fun invoke(state: State, action: Action): Observable<out Effect> {
            return when (action) {
                is Action.BoardClick -> {
                    when (state.gameState) {
                        GameState.ReadyToStart -> startCountdown()
                        GameState.CountingDown -> Observable.empty()
                        GameState.WaitingToShowSquare -> Observable.empty()
                        GameState.ShowingSquare -> checkBoardClick(state, action)
                        GameState.GameOver -> Observable.just(Effect.RestartGame)
                    }
                }
                Action.StartSquareDelay ->
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
                    Observable.timer(
                        getSquareShowDuration(state),
                        TimeUnit.MILLISECONDS
                    ).map {
                        Effect.Fail
                    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            squareShowDisposable = it
                        }
            }
        }

        private fun startCountdown(): Observable<out Effect> {
            return Observable.zip(Observable.range(0, countdownLength + 1),
                Observable.interval(0, 1000, TimeUnit.MILLISECONDS),
                BiFunction<Int, Long, Int> { countdownVal, _ ->
                    countdownLength - countdownVal
                })
                .map {
                    Effect.UpdateStartCountdownValue(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

        private fun checkBoardClick(
            state: State,
            boardClickAction: Action.BoardClick
        ): Observable<out Effect> {
            return if (state.squareRect != null &&
                state.squareRect.contains(boardClickAction.x, boardClickAction.y)
            ) {
                squareShowDisposable?.dispose()
                Observable.just(Effect.Success)
            } else {
                Observable.empty()
            }
        }

        private fun getNewSquareSize(state: State): Int {
            return max(((boardSize / 10) - state.streak), boardSize / 20)
        }

        private fun getRandomSquareRect(
            squareSize: Int
        ): Rect {
            val rand = Random(System.currentTimeMillis())
            val x = rand.nextInt(boardSize - squareSize)
            val y = rand.nextInt(boardSize - squareSize)
            return Rect(x, y, x + squareSize, y + squareSize)
        }

        private fun getSquareShowDelay(state: State): Long {
            return max(1250 - state.streak * 10, 500).toLong()
        }

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
                Effect.RestartGame ->
                    state.copy(
                        gameState = GameState.ReadyToStart,
                        streak = 0,
                        squareRect = null
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
                Effect.Fail -> state.copy(gameState = GameState.GameOver)

            }
        }
    }

    // Post Processor
    class GamePostProcessor : PostProcessor<Action, Effect, State> {
        override fun invoke(action: Action, effect: Effect, state: State): Action? {
            return when {
                effect is Effect.UpdateStartCountdownValue && state.countdownValue == 0 ||
                        effect is Effect.Success -> Action.StartSquareDelay
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
    data class BoardClick(val x: Float, val y: Float) : Wish()
}

sealed class Action {
    data class BoardClick(val x: Int, val y: Int) : Action()
    object StartSquareDelay : Action()
    object StartSquareDuration : Action()
}

sealed class Effect {
    object RestartGame : Effect()
    data class UpdateStartCountdownValue(val countdownValue: Int) : Effect()
    data class DrawSquare(val squareRect: Rect) : Effect()
    object Success : Effect()
    object Fail : Effect()
}