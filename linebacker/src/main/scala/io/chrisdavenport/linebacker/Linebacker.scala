package io.chrisdavenport.linebacker

import cats.effect._
import cats.effect.concurrent.Semaphore
import cats.implicits._
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

trait Linebacker[F[_]] {

  def blockingContext: ExecutionContext

  /**
   * Attempts to Run the Given `F[A]` on the blocking pool.
   * Then shifts back to the F to the Context Shift
   * Requires Implicit ContextShift Available
   */
  def blockContextShift[A](fa: F[A])(implicit cs: ContextShift[F]): F[A] =
    cs.evalOn(blockingContext)(fa)

  /**
   * Same Method as blockContextShift but significantly shorter.
    **/
  final def blockCS[A](fa: F[A])(implicit cs: ContextShift[F]): F[A] =
    blockContextShift(fa)
}

object Linebacker {
  def apply[F[_]](implicit ev: Linebacker[F]): Linebacker[F] = ev

  def fromExecutorService[F[_]](es: ExecutorService): Linebacker[F] = new Linebacker[F] {
    def blockingContext = ExecutionContext.fromExecutorService(es)
  }
  def fromExecutionContext[F[_]](ec: ExecutionContext): Linebacker[F] = new Linebacker[F] {
    def blockingContext = ec
  }

  def bounded[F[_]: Concurrent](lb: Linebacker[F], bound: Long): F[Linebacker[F]] = 
    Semaphore[F](bound).map(new BoundedLinebacker(lb, _))

  private class BoundedLinebacker[F[_]: Concurrent](lb: Linebacker[F], s: Semaphore[F]) extends Linebacker[F]{
    def blockingContext: ExecutionContext = lb.blockingContext
    override def blockContextShift[A](fa: F[A])(implicit cs: ContextShift[F]): F[A] =
      s.withPermit(lb.blockContextShift(fa))
  }
}
