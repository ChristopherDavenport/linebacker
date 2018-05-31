package io.chrisdavenport.linebacker

import cats.effect._
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

trait Linebacker[F[_]] {

  def blockingContext: ExecutionContext

  /**
   * Attempts to Run the Given `F[A]` on the blocking pool.
   * Then shifts back to the given implicit execution context
   * after the Async `F[A]` is evaluated.
   */
  final def blockEc[A](fa: F[A])(implicit F: Async[F], ec: ExecutionContext): F[A] =
    dualShift(blockingContext, ec, fa)

  /**
   * Attempts to Run the Given `F[A]` on the blocking pool.
   * Then shifts back to the F for the timer.
   */
  final def blockTimer[A](fa: F[A])(implicit F: Async[F], timer: Timer[F]): F[A] =
    F.bracket(Async.shift[F](blockingContext))(_ => fa)(_ => timer.shift)
}

object Linebacker {
  def apply[F[_]](implicit ev: Linebacker[F]): Linebacker[F] = ev

  def fromExecutorService[F[_]](es: ExecutorService): Linebacker[F] = new Linebacker[F] {
    def blockingContext = ExecutionContext.fromExecutorService(es)
  }
  def fromExecutionContext[F[_]](ec: ExecutionContext): Linebacker[F] = new Linebacker[F] {
    def blockingContext = ec
  }
}
