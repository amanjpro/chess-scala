package me.amanj.zahak

import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit


object Evaluate {

  /**
    * Evaluates the board in its current state.
    *
    * @param board The board to evaluate
    * @return A Double value representing the "favorability" of a
    *         particular side.  A negative value indicates that the BLACK side is favored; a positive value indicates that
    *         the WHITE side is favored; a value of zero represents a balanced game
    */
  def apply(board: Board): Double = {
    val pieceCountValue = Future(pieceCount(board))
    val mobilityValue = Future(mobility(board))
    val openFilesValue = Future(openFiles(board))
    val semiOpenFilesValue = Future(semiOpenFiles(board))
    val twoBishopsValue = Future(twoBishops(board))
    val passedPawnsValue = Future(passedPawns(board))
    val castlingValue = Future(castling(board))
    val centerValue = Future(center(board))
    val backwardPawnValue = Future(backwardPawn(board))
    val result = for {
      pieces <- pieceCountValue
      mob <- mobilityValue
      files <- openFilesValue
      semiFiles <- semiOpenFilesValue
      bishops <- twoBishopsValue
      passer <- passedPawnsValue
      cstl <- castlingValue
      cntr <- centerValue
      bkwrd <- backwardPawnValue
    } yield { pieces + mob + files + semiFiles + bishops + passer + cstl + cntr + bkwrd }
    try {
      Await.result(result, Duration.apply(3, TimeUnit.SECONDS))
    } catch {
      case _: Exception => Double.MaxValue
    }
  }

  def castling(board: Board): Double = {
    val wk = board.pieces.indexOf(WK)
    val bk = board.pieces.indexOf(BK)
    var sum = 0
    if(board.whiteKingCastleAvailable ||
      board.whiteQueenCastleAvailable ||
      wk == 91 || wk == 92 || wk == 96 || wk == 97 || wk == 98
      ) sum += 100

    if(board.blackKingCastleAvailable ||
      board.blackQueenCastleAvailable ||
      bk == 21 || bk == 22 || bk == 26 || bk == 27 || bk == 28
      ) sum -= 100

    sum
  }

  def backwardPawn(board: Board): Double = 0

  def mobility(board: Board): Double = {
    if(board.whiteToMove)
      board.availableMoves.length - board.copy(whiteToMove = !board.whiteToMove).availableMoves.length
    else board.copy(whiteToMove = !board.whiteToMove).availableMoves.length - board.availableMoves.length
  }

  def center(board: Board): Double = {
    val d5 = board.pieces(54)
    val d4 = board.pieces(55)
    val e5 = board.pieces(64)
    val e4 = board.pieces(65)

    Vector(e4, e5, d4, d5).map {
      case `BP` => -2
      case `WP` => 2
      case _    => 0
    }.sum
  }

  def openFiles(board: Board): Double = 0
  def semiOpenFiles(board: Board): Double = 0
  def passedPawns(board: Board): Double =  {

    def isPassed(index: Int, white: Boolean): Boolean = {
      if(white)
        (Range(index % 10, 29, -10).exists(i => board.pieces(i) == WP && board.pieces(i) == BP))
      else
        (Range(index % 10, 99, 10).exists(i => board.pieces(i) == WP && board.pieces(i) == BP))
    }

    val blackPawns = board.pieces.filter(_ == `BP`)
    val whitePawns = board.pieces.filter(_ == `WP`)

    val passedBlackPawns = blackPawns.filter(p => isPassed(p, false))
    val passedWhitePawns = whitePawns.filter(p => isPassed(p, true))

    val supportedPassedBlackPawns = {
      val columns = passedBlackPawns.map(_ % 10)
      columns.zip(columns.tail).filter { case (a, b) => a + 1 == b }
    }

    val supportedPassedWhitePawns = {
      val columns: Vector[Int] = passedWhitePawns.map(_ % 10)
      columns.zip(columns.tail).filter { case (a, b) => (a + 1) == b }
    }

    passedBlackPawns.map(_ => -10).sum + supportedPassedBlackPawns.map(_ => -5).sum +
      passedWhitePawns.map(_ => 10).sum + supportedPassedWhitePawns.map(_ => 5).sum
  }

  def twoBishops(board: Board): Double = {
    LEGAL_SQUARES.map { index =>
      board.pieces(index) match {
        case `WB` => 100
        case `BB` => -100
        case _    => 0
      }
    }.sum
  }

  def pieceCount(board: Board): Double = {
    val pawnCount =
      board.pieces count (piece => piece == BP || piece == WP)

    val bishopWeightFactor: Double =
      1 + (16d - pawnCount) / 64

    val knightWeightFactor: Double =
      1 - (16d - pawnCount) / 64

    LEGAL_SQUARES
      .map(index =>
        board.pieces(index) match {
          case `_E` =>
            _E.weight
          case `BP` =>
            -BP.weight *
              (1 -
                (Range(index % 10, 88, 10)
                  .count(i => board.pieces(i) == BP) - 1) / 4
                )
          case `BB` =>
            -BB.weight * bishopWeightFactor
          case `BN` =>
            -BN.weight * knightWeightFactor
          case `BR` | `BQ` | `BK` =>
            -board.pieces(index).weight
          case `WP` =>
            WP.weight *
              (1 -
                (Range(index % 10, 29, -10)
                  .count(i => board.pieces(i) == WP) - 1) / 4
                )
          case `WB` =>
            WB.weight * bishopWeightFactor
          case `WN` =>
            WN.weight * knightWeightFactor
          case `WR` | `WQ` | `WK` =>
            board.pieces(index).weight
        }
      )
      .sum
  }
}
