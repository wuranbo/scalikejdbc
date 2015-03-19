package scalikejdbc

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Failure, Success }
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
 * This type class enable users to customize the behavior of transaction boundary(commit/rollback).
 */
trait TxBoundary[A] {

  /**
   * Finishes the current transaction.
   */
  def finishTx(result: A, tx: Tx): A

  /**
   * Closes the current connection if needed.
   */
  def closeConnection(result: A, doClose: () => Unit): A = {
    doClose()
    result
  }

}

/**
 * TxBoundary type class instances.
 */
object TxBoundary {

  /**
   * Exception TxBoundary type class instance.
   */
  object Exception {

    implicit def exceptionTxBoundary[A] = new TxBoundary[A] {
      def finishTx(result: A, tx: Tx): A = {
        tx.commit()
        result
      }
    }
  }

  sealed abstract class TxBoundaryMissingImplicits {
    implicit def ambiguousTxBoundary[A]: TxBoundary[Future[A]] = macro TxBoundaryMissingImplicits.missingTxBoundary[A]
  }
  private object TxBoundaryMissingImplicits {

    def missingTxBoundary[A: c.WeakTypeTag](c: Context): c.Expr[TxBoundary[Future[A]]] = {
      import c.universe._
      c.abort(c.enclosingPosition, s"You import TxBoundary.Future._, However any scala.concurrent.ExecutionContext instance could not found in implicit scope.")
    }

  }

  /**
   * Future TxBoundary type class instance.
   */
  object Future extends TxBoundaryMissingImplicits {

    implicit def futureTxBoundary[A](implicit ec: ExecutionContext) = new TxBoundary[Future[A]] {
      def finishTx(result: Future[A], tx: Tx): Future[A] = {
        result.andThen {
          case Success(_) => tx.commit()
          case Failure(_) => tx.rollback()
        }
      }
      override def closeConnection(result: Future[A], doClose: () => Unit): Future[A] = {
        result.andThen {
          case _ => doClose()
        }
      }
    }
  }

  /**
   * Either TxBoundary type class instance.
   */
  object Either {

    implicit def eitherTxBoundary[L, R] = new TxBoundary[Either[L, R]] {
      def finishTx(result: Either[L, R], tx: Tx): Either[L, R] = {
        result match {
          case Right(_) => tx.commit()
          case Left(_) => tx.rollback()
        }
        result
      }
    }
  }

  /**
   * Try TxBoundary type class instance.
   */
  object Try {

    implicit def tryTxBoundary[A] = new TxBoundary[Try[A]] {
      def finishTx(result: Try[A], tx: Tx): Try[A] = {
        result match {
          case Success(_) => tx.commit()
          case Failure(_) => tx.rollback()
        }
        result
      }
    }
  }

}
