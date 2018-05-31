package io.chrisdavenport.linebacker

import cats._
import cats.implicits._
import cats.effect._
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

trait Linebacker[F[_]] {

  def blockingPool: ExecutionContext

  // TODO: REMOVE 0.2
  @deprecated("0.2.0", "Use blockShift instead.")
  final def block[A](fa: F[A])(implicit F: Async[F], ec: ExecutionContext): F[A] =
    blockShift(fa)(F, ec)

  /**
   * Attempts to Run the Given `F[A]` on the blocking pool.
   * Then shifts back to the given implicit execution context
   * after the Async `F[A]` is evaluated.
   */
  final def blockShift[A](fa: F[A])(implicit F: Async[F], ec: ExecutionContext): F[A] =
    for {
      _ <- Async.shift(blockingPool)
      eA <- fa.attempt
      _ <- Async.shift(ec)
      a <- Applicative[F].pure(eA).rethrow
    } yield a

  /**
   * Attempts to Run the Given `F[A]` on the blocking pool.
   * Then shifts back to the F for the timer.
   */
  final def blockTimer[A](fa: F[A])(implicit F: Async[F], timer: Timer[F]): F[A] =
    for {
      _ <- Async.shift(blockingPool)
      eA <- fa.attempt
      _ <- timer.shift
      a <- Applicative[F].pure(eA).rethrow
    } yield a
}

object Linebacker {
  def apply[F[_]](implicit ev: Linebacker[F]): Linebacker[F] = ev

  def fromExecutorService[F[_]](es: ExecutorService): Linebacker[F] = new Linebacker[F] {
    def blockingPool = ExecutionContext.fromExecutorService(es)
  }
  def fromExecutionContext[F[_]](ec: ExecutionContext): Linebacker[F] = new Linebacker[F] {
    def blockingPool = ec
  }
}
