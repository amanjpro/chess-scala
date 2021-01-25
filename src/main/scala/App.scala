package me.amanj.zahak

import scala.io.StdIn.readLine


object App {
  def main(args: Array[String]): Unit = {
    var board = Board.default
    println(board.display)
    while(board.winner.isEmpty) {
      println("Enter move for white")
      val Array(from, to) = readLine().split(",").map(_.trim.toByte)
      board = board.move(from, to, false)
      println(board.display)
      val (efrom, eto) = board.search(Config.SEARCH_DEPTH)
      board = board.move(efrom, eto, false)
      println(board.display)
    }
    val winner = if(board.winner.get == BLACK) "Game over! you lost" else "Congrats! you won"
    println(winner)
  }
}

