package me.amanj.zahak

/**
  * The state representation of a chess board.
  *
  * A "Board" provides the functionality for search, evaluation, and move
  * generation (the core components of a chess engine).
  *
  * @param pieces A sequence of Bytes of length 120.  An actual chess board consists of an 8x8 grid; however,
  *               to allow for efficient move generation for knights, the board is padded by two squares on the top
  *               and bottom, and one square on the left and right side.  This board is represented with
  *               10 columns and 12 rows.  This improves move generation for knights by being able to detect
  *               an "out-of-bounds" square without having to deal with null pointer exceptions.  It also
  *               simplifies the math involved with move generation, since each row has ten slots,
  *               which correspond nicely to base-10 arithmetic
  * @param whiteToMove Flag indicating if it is White's turn to move.  False indicates it is Black's turn to move.
  * @param blackKingCastleAvailable Flag indicating if the Black King-side Castle is still available
  * @param blackQueenCastleAvailable Flag indicating if the Black Queen-side Castle is still available
  * @param whiteKingCastleAvailable Flag indicating if the White King-side Castle is still available
  * @param whiteQueenCastleAvailable Flag indicating if the White Queen-side Castle is still available
  * @param enPassant An optional index to the "en passant" square from a previous move
  * @param moveCount The number of moves which have occured on the board thus far
  */
case class Board(pieces: Vector[Byte],
                 whiteToMove: Boolean,
                 blackKingCastleAvailable: Boolean,
                 blackQueenCastleAvailable: Boolean,
                 whiteKingCastleAvailable: Boolean,
                 whiteQueenCastleAvailable: Boolean,
                 enPassant: Option[Byte],
                 moveCount: Short) {

  /**
    * Helper alias to retrieve this board's available moves
    *
    * @return a sequence of (from, to) tuples, indicating the index of the piece being moved,
    *         and the index to move to
    */
  @inline
  def availableMoves: Vector[(Byte, Byte)] =
    Search.availableMoves(this)

  /**
    * Executes the move from the given index to the given index.  The value at
    * the "from" index is set to Empty.  Any piece at the "to" index will be
    * captured/removed from the board.
    *
    * @param from     The index of the position from which the piece is moving
    * @param to       The target index to move to
    * @param isEval   In evaluation mode, moveCount is not incremented, making
    *                 it easier to keep track of "seen" positions
    * @return A new instance of the updated Board, with all of the proper flags
    *         set
    */
  def move(from: Byte,
           to: Byte, isEval: Boolean): Board = {

    val isBKCastle: Boolean =
      blackKingCastleAvailable &&
        (
          pieces(from.toInt) == BK &&
            from == 25 &&
            to == 27
          )

    val isBQCastle: Boolean =
      blackQueenCastleAvailable &&
        !isBKCastle &&
        (
          pieces(from.toInt) == BK &&
            from == 25 &&
            to == 23
          )

    val isWKCastle: Boolean =
      whiteKingCastleAvailable &&
        !isBKCastle &&
        !isBQCastle &&
        (
          pieces(from.toInt) == WK &&
            from == 95 &&
            to == 97
          )

    val isWQCastle: Boolean =
      whiteQueenCastleAvailable &&
        !isBKCastle &&
        !isBQCastle &&
        !isWKCastle &&
        (
          pieces(from.toInt) == WK &&
            from == 95 &&
            to == 93
          )
    val isPawn = pieces(from) match {
      case `WP` | `BP` => true
      case _           => false
    }

    val enPassantDirection = if(whiteToMove) -10 else 10
    val newEnPassant: Option[Byte] = {
      if(isPawn && Math.abs(from - to) == 20) {
        Some((from + enPassantDirection).toByte)
      } else None
    }

    // enPassant capture
    val isEnPassant =
      isPawn &&
      enPassant.filter(_ == to).isDefined &&
      pieces(to).isEmpty &&
      enPassant.filter { ep =>
        val piece = pieces(ep - enPassantDirection)
        whiteToMove != piece.isWhite && (piece == WP && !whiteToMove || piece == BP && whiteToMove)
      }.isDefined

    val newPieces =
      if(!isEnPassant)
        pieces
          .updated(to.toInt, pieces(from.toInt))
          .updated(from.toInt, 1.toByte)
      else
        pieces
          .updated(to.toInt, pieces(from.toInt))
          .updated(from.toInt, 1.toByte)
          .updated(enPassant.get - enPassantDirection, 1.toByte)

    Board(
      newPieces,
      !whiteToMove,
      (blackKingCastleAvailable || isBKCastle) && from != 28 && from != 25,
      (blackQueenCastleAvailable || isBQCastle) && from != 21 && from != 25,
      (whiteKingCastleAvailable || isWKCastle) && from != 98 && from != 95,
      (whiteQueenCastleAvailable || isWQCastle) && from != 91 && from != 95,
      newEnPassant,
      if(isEval) moveCount else (moveCount + 1).toByte
    )
  }

  /**
    * Determines if there is a winner of the match, by checking for the non-existence of a King for each side.
    *
    * @return An option indicating the "side" which won.
    *         Some([[me.amanj.zahak#BLACK]]) if Black won the match
    *         Some([[me.amanj.zahak#WHITE]]) if White won the match
    *         None if the game is still in progress
    */
  def winner: Option[Byte] =
    if (pieces contains BK)
      if (pieces contains WK)
        None
      else
        Some(BLACK)
    else
      Some(WHITE)

  /**
    * Helper alias to search this board
    *
    * @param depth The maximum depth to search
    * @return The best move to be made from the available moves
    */
  @inline
  def search(depth: Int) =
    Search(depth)(this)

  /**
    * Helper alias to evaluate this board
    *
    * @return A Double value representing the "favorability" of a
    *         particular side.  A negative value indicates that the BLACK side is favored; a positive value indicates that
    *         the WHITE side is favored; a value of zero represents a balanced game
    */
  @inline
  def evaluate =
    Evaluate(this)

  def display: String = {
    val sep = s"|${"-" * 15}|"
    var index = 2
    var builder = new StringBuilder()
    LEGAL_SQUARES.zipWithIndex.foreach { case (square, index) =>
      if(index != 0 && index % 8 == 0) {
        builder.append("|")
        builder.append("\n")
        builder.append(sep)
        builder.append("\n")
      }
      builder.append("|")
      pieces(square) match {
        case BP => builder.append("\u265F")
        case BN => builder.append("\u265E")
        case BB => builder.append("\u265D")
        case BR => builder.append("\u265C")
        case BQ => builder.append("\u265B")
        case BK => builder.append("\u265A")
        case WP => builder.append("\u2659")
        case WN => builder.append("\u2658")
        case WB => builder.append("\u2657")
        case WR => builder.append("\u2656")
        case WQ => builder.append("\u2655")
        case WK => builder.append("\u2654")
        case _E  => builder.append(" ")
      }
    }
    builder.append("|").toString
  }

}

object Board {

  final val default: Board =
    Board(
      DEFAULT_BOARD_SQUARES,
      whiteToMove = true,
      blackKingCastleAvailable = true,
      blackQueenCastleAvailable = true,
      whiteKingCastleAvailable = true,
      whiteQueenCastleAvailable = true,
      enPassant = None,
      moveCount = 0
    )

  final val challenge: Board =
    Board(
      CHALLENGE_BOARD_SQUARES,
      whiteToMove = true,
      blackKingCastleAvailable = false,
      blackQueenCastleAvailable = false,
      whiteKingCastleAvailable = false,
      whiteQueenCastleAvailable = false,
      enPassant = None,
      moveCount = 14
    )

}
