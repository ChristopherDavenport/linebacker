package io.chrisdavenport.linebacker

import cats._
import cats.implicits._
import cats.effect.Async
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

trait Linebacker[F[_]] {

  def blockingPool: ExecutionContext

  /**
   * Attempts to Run the Given `F[A]` on the blocking pool.
   * Then shifts back to the given implicit execution context
   * after the Async `F[A]` is evaluated.
   */
  final def block[A](fa: F[A])(implicit F: Async[F], ec: ExecutionContext): F[A] =
    for {
      _ <- Async.shift(blockingPool)
      eA <- fa.attempt
      _ <- Async.shift(ec)
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
